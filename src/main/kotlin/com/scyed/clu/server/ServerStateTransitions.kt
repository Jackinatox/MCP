package com.scyed.clu.server

import com.scyed.clu.db.entity.ServerEntity
import com.scyed.clu.db.enums.ServerState
import com.scyed.clu.db.enums.ServerStatus
import com.scyed.clu.db.repository.ServerRepository
import com.scyed.clu.infra.event.ContainerEvent
import com.scyed.clu.infra.event.ContainerEventBus
import org.springframework.stereotype.Component

@Component
class ServerStateTransitions(private val bus: ContainerEventBus, private val serverRepository: ServerRepository) {

    fun starting(server: ServerEntity): ServerEntity {
        server.status.starting()
        publish(server)
        return serverRepository.save(server);
    }

    fun started(server: ServerEntity): ServerEntity {
        server.status.started()
        publish(server)
        return serverRepository.save(server);
    }

    fun stopping(server: ServerEntity): ServerEntity {
        server.status.stop()
        publish(server)
        return serverRepository.save(server);
    }

    fun stopped(server: ServerEntity): ServerEntity {
        server.status.stopped()
        publish(server)
        return serverRepository.save(server);
    }

    fun crashed(server: ServerEntity): ServerEntity {
        server.status.crashed()
        publish(server)
        return serverRepository.save(server);
    }

    fun install(server: ServerEntity): ServerEntity {
        server.status.install()
        publish(server)
        return serverRepository.save(server);
    }

    fun kill(server: ServerEntity): ServerEntity {
        server.status.kill()
        publish(server)
        return serverRepository.save(server);
    }

    fun error(server: ServerEntity): ServerEntity {
        server.status.error()
        publish(server)
        return serverRepository.save(server);
    }

    fun deleting(server: ServerEntity): ServerEntity {
        server.status.deleting()
        publish(server)
        return serverRepository.save(server);
    }

    fun deleted(server: ServerEntity): ServerEntity {
        server.status.deleted()
        publish(server)
        return serverRepository.save(server);
    }

    // Bypasses the state machine; for reconciliation against observed
    // reality (startup check, post-install) where no valid transition exists.
    fun force(server: ServerEntity, status: ServerStatus): ServerEntity {
        server.status = ServerState(status)
        bus.publish(ContainerEvent.ServerStatusChanged(server.id!!, status))
        return serverRepository.save(server);
    }

    private fun publish(server: ServerEntity) {
        bus.publish(ContainerEvent.ServerStatusChanged(server.id!!, server.status.status))
    }
}
