package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Frame
import com.scyed.clu.glyph.toDto
import com.scyed.clu.server.ServerEntity
import com.scyed.clu.server.ServerRepository
import com.scyed.clu.server.ServerStatus
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
    private val containerService: ContainerService,
) {
    private val installScriptName = "install.sh"
    private val installScriptPathInContainer = "/mnt/installScript"
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
            containerService.ensureGameFilesDirectory(event.serverId.toString())

            val container = containerService.createInstallContainer(
                server,
                glyph.installContainer,
                installScript.parent,
                installScriptPathInContainer,
                installScriptName,
            )

            server.containerId = container.id
            server.status = ServerStatus.INSTALLING
            server = serverRepository.save(server)

            containerService.startContainer(container.id)
            log.info("Started install container: ${server.containerId}")

            val logCallback = streamLogsToFile(container.id, installScript.parent.resolve("install.log"))
            val waitCallback = WaitContainerResultCallback()
            docker.waitContainerCmd(container.id).exec(waitCallback)
            log.debug("Waiting for install to finish: ${server.containerId}")
            val exitCode = waitCallback.awaitStatusCode()

            log.info("Installer finished exitcode: $exitCode")

            logCallback.awaitCompletion()
            logCallback.close()
            log.info("Logfile closed")

            server.status = ServerStatus.IDLE
            server = serverRepository.save(server)

        } catch (e: Exception) {
            log.error("Provisioning failed for server ${server.id}", e)
            server.status = ServerStatus.ERROR
            serverRepository.save(server)
        }
    }

    fun startServer(server: ServerEntity) {
        if (server.containerId != null) {
            containerService.startExistingContainer(server.id!!, server.containerId!!)
            log.info("Restarted existing container ${server.containerId}")
        } else {
            val startup = server.glyphEntity.toDto().renderStartup(server.env)
            containerService.ensureGameFilesDirectory(server.id.toString())
            val container = containerService.createAndStartGameServer(server, startup)
            log.info("Created new game server container ${container.id}")
            server.containerId = container.id
        }
        server.status = ServerStatus.RUNNING
        serverRepository.save(server)
    }

    private fun createInstallScript(serverId: String, script: String): Path {
        val installDirectory =
            properties.installTemp.resolve(serverId, "installScript").toAbsolutePath().normalize()
        Files.createDirectories(installDirectory)
        log.info("Created Installer Folder structure $installDirectory")

        val installScript = installDirectory.resolve(installScriptName)
        Files.writeString(installScript, script)
        log.info("Wrote InstallScript: $installScript")
        return installScript
    }

    private fun streamLogsToFile(containerId: String, logFile: Path): ResultCallback.Adapter<Frame> {
        Files.createDirectories(logFile.parent)
        val writer = Files.newBufferedWriter(
            logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND
        )

        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame?) {
                writer.write(String(frame!!.payload, Charsets.UTF_8))
                writer.flush()
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
        docker.logContainerCmd(containerId)
            .withStdOut(true).withStdErr(true)
            .withFollowStream(true).withTimestamps(true)
            .exec(callback)

        return callback
    }
}