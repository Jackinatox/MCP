package com.scyed.clu.startup

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.scyed.clu.provisioning.ContainerService
import com.scyed.clu.server.ServerEntity
import com.scyed.clu.server.ServerRepository
import com.scyed.clu.server.ServerState
import com.scyed.clu.server.ServerStateTransitions
import com.scyed.clu.server.ServerStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContainerStateStartupCheck(
    private val dockerClient: DockerClient,
    private val serverRepository: ServerRepository,
    private val containerService: ContainerService,
    private val transitions: ServerStateTransitions,
) : StartupCheck {
    private val log = LoggerFactory.getLogger(javaClass)
    override val name: String = "ContainerStateStartupCheck"

    override fun getOSType(): StartupCheck.OSType {
        return StartupCheck.OSType.BOTH
    }

    override fun runCheck() {
        val servers = serverRepository.findByNotStatus(ServerState(ServerStatus.DELETED));

        log.info("$name server-count: ${servers.count()}")

        servers.forEach { server ->
            val newStatus = resolveStatus(server)

            server.containerId = newStatus.cid
            serverRepository.save(server)
            transitions.force(server, newStatus.state)
            log.info("New container Status: ${newStatus.state} - ${server.id} id: ${server.containerId}")
        }
    }

    private fun resolveStatus(server: ServerEntity): ServerStateCheck {
        log.trace("Resolving container state for ${server.id}")
        val cid = server.containerId

        if (cid.isNullOrBlank()) {
            return ServerStateCheck(ServerStatus.STOPPED, null)
//            throw RuntimeException("Container cid not set during startup check for server ${server.name} - ${server.id}")
        }

        // server was mid-install when app crashed — install did not complete
        if (server.status.status in setOf(ServerStatus.PROVISIONING, ServerStatus.INSTALLING)) {
            log.warn("Removing container cause it was ${server.status.status}")
            containerService.removeContainer(server)
            return ServerStateCheck(ServerStatus.ERROR, null)
        }


        return try {
            val state = dockerClient.inspectContainerCmd(cid).exec().state
            if (state?.running == true) ServerStateCheck(
                ServerStatus.STARTED, cid
            ) else ServerStateCheck(ServerStatus.STOPPED, cid)
        } catch (e: NotFoundException) {
            return ServerStateCheck(ServerStatus.STOPPED, null) // container gone, mark stopped + clear containerId
        }
    }

    data class ServerStateCheck(val state: ServerStatus, val cid: String?)

}