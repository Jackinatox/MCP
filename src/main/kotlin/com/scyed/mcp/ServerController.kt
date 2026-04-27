package com.scyed.mcp

import com.scyed.mcp.jpa.Server
import com.scyed.mcp.jpa.ServerStatus
import com.scyed.mcp.jpa.ServerbyImageName
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

@RestController
@RequestMapping("/server")
class ServerController {
    private final val serverRepository: ServerbyImageName

    constructor(servers: ServerbyImageName) {
        this.serverRepository = servers
    }

    @RequestMapping
    fun getAll(): ServerResponse {
        return ServerResponse(serverRepository.count(), serverRepository.findAll().toList())
    }

    @PostMapping
    fun create(@RequestBody @Valid request: CreateServerRequest): Server {
        return serverRepository.save(
            Server(
                request.name,
                request.description,
                request.imageName,
                ServerStatus.PROVISIONING,
                false,
                request.memoryMb,
                request.cpuPercent,
                request.env
            )
        );
    }

    data class CreateServerRequest(
        @NotBlank val name: String,
        @NotBlank val imageName: String,
        val description: String? = null,
        @NotNull val cpuPercent: Int,
        @NotNull val memoryMb: Long,
        @NotNull val env: Map<String, String> = emptyMap()
    )

    data class ServerResponse(
        val count: Long, val servers: List<Server>
    )
}