package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Frame
import com.scyed.clu.glyph.toDto
import com.scyed.clu.infra.properrties.GameserverProperties
import com.scyed.clu.db.entity.ServerEntity
import com.scyed.clu.db.repository.ServerRepository
import com.scyed.clu.server.ServerStateTransitions
import com.scyed.clu.db.enums.ServerStatus
import com.scyed.clu.infra.zfs.ZFSManager
import com.scyed.clu.server.event.ServerReinstallRequested
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption




@Component
class DockerProvisioner(
    private val docker: DockerClient,
    private val serverRepository: ServerRepository,
    private val properties: GameserverProperties,
    private val containerService: ContainerService,
    private val transitions: ServerStateTransitions,
    private val zFSManager: ZFSManager,
) {
    private val installScriptName = "install.sh"
    private val installScriptPathInContainer = "/mnt/installScript"
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("provisioningExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reInstallServer(event: ServerReinstallRequested) {
        val server = serverRepository.findById(event.serverId).orElseThrow()
        val glyph = server.glyphEntity
        requireNotNull(glyph) { "glyph entity not found" }
        log.info("Provisioning request for ${server.id}")
        try {
            zFSManager.createServerDataset(server)
            val installScript = createInstallScript(event.serverId.toString(), glyph.installScript)
            containerService.ensureGameFilesDirectory(server)

            val container = containerService.createInstallContainer(
                server,
                glyph.installContainer,
                installScript.parent,
                installScriptPathInContainer,
                installScriptName,
            )

            server.containerId = null
            transitions.install(server)
            serverRepository.save(server)

            containerService.startContainer(container.id)
            log.info("Started install container: ${container.id}")

            val logfileCallback = streamLogsToFile(container.id, installScript.parent.resolve("install.log"))
            val installFinishedCallback = WaitContainerResultCallback()
            docker.waitContainerCmd(container.id).exec(installFinishedCallback)
            log.debug("Waiting for install to finish: ${container.id}")
            val exitCode = installFinishedCallback.awaitStatusCode()

            log.info("Installer finished for ${container.id} exitcode: $exitCode")

            logfileCallback.awaitCompletion()
            logfileCallback.close()
            log.info("Logfile closed")

            transitions.force(server, ServerStatus.STOPPED)

        } catch (e: Exception) {
            log.error("Provisioning failed for server ${server.id}", e)
            transitions.error(server)
        }
    }

    fun startServer(server: ServerEntity) {
        transitions.starting(server)

        if (server.containerId != null) {
            containerService.startExistingContainer(server.id!!, server.containerId!!)
            log.info("Restarted existing container ${server.containerId}")
        } else {
            val startup = server.glyphEntity.toDto().renderStartup(server.env)
            containerService.ensureGameFilesDirectory(server)
            val container = containerService.createAndStartGameServer(server, startup)
            log.info("Created new game server container ${container.id}")
            server.containerId = container.id
            // Bulk update — don't save(entity), which would flush stale status and race
            // the waitCallback's STOPPED/ERROR publish for containers that exit immediately.
            serverRepository.updateContainerId(server.id!!, container.id)
        }

        // STARTED is published by the console listener once a readiness line is detected;
        // if the container exits before that, ContainerAttachmentManager publishes STOPPED/ERROR.
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