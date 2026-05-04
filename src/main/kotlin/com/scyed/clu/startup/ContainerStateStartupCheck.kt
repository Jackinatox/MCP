package com.scyed.clu.startup

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.scyed.clu.server.ServerEntity
import com.scyed.clu.server.ServerRepository
import com.scyed.clu.server.ServerStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ContainerStateStartupCheck(
    private val dockerClient: DockerClient, private val serverRepository: ServerRepository
) : StartupCheck {
    private val log = LoggerFactory.getLogger(javaClass)
    override val name: String = "ContainerStateStartupCheck"

    override fun getOSType(): StartupCheck.OSType {
        return StartupCheck.OSType.BOTH
    }

    override fun runCheck() {
        val servers = serverRepository.findAll();

        log.info("$name server-count: ${servers.count()}")

        servers.forEach { server ->
            val newStatus = resolveStatus(server)

            if (newStatus != null && newStatus != server.status) {
                server.status = newStatus
                if (newStatus == ServerStatus.STOPPED) server.containerId = null
                log.info("New container Status: $newStatus - ${server.id}")
                serverRepository.save(server)
            }
        }
    }

    private fun resolveStatus(server: ServerEntity): ServerStatus {
        log.trace("Resolving container state for ${server.id}")
        val cid = server.containerId

        // server was mid-install when app crashed — install did not complete
        if (server.status in setOf(ServerStatus.PROVISIONING, ServerStatus.INSTALLING)) {
            return ServerStatus.ERROR
        }

        if (cid.isNullOrBlank()) {
            throw RuntimeException("Container cid not set during startup check for server ${server.name} - ${server.id}")
        }

        return try {
            val state = dockerClient.inspectContainerCmd(cid).exec().state
            if (state?.running == true) ServerStatus.RUNNING else ServerStatus.STOPPED
        } catch (e: NotFoundException) {
            ServerStatus.STOPPED  // container gone, mark stopped + clear containerId
        }
    }

}