package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Volume
import com.scyed.clu.console.ConsolePump
import com.scyed.clu.server.ServerEntity
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Component
class ContainerService(
    private val docker: DockerClient,
    private val properties: GameserverProperties,
    private val containerAttachmentManager: ContainerAttachmentManager,
    private val consolePump: ConsolePump,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val gameserverPathInContainer = "/home/container"

    fun createAndStartGameServer(server: ServerEntity, startup: String): CreateContainerResponse {
        val container = docker.createContainerCmd(server.image)
            .withName(server.id.toString())
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withCpuPercent(server.cpuPercent)
                    .withMemory(server.memoryMb * 1024L * 1024L)
                    .withReadonlyRootfs(true)
                    .withSecurityOpts(listOf("no-new-privileges"))
                    .withBinds(Binds(gameFiles(server.id.toString())))
            )
            .withStdinOpen(true)
            .withUser(containerUser())
            .withEnv(server.toEnvList())
            .withCmd("/bin/sh", "-c", startup)
            .exec()

        val attachment = containerAttachmentManager.attach(server.id!!, container.id)
        consolePump.start(attachment)

        docker.startContainerCmd(container.id).exec()
        log.info("Created and started container ${container.id} for server ${server.id}")

        return container
    }

    fun createInstallContainer(
        server: ServerEntity,
        installImage: String,
        installScriptDir: Path,
        installScriptPathInContainer: String,
        installScriptName: String,
    ): CreateContainerResponse {
        return docker.createContainerCmd(installImage)
            .withName(server.id.toString())
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withAutoRemove(true)   // Automaticly remove Install container
                    .withCpuPercent(server.cpuPercent)
                    .withMemory(server.memoryMb * 1024L * 1024L)
                    .withSecurityOpts(listOf("no-new-privileges"))
                    .withBinds(
                        Binds(
                            Bind(installScriptDir.toString(), Volume(installScriptPathInContainer)),
                            gameFiles(server.id.toString())
                        )
                    )
            )
            .withUser(containerUser())
            .withEnv(server.toEnvList())
            .withCmd("ash", "$installScriptPathInContainer/$installScriptName")
            .exec()
    }

    fun startContainer(containerId: String) {
        docker.startContainerCmd(containerId).exec()
        log.info("Started container $containerId")
    }

    fun stopContainer(serverId: UUID, containerId: String) {
        log.info("Stopping container $containerId")
        try {
            consolePump.stop(serverId)
            containerAttachmentManager.detach(serverId)
            docker.killContainerCmd(containerId).exec()
            log.info("Stopped container $containerId")
        } catch (e: Exception) {
            log.error("Failed to stop container $containerId: ${e.message}")
        }
    }

    fun removeContainer(containerId: String) {
        log.info("Removing container $containerId")
        try {
            docker.removeContainerCmd(containerId).withForce(true).exec()
        } catch (e: Exception) {
            log.error("Failed to remove container $containerId: ${e.message}")
        }
        log.info("Removed container $containerId")
    }

    fun startExistingContainer(serverId: UUID, containerId: String) {
        docker.startContainerCmd(containerId).exec()
        val attachment = containerAttachmentManager.attach(serverId, containerId)
        consolePump.start(attachment)
        log.info("Started existing container $containerId for server $serverId")
    }

    fun ensureGameFilesDirectory(serverId: String): Path {
        val gameFilesDirectory = gameFilesPath(serverId)
        Files.createDirectories(gameFilesDirectory)
        return gameFilesDirectory
    }

    private fun gameFiles(serverId: String): Bind =
        Bind(gameFilesPath(serverId).toString(), Volume(gameserverPathInContainer))

    private fun gameFilesPath(serverId: String): Path =
        properties.gameserverStorage.toAbsolutePath().resolve(serverId, "gameFiles").normalize()

    private fun containerUser(): String = "${properties.userUid}:${properties.userGid}"
}
