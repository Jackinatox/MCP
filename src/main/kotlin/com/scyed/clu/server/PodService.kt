package com.scyed.clu.server

import com.scyed.clu.api.dto.CreatePodRequest
import com.scyed.clu.db.entity.PodEntity
import com.scyed.clu.db.repository.PodRepository
import com.scyed.clu.infra.zfs.ZFSManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom

@Service
class PodService(private val podRepository: PodRepository, private val zfsManager: ZFSManager) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllPods(): Pair<Long, List<PodEntity>> =
        podRepository.count() to podRepository.findAll().toList()

    fun createPod(request: CreatePodRequest): PodEntity {
        var datasetName: String = "";
        for (i in 0..5) {
            datasetName = generateAlphanumericId(12);
            if (!podRepository.existsByDatasetName(datasetName)) {
                break
            }
            if (i >= 5) {
                throw RuntimeException("Couldnt generate datasetname after 5 tries")
            }
        }


        val pod = podRepository.save(
            PodEntity(
                cpuPercent = request.cpuPercent,
                memoryMb = request.memoryMb,
                diskMb = request.diskMb,
                backups = request.backups,
                datasetName = datasetName
            )
        )
        log.info("Created pod id=${pod.id}")


        if (!zfsManager.createPodDataset(pod).isSuccess) {
            throw Exception("Failed to create pod")
        }

        return pod
    }

    private fun generateAlphanumericId(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random: SecureRandom = SecureRandom()
        val sb = StringBuilder(length)
        for (i in 0..<length) {
            sb.append(chars.get(random.nextInt(chars.length)))
        }
        return sb.toString()
    }
}
