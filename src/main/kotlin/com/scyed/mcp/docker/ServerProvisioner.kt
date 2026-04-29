package com.scyed.mcp.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.scyed.mcp.game.GlyphProvider
import com.scyed.mcp.jpa.ServerRepository
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
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("provisioningExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun onProvisioningRequest(event: ServerProvisioningRequested) {
        val server = serverRepository.findById(event.serverId).orElseThrow()
        val test = glyphProvider.getAll();
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
                                    installScript.toString(), Volume("/gameserver/install.sh")
                                )
                            )
                        )
                ).withCmd("bash", "/gameserver/install.sh").exec()

            server.containerId = container.id
            server.status = ServerStatus.STOPPED
            serverRepository.save(server)


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

        val installScript = installDirectory.resolve("install.sh")

        log.info("Creating Script: $script")

        Files.writeString(installScript, script)
        log.info("Created: $installScript")
        return installScript
    }


    data class ServerProvisioningRequested(val serverId: UUID)

}