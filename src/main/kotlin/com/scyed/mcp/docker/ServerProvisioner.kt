package com.scyed.mcp.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.HostConfig
import com.scyed.mcp.jpa.ServerRepository
import com.scyed.mcp.jpa.ServerStatus
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

@Component
open class ServerProvisioner(
    private val docker: DockerClient, private val serverRepository: ServerRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async("provisioningExecutor")
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun onProvisioningrequest(event: ServerProvisioningRequested) {
        val server = serverRepository.findById(event.serverId).orElseThrow()
        log.info("Provisioning request for ${server.id}")
        try {
            val container = docker.createContainerCmd(server.image)
                .withName(server.name)
                .withHostConfig(
                    HostConfig.newHostConfig()
                        .withCpuPercent(server.cpuPercent)
                        .withMemory(server.memoryMb * 1024L * 1024L) // bytes!
                )
                .exec()

            server.containerId = container.id
            server.status = ServerStatus.STOPPED
            serverRepository.save(server)
        } catch (e: Exception) {
            log.error("Provisioning failed for server ${server.id}", e)
            server.status = ServerStatus.ERROR // TODO: Maybe creation failed
//            server.failureReason = e.message
            serverRepository.save(server)
        }
    }


    data class ServerProvisioningRequested(val serverId: UUID)

}