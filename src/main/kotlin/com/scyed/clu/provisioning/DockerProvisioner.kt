package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.scyed.clu.console.ConsolePump
import com.scyed.clu.glyph.toDto
import com.scyed.clu.server.PowerAction
import com.scyed.clu.server.ServerEntity
import com.scyed.clu.server.ServerRepository
import com.scyed.clu.server.ServerStatus
import com.scyed.clu.server.event.KillServerRequested
import com.scyed.clu.server.event.ServerPowerRequested
import com.scyed.clu.server.event.ServerReinstallRequested
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


@ConfigurationProperties(prefix = "scyed.gameserver")
data class GameserverProperties(
    val installTemp: Path = Paths.get("leck"),
    val gameserverStorage: Path = Paths.get("leck"),
    val userUid: Long = 1001,
    val userGid: Long = 1001,
)

@Configuration
@EnableConfigurationProperties(GameserverProperties::class)
class EggConfiguration

@Component
class DockerProvisioner(
    private val docker: DockerClient,
    private val serverRepository: ServerRepository,
    private val properties: GameserverProperties,
    private val containerAttachmentManager: ContainerAttachmentManager,
    private val consolePump: ConsolePump
) {
    private final val installScriptName = "install.sh"
    private final val gameserverPathInContainer = "/mnt/server"
    private final val installScriptPathInContainer = "/mnt/installScript"
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("provisioningExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reInstallServer(event: ServerReinstallRequested) {
        var server = serverRepository.findById(event.serverId).orElseThrow()
        val glyph = server.glyphEntity
        requireNotNull(glyph) { "glyph entity not found" }
        log.info("Provisioning request for ${server.id}")
        try {
            val installScript = createInstallScript(event.serverId.toString(), glyph.installScript)
            ensureGameFilesDirectory(event.serverId.toString())
            val container = docker.createContainerCmd(glyph.installContainer).withName(server.id.toString())
                .withHostConfig(// TODO: replace with actual server name
                    HostConfig.newHostConfig().withCpuPercent(server.cpuPercent)
                        .withMemory(server.memoryMb * 1024L * 1024L) // bytes!
                        .withSecurityOpts(listOf("no-new-privileges")).withBinds(
                            Binds(
                                Bind(
                                    installScript.parent.toString(), Volume("$installScriptPathInContainer")
                                ), gameFiles(server.id.toString())
                            )
                        )
                ).withUser(containerUser()).withEnv(server.toEnvList())
                .withCmd("ash", "$installScriptPathInContainer/$installScriptName").exec()


            server.containerId = container.id
            server.status = ServerStatus.INSTALLING
            server = serverRepository.save(server)

            val callback = WaitContainerResultCallback()
            docker.startContainerCmd(container.id).exec();
            log.info("Started Conatiner: ${server.containerId}")
            val logCallback = streamLogsToFile(container.id, installScript.parent.resolve("install.log"))


            docker.waitContainerCmd(container.id).exec(callback);
            log.debug("Waiting for install to finish: ${server.containerId}")
            val exitCode = callback.awaitStatusCode()

            log.info("Installer finised exitcode: $exitCode")

            logCallback.awaitCompletion()
            logCallback.close()
            log.info("Logfile closed")


            server.status = ServerStatus.IDLE
            server = serverRepository.save(server)

        } catch (e: Exception) {
            log.error("Provisioning failed for server ${server.id}", e)
            server.status = ServerStatus.ERROR // TODO: Maybe creation failed
//            server.failureReason = e.message
            serverRepository.save(server)
        }
    }

    @Async("provisioningExecutor")
    @EventListener
    fun onPowerRequested(event: ServerPowerRequested) {
        val server = serverRepository.findById(event.serverId)
            .orElseThrow { throw RuntimeException("Server ${event.serverId} not found") }

        when (event.action) {
            PowerAction.START -> startServer(server)
            else -> throw RuntimeException("${event.action} not implemented")
        }
    }

    private fun startServer(serevr: ServerEntity) {
        log.info("Starting ${serevr.id}")
        val server = serverRepository.findWithGlyphById(serevr.id!!)
        requireNotNull(server) { throw RuntimeException("Server ${serevr.id.toString()} not found") }
        val startup = server.glyphEntity.toDto().renderStartup(server.env)
        ensureGameFilesDirectory(server.id.toString())

        if (server.containerId != null) {
            killAndRemoveServer(KillServerRequested(server))
            log.debug("Server ${server.containerId} was killed")
        }

        val container = docker.createContainerCmd(server.image).withName(server.id.toString()).withHostConfig(
            HostConfig.newHostConfig().withCpuPercent(server.cpuPercent)
                .withMemory(server.memoryMb * 1024L * 1024L) // bytes!
                .withReadonlyRootfs(true).withSecurityOpts(listOf("no-new-privileges")).withBinds(
                    Binds(
                        gameFiles(server.id.toString())
                    )
                )
        ).withStdinOpen(true).withUser(containerUser()).withEnv(server.toEnvList()).withCmd("/bin/sh", "-c", startup)
            .exec()

        val attachment = containerAttachmentManager.attach(server.id!!, container.id)
        consolePump.start(attachment)

        log.info("Created Container ${container.id}")
        server.containerId = container.id
        docker.startContainerCmd(container.id).exec()
        log.info("Started Container ${container.id}")
        serverRepository.save(server)
    }

    private fun createInstallScript(containerId: String, script: String): Path {
        val installDirectory =
            properties.installTemp.resolve(containerId, "installScript").toAbsolutePath().normalize();
        Files.createDirectories(installDirectory)
        log.info("Created Installer Folder structure $installDirectory")

        val installScript = installDirectory.resolve(installScriptName)

        Files.writeString(installScript, script)
        log.info("Wrote InstallScript: $installScript")
        return installScript
    }

    private fun containerUser(): String = "${properties.userUid}:${properties.userGid}"

    public fun killAndRemoveServer(event: KillServerRequested): ServerEntity {
        if (event.server.containerId != null) {
            log.info("Killing and removing container ${event.server.containerId}")
            try {
                consolePump.stop(event.server.id!!)
                containerAttachmentManager.detach(event.server.id!!)
                docker.killContainerCmd(event.server.containerId!!).exec();
                log.info("Killed container ${event.server.containerId}")
                Thread.sleep(1000)
            } catch (e: Exception) {
                log.error("Failed to kill container ${event.server.containerId}: ${e.message}")
            }
            try {
                docker.removeContainerCmd(event.server.containerId!!).exec();
            } catch (e: Exception) {
                log.error("Failed to remove container ${event.server.containerId}: ${e.message}")
            }
            Thread.sleep(1000)
            log.info("Removed container ${event.server.containerId}")
            event.server.containerId = null
            event.server.status = ServerStatus.STOPPED

            val server = serverRepository.save(event.server)
            log.info("Saved server ${event.server.id}")
            return server
        } else {
            throw RuntimeException("No container to kill for server, container: ${event.server.containerId} Status: ${event.server.status}")
        }
    }

    private fun gameFiles(containerId: String): Bind =
        Bind(gameFilesPath(containerId).toString(), Volume(gameserverPathInContainer))

    private fun gameFilesPath(containerId: String): Path =
        properties.gameserverStorage.toAbsolutePath().resolve(containerId, "gameFiles").normalize()

    private fun ensureGameFilesDirectory(containerId: String): Path {
        val gameFilesDirectory = gameFilesPath(containerId)
        Files.createDirectories(gameFilesDirectory)
        return gameFilesDirectory
    }


    private fun streamLogsToFile(containerId: String, logFile: Path): ResultCallback.Adapter<Frame> {
        Files.createDirectories(logFile.parent)
        val writer = Files.newBufferedWriter(
            logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND
        )

        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame?) {
                // if (frame.streamType == StreamType.STDERR)
                writer.write(String(frame!!.payload, Charsets.UTF_8))
                writer.flush();
            }

            override fun onError(throwable: Throwable) {
                log.error("Log streaming error for $containerId", throwable)
                writer.close()
                super.onError(throwable)
            }

            override fun onComplete() {
                writer.close()
                super.onComplete()
            }
        }
        docker.logContainerCmd(containerId).withStdOut(true).withStdErr(true).withFollowStream(true)
            .withTimestamps(true).exec(callback)

        return callback;
    }

}
