package com.scyed.mcp

import com.scyed.mcp.docker.ServerProvisioner
import com.scyed.mcp.docker.ServerProvisioner.TriggerdBy
import com.scyed.mcp.docker.stuff.GlyphEnvVarValidator
import com.scyed.mcp.jpa.ServerEntity
import com.scyed.mcp.jpa.ServerStatus
import com.scyed.mcp.jpa.repositories.GlyphRepository
import com.scyed.mcp.jpa.repositories.ServerRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/{version}/server", version = "v1")
class ServerController {
    private final val serverRepository: ServerRepository
    private final val eventPublisher: ApplicationEventPublisher
    private final val glyphRepository: GlyphRepository
    private final val provisioner: ServerProvisioner
    private final val glyphEnvVarValidator: GlyphEnvVarValidator
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(
        servers: ServerRepository,
        eventPublisher: ApplicationEventPublisher,
        glyphRepository: GlyphRepository,
        provisioner: ServerProvisioner,
        glyphEnvVarValidator: GlyphEnvVarValidator
    ) {
        this.serverRepository = servers
        this.eventPublisher = eventPublisher
        this.glyphRepository = glyphRepository
        this.provisioner = provisioner
        this.glyphEnvVarValidator = glyphEnvVarValidator
    }

    @RequestMapping
    fun getAll(): ServerResponse {
        return ServerResponse(serverRepository.count(), serverRepository.findAll().toList())
    }

    @PostMapping
    fun create(@RequestBody @Valid request: CreateServerRequest): ServerEntity {
        if (serverRepository.existsByName(request.name)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is already taken")
        }
        val glyph = glyphRepository.findById(request.glyphId).orElseThrow() {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Glyph not found")
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
                glyph
            )
        )

        log.info("Creating server ${server.name} and id: ${server.id}")

        eventPublisher.publishEvent(
            ServerProvisioner.ServerReinstallRequested(
                requireNotNull(server.id) { "Server ID was null after save" })
        )
        return server
    }

    @PostMapping("{serverId}/reinstall")
    fun reinstall(
        @RequestBody @Valid request: ReinstallServerRequest, @PathVariable serverId: UUID
    ): ReinstallServerResponse {
        val server = serverRepository.findById(serverId).orElseThrow {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND, "Server with ID $serverId not found"
            )
        }

        if (!request.forceStop && server.status != ServerStatus.STOPPED) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Server must be stopped to reinstall or use forceStop=true"
            )
        } else {
            provisioner.killAndRemoveServer(ServerProvisioner.KillServerRequested(server))
        }


        log.info("Reinstalling server $serverId Request: $request")

        eventPublisher.publishEvent(
            ServerProvisioner.ServerReinstallRequested(serverId)
        )

        return ReinstallServerResponse(serverId, ServerStatus.INSTALLING)
    }

    @PostMapping("{serverId}/power")
    fun powerAction(@PathVariable serverId: UUID, @RequestParam action: ServerProvisioner.PowerAction) {
        val server = serverRepository.findById(serverId).orElseThrow {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND, "Server with ID $serverId not found"
            )
        }

        eventPublisher.publishEvent(
            ServerProvisioner.ServerPowerRequested(serverId, action, TriggerdBy.USER)
        )
        log.info("Powering action $action")
    }

    data class CreateServerRequest(
        @NotBlank val name: String,
        @NotBlank val imageName: String,
        val description: String? = null,
        @NotNull var cpuPercent: Long,
        @NotNull var memoryMb: Long,
        @NotNull var env: Map<String, String> = emptyMap(),
        @NotNull var glyphId: Long,
        var startCommand: String?,
    )

    data class ReinstallServerRequest(
        val deleteFiles: Boolean = false, val forceStop: Boolean = false
    )

    data class ReinstallServerResponse(val id: UUID, val status: ServerStatus)

    data class ServerResponse(
        val count: Long, val servers: List<ServerEntity>
    )
}