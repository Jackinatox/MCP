package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.CreateContainerResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Binds
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.api.model.Volume
import com.scyed.clu.infra.properrties.GameserverProperties
import com.scyed.clu.db.entity.ServerEntity
import com.scyed.clu.infra.properrties.EnvironmentProperties
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
    private val environmentProperties: EnvironmentProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val gameserverPathInContainer = "/home/container"

    fun createAndStartGameServer(server: ServerEntity, startup: String): CreateContainerResponse {
        pullImage(server.image)
        val container = docker.createContainerCmd(server.image)
            .withName(server.id.toString())
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withCpuPercent(server.cpuPercent)
                    .withMemory(server.memoryMb * 1024L * 1024L)
                    .withReadonlyRootfs(true)
                    .withSecurityOpts(listOf("no-new-privileges"))
                    .withBinds(Binds(gameFiles(server)))
            )
            .withStdinOpen(true)
            .withUser(containerUser())
            .withEnv(server.toEnvList())
            .withCmd("/bin/sh", "-c", startup)
            .exec()

        containerAttachmentManager.attach(server.id!!, container.id)

        docker.startContainerCmd(container.id).exec()
        log.info("Created and started container ${container.id} for server ${server.id}")

        return container
    }

    private fun pullImage(image: String) {
        log.info("Started pulling Image {}", image);
        val splits = image.split(":")
        if (splits.size != 2) {
            throw IllegalArgumentException("Server image format is incorrect, needs to have one ':' image: $image")
        }
        val pull = docker.pullImageCmd(splits[0]).withTag(splits[1]).exec(object :PullImageResultCallback() {
            override fun onNext(item: PullResponseItem?) {
                log.debug(item?.progressDetail.toString())
                super.onNext(item)
            }

            override fun onComplete() {
                log.info("Image pulling completed.")
                super.onComplete()
            }

            override fun onError(throwable: Throwable?) {
                log.error("Image pulling failed", throwable)
                super.onError(throwable)
            }
        }).awaitCompletion();
    }

    fun createInstallContainer(
        server: ServerEntity,
        installImage: String,
        installScriptDir: Path,
        installScriptPathInContainer: String,
        installScriptName: String,
    ): CreateContainerResponse {
        pullImage(installImage)
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
                            gameFiles(server)
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
            // Detach first so the wait callback can't race the caller's explicit
            // transitions.stopped() with a CRASHED (SIGKILL exits non-zero).
            containerAttachmentManager.detach(serverId)
            docker.killContainerCmd(containerId).exec()
            log.info("Stopped container $containerId")
        } catch (e: Exception) {
            log.error("Failed to stop container $containerId: ${e.message}")
        }
    }

    fun removeContainer(server: ServerEntity) {
        if (server.containerId == null) {
            log.info("No container to remove")
            return
        }
        val containerId = server.id.toString()
        log.info("Removing container $containerId for server ${server.id}")
        try {
            docker.removeContainerCmd(containerId).withForce(true).exec()
        } catch (e: Exception) {
            log.error("Failed to remove container $containerId: ${e.message}")
        }
        log.info("Removed container $containerId")
    }

    fun startExistingContainer(serverId: UUID, containerId: String) {
        docker.startContainerCmd(containerId).exec()
        containerAttachmentManager.attach(serverId, containerId)
        log.info("Started existing container $containerId for server $serverId")
    }

    fun ensureGameFilesDirectory(server: ServerEntity): Path {
        val gameFilesDirectory = buildGameFilesHostPath(server)
        Files.createDirectories(gameFilesDirectory)
        return gameFilesDirectory
    }

    private fun gameFiles(server: ServerEntity): Bind =
        Bind(buildGameFilesHostPath(server).toString(), Volume(gameserverPathInContainer))

    private fun buildGameFilesHostPath(server: ServerEntity): Path =
        environmentProperties.gmStoragePath.toAbsolutePath().resolve(server.podEntity.datasetName, server.id.toString(), "gameFiles").normalize()

    private fun containerUser(): String = "${properties.userUid}:${properties.userGid}"
}
