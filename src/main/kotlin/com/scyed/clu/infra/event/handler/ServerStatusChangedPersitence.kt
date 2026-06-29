package com.scyed.clu.infra.event.handler

import com.scyed.clu.infra.event.ContainerEvent
import com.scyed.clu.infra.event.ContainerEventBus
import com.scyed.clu.db.repository.ServerRepository
import com.scyed.clu.db.enums.ServerState
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ServerStatusChangedPersitence(
    private val bus: ContainerEventBus, private val serverRepository: ServerRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        bus.subscribe(ContainerEventBus.GLOBAL) { event ->
            if (event is ContainerEvent.ServerStatusChanged) {
                log.info("Updating server status for serverId: ${event.serverId} to ${event.newStatus}")
                serverRepository.updateStatus(event.serverId, ServerState(event.newStatus))
            }
        }
    }
}