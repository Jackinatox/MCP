package com.scyed.clu.server

import com.scyed.clu.api.dto.CreatePodRequest
import com.scyed.clu.db.entity.PodEntity
import com.scyed.clu.db.repository.PodRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PodService(private val podRepository: PodRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllPods(): Pair<Long, List<PodEntity>> =
        podRepository.count() to podRepository.findAll().toList()

    fun createPod(request: CreatePodRequest): PodEntity {
        val pod = podRepository.save(
            PodEntity(
                cpuPercent = request.cpuPercent,
                memoryMb = request.memoryMb,
                diskMb = request.diskMb,
                backups = request.backups,
            )
        )
        log.info("Created pod id=${pod.id}")
        return pod
    }
}
