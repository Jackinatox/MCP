package com.scyed.mcp.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.exception.ConflictException
import com.github.dockerjava.api.exception.DockerException
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.scyed.mcp.game.GlyphProvider
import com.scyed.mcp.jpa.ServerEntity
import com.scyed.mcp.jpa.repositories.ServerRepository
import com.scyed.mcp.jpa.ServerStatus
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
import java.util.*


@ConfigurationProperties(prefix = "scyed.gameserver")
data class GameserverProperties(
    val installTemp: Path = Paths.get("leck"),
    val gameserverStorage: Path = Paths.get("leck"),
)

@Configuration
@EnableConfigurationProperties(GameserverProperties::class)
class EggConfiguration

@Component
class ServerProvisioner(
    private val docker: DockerClient,
    private val serverRepository: ServerRepository,
    private val properties: GameserverProperties,
    private val glyphProvider: GlyphProvider
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
        val glyph = requireNotNull(glyphProvider.getById("egg"))
        log.info("Provisioning request for ${server.id}")
        try {
            val installScript = createInstallScript(event.serverId.toString(), glyph.scripts.installation.script)
            val container =
                docker.createContainerCmd(glyph.scripts.installation.container).withName(server.name).withHostConfig(
                    HostConfig.newHostConfig().withCpuPercent(server.cpuPercent)
                        .withMemory(server.memoryMb * 1024L * 1024L) // bytes!
                        .withBinds(
                            Binds(
                                Bind(
                                    installScript.toString(), Volume("$installScriptPathInContainer/$installScriptName")
                                ), gameFiles(server.id.toString())
                            )
                        )
                ).withCmd("bash", "$installScriptPathInContainer/$installScriptName").exec()

            server.containerId = container.id
            server.status = ServerStatus.INSTALLING
            server = serverRepository.save(server)

            val callback = WaitContainerResultCallback()
            log.info("Started Conatiner: ${server.containerId}")
            docker.startContainerCmd(container.id).exec();
            docker.waitContainerCmd(container.id).exec(callback);
            log.debug("Waiting for install to finish: ${server.containerId}")
            callback.awaitStatusCode()
            log.info("Installer finised")

            server.status = ServerStatus.IDLE
            server = serverRepository.save(server)

        } catch (e: Exception) {
            log.error("Provisioning failed for server ${server.id}", e)
            server.status = ServerStatus.ERROR // TODO: Maybe creation failed
//            server.failureReason = e.message
            serverRepository.save(server)
        }
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

    @EventListener
    public fun stopServer(event: KillServerRequested) {
        if (event.server.containerId != null) {
            log.info("Killing and removing container ${event.server.containerId}")
            try {
                docker.killContainerCmd(event.server.containerId!!).exec();
            } catch (e: ConflictException) {
                log.warn("Failed to kill container ${event.server.containerId}: ${e.message}")
            }
            docker.removeContainerCmd(event.server.containerId!!).exec();
            log.info("Killed and removed container ${event.server.containerId}")
            event.server.containerId = null
            event.server.status = ServerStatus.STOPPED

            serverRepository.save(event.server)
            log.info("Saved server ${event.server.id}")
        } else {
            log.info("No container to kill for server, container: ${event.server.containerId} Status: ${event.server.status}")
        }
    }

    private fun gameFiles(containerId: String): Bind {
        return Bind(
            properties.gameserverStorage.toAbsolutePath().resolve(containerId, "gameFiles").normalize().toString(),
            Volume(gameserverPathInContainer)
        )
    }


    data class ServerReinstallRequested(val serverId: UUID)
    data class KillServerRequested(val server: ServerEntity)
}