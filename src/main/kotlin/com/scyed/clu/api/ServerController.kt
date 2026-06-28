package com.scyed.clu.api

import com.scyed.clu.api.dto.CreateServerRequest
import com.scyed.clu.server.PowerAction
import com.scyed.clu.server.ServerEntity
import com.scyed.clu.server.ServerService
import com.scyed.clu.server.ServerState
import com.scyed.clu.server.ServerStatus
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/{version}/server", version = "v1")
class ServerController(private val serverService: ServerService) {

    @RequestMapping
    fun getAll(): ServerResponse {
        val (count, servers) = serverService.getAllServers()
        return ServerResponse(count, servers)
    }

    @PostMapping
    fun create(@RequestBody @Valid request: CreateServerRequest): ServerEntity = serverService.createServer(request)

    @PostMapping("{serverId}/reinstall")
    fun reinstall(
        @RequestBody @Valid request: ReinstallServerRequest,
        @PathVariable serverId: UUID,
    ): ReinstallServerResponse {
        serverService.reinstallServer(serverId, request.forceStop)
        return ReinstallServerResponse(serverId, ServerStatus.INSTALLING)
    }

    @PostMapping("{serverId}/power")
    fun powerAction(@PathVariable serverId: UUID, @RequestParam action: PowerAction) {

        serverService.powerAction(serverId, action)
    }

    @DeleteMapping("{serverId}")
    fun delete(@PathVariable serverId: UUID) {
        serverService.deleteServer(serverId)
    }

    data class ReinstallServerRequest(val deleteFiles: Boolean = false, val forceStop: Boolean = false)
    data class ReinstallServerResponse(val id: UUID, val status: ServerStatus)
    data class ServerResponse(val count: Long, val servers: List<ServerEntity>)
}
