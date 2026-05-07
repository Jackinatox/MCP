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
import java.util.UUID

@Service
class ServerService(
    private val serverRepository: ServerRepository,
    private val glyphRepository: GlyphRepository,
    private val glyphEnvVarValidator: GlyphEnvVarValidator,
    private val eventPublisher: ApplicationEventPublisher,
    private val provisioner: DockerProvisioner,
    private val containerService: ContainerService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllServers(): Pair<Long, List<ServerEntity>> =
        serverRepository.count() to serverRepository.findAll().toList()

    fun createServer(request: CreateServerRequest): ServerEntity {
        if (serverRepository.existsByName(request.name)) throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Name is already taken"
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
                request.name,
                request.description,
                "",
                request.imageName,
                ServerStatus.PROVISIONING,
                false,
                request.memoryMb,
                request.cpuPercent,
                request.env,
                request.startCommand ?: glyph.startup,
                glyph,
            )
        )
        log.info("Created server ${server.name} id=${server.id}")
        eventPublisher.publishEvent(ServerReinstallRequested(requireNotNull(server.id) { "Server ID was null after save" }))
        return server
    }

    fun reinstallServer(serverId: UUID, forceStop: Boolean) {
        val server = serverRepository.findById(serverId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Server with ID $serverId not found")
        }
        if (!forceStop && server.status != ServerStatus.STOPPED) throw ResponseStatusException(
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
            PowerAction.START -> startServer(server)
            PowerAction.STOP -> stopServer(server)
            PowerAction.KILL -> killServer(server)
            PowerAction.RESTART -> {
                stopServer(server)
                startServer(server)
            }
        }
    }

    private fun startServer(server: ServerEntity) {
        log.info("Starting ${server.id}")
        provisioner.startServer(server)
    }

    private fun stopServer(server: ServerEntity) {
        if (server.containerId == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No container to stop for server ${server.id}")
        }
        containerService.stopContainer(server.id!!, server.containerId!!)
        server.status = ServerStatus.STOPPED
        serverRepository.save(server)
        log.info("Server ${server.id} stopped")
    }

    private fun killServer(server: ServerEntity) {
        if (server.containerId == null) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No container to kill for server ${server.id}")
        }
        containerService.stopContainer(server.id!!, server.containerId!!)
        containerService.removeContainer(server.containerId!!)
        server.containerId = null
        server.status = ServerStatus.STOPPED
        serverRepository.save(server)
        log.info("Server ${server.id} killed and container removed")
    }
}