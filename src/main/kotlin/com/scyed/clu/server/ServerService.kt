package com.scyed.clu.server

import com.scyed.clu.api.dto.CreateServerRequest
import com.scyed.clu.glyph.GlyphEnvVarValidator
import com.scyed.clu.glyph.GlyphRepository
import com.scyed.clu.provisioning.ContainerService
import com.scyed.clu.provisioning.DockerProvisioner
import com.scyed.clu.server.event.ServerReinstallRequested
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.*

@Service
class ServerService(
    private val serverRepository: ServerRepository,
    private val glyphRepository: GlyphRepository,
    private val glyphEnvVarValidator: GlyphEnvVarValidator,
    private val eventPublisher: ApplicationEventPublisher,
    private val provisioner: DockerProvisioner,
    private val containerService: ContainerService,
    private val transitions: ServerStateTransitions,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllServers(): Pair<Long, List<ServerEntity>> =
        serverRepository.count() to serverRepository.findAll().toList()

    fun createServer(request: CreateServerRequest): ServerEntity {
        if (serverRepository.existsByName(request.name)) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Name is already taken"
        )

        val glyph = glyphRepository.findById(request.glyphId).orElseThrow {
            ResponseStatusException(HttpStatus.BAD_REQUEST, "Glyph not found")
        }

        try {
            glyphEnvVarValidator.validate(glyph.envVars, request.env)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }

        val server = serverRepository.save(
            ServerEntity(
                name = request.name,
                description = request.description,
                containerId = "",
                image = request.imageName,
                skip_scripts = false,
                memoryMb = request.memoryMb,
                cpuPercent = request.cpuPercent,
                env = request.env,
                startCommand = request.startCommand ?: glyph.startup,
                glyphEntity = glyph,
            )
        )
        log.info("Created server ${server.name} id=${server.id}")
        eventPublisher.publishEvent(ServerReinstallRequested(server.id!!))
        return server
    }

    fun reinstallServer(serverId: UUID, forceStop: Boolean) {
        val server = serverRepository.findById(serverId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Server with ID $serverId not found")
        }
        if (!forceStop && server.status.status != ServerStatus.STOPPED) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Server must be stopped to reinstall or use forceStop=true"
        )

        if (server.containerId != null) {
            killServer(server)
        }

        log.info("Queuing reinstall for server $serverId")
        eventPublisher.publishEvent(ServerReinstallRequested(serverId))
    }

    fun powerAction(serverId: UUID, action: PowerAction) {
        val server = serverRepository.findWithGlyphById(serverId)

        requireNotNull(server) { ResponseStatusException(HttpStatus.NOT_FOUND, "Server with ID $serverId not found") }

        when (action) {
            PowerAction.START -> {
                provisioner.startServer(server)
            }

            PowerAction.STOP -> stopServer(server)
            PowerAction.KILL -> killServer(server)
            else -> log.error("Unexpected action $action")
        }
    }

    private fun stopServer(server: ServerEntity) {
        // If somehow bugged and now conatinerId, just set the status to stopped
        if (server.containerId == null) {
            log.warn("Server ${server.id} wasnt stopped but didnt had a container id")
        }
        transitions.stopping(server)
        containerService.stopContainer(server.id!!, server.containerId!!)
        // STOPPED is published by ContainerAttachmentManager's waitCallback on actual exit.
        log.info("Server ${server.id} stopped")
    }

    private fun killServer(server: ServerEntity) {
        if (server.containerId == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No container to kill for server ${server.id}")
        }
        transitions.kill(server)
        try {
            // TODO: Proper kill
            containerService.stopContainer(server.id!!, server.containerId!!)
            containerService.removeContainer(server.containerId!!)
            server.containerId = null
            serverRepository.save(server)
            log.info("Server ${server.id} killed and container removed")
        } catch (e: Exception) {
            log.error("Failed to kill Server ${server.id}")
        }
    }
}