package com.scyed.clu.api

import com.scyed.clu.api.dto.CreatePodRequest
import com.scyed.clu.db.entity.PodEntity
import com.scyed.clu.db.entity.ServerEntity
import com.scyed.clu.db.repository.PodRepository
import com.scyed.clu.db.repository.ServerRepository
import com.scyed.clu.server.PodService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/{version}/pod", version = "v1")
class PodController(private val podService: PodService, val serverRepository: ServerRepository) {

    @RequestMapping
    fun getAll(): PodResponse {
        val (count, pods) = podService.getAllPods()
        return PodResponse(count, pods)
    }

    @PostMapping
    fun create(@RequestBody @Valid request: CreatePodRequest): PodEntity = podService.createPod(request)

    @GetMapping("{podId}")
    fun getServersForPod(@PathVariable podId: UUID): ServerController.ServerResponse {
        var servers = serverRepository.findByPodEntityId(podId)

        return ServerController.ServerResponse(servers.count().toLong(), servers)
    }

    data class PodResponse(val count: Long, val pods: List<PodEntity>)
}
