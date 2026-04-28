package com.scyed.mcp

import com.scyed.mcp.docker.ServerProvisioner
import com.scyed.mcp.jpa.Server
import com.scyed.mcp.jpa.ServerStatus
import com.scyed.mcp.jpa.ServerRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/{version}/server", version = "v1")
class ServerController {
    private final val serverRepository: ServerRepository
    private final val eventPublisher: ApplicationEventPublisher
    private val log = LoggerFactory.getLogger(javaClass)

    constructor(servers: ServerRepository, eventPublisher: ApplicationEventPublisher) {
        this.serverRepository = servers
        this.eventPublisher = eventPublisher
    }

    @RequestMapping
    fun getAll(): ServerResponse {
        return ServerResponse(serverRepository.count(), serverRepository.findAll().toList())
    }

    @PostMapping
    fun create(@RequestBody @Valid request: CreateServerRequest): Server {
        val server = serverRepository.save(
            Server(
                request.name,
                request.description,
                "",
                request.imageName,
                ServerStatus.PROVISIONING,
                false,
                request.memoryMb,
                request.cpuPercent,
                request.env
            )
        );

        log.info("Creating server ${server.name} and id: ${server.id}")

        eventPublisher.publishEvent(
            ServerProvisioner.ServerProvisioningRequested(
                requireNotNull(server.id) { "Server ID was null after save" })
        )
        return server;
    }

    data class CreateServerRequest(
        @NotBlank val name: String,
        @NotBlank val imageName: String,
        val description: String? = null,
        @NotNull val cpuPercent: Long,
        @NotNull val memoryMb: Long,
        @NotNull val env: Map<String, String> = emptyMap()
    )

    data class ServerResponse(
        val count: Long, val servers: List<Server>
    )
}