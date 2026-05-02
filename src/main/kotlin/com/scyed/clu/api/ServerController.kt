package com.scyed.clu.api

import com.scyed.clu.api.dto.CreateServerRequest
import com.scyed.clu.server.PowerAction
import com.scyed.clu.server.ServerEntity
import com.scyed.clu.server.ServerService
import com.scyed.clu.server.ServerStatus
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/{version}/server", version = "v1")
class ServerController(private val serverService: ServerService) {

    @RequestMapping
    fun getAll(): ServerResponse {
        val (count, servers) = serverService.getAllServers()
        return ServerResponse(count, servers)
    }

    @PostMapping
    fun create(@RequestBody @Valid request: CreateServerRequest): ServerEntity =
        serverService.createServer(request)

    @PostMapping("{serverId}/reinstall")
    fun reinstall(
        @RequestBody @Valid request: ReinstallServerRequest,
        @PathVariable serverId: UUID,
    ): ReinstallServerResponse {
        serverService.reinstallServer(serverId, request.forceStop)
        return ReinstallServerResponse(serverId, ServerStatus.INSTALLING)
    }

    @PostMapping("{serverId}/power")
    fun powerAction(@PathVariable serverId: UUID, @RequestParam action: PowerAction) =
        serverService.powerAction(serverId, action)

    data class ReinstallServerRequest(val deleteFiles: Boolean = false, val forceStop: Boolean = false)
    data class ReinstallServerResponse(val id: UUID, val status: ServerStatus)
    data class ServerResponse(val count: Long, val servers: List<ServerEntity>)
}
