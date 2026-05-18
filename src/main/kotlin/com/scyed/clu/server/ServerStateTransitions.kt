package com.scyed.clu.server

import com.scyed.clu.infra.event.ContainerEvent
import com.scyed.clu.infra.event.ContainerEventBus
import org.springframework.stereotype.Component

@Component
class ServerStateTransitions(private val bus: ContainerEventBus) {

    fun starting(server: ServerEntity) {
        server.status.starting()
        publish(server)
    }

    fun started(server: ServerEntity) {
        server.status.started()
        publish(server)
    }

    fun stopping(server: ServerEntity) {
        server.status.stop()
        publish(server)
    }

    fun stopped(server: ServerEntity) {
        server.status.stopped()
        publish(server)
    }

    fun crashed(server: ServerEntity) {
        server.status.crashed()
        publish(server)
    }

    fun install(server: ServerEntity) {
        server.status.install()
        publish(server)
    }

    fun kill(server: ServerEntity) {
        server.status.kill()
        publish(server)
    }

    fun error(server: ServerEntity) {
        server.status.error()
        publish(server)
    }

    // Bypasses the state machine; for reconciliation against observed
    // reality (startup check, post-install) where no valid transition exists.
    fun force(server: ServerEntity, status: ServerStatus) {
        server.status = ServerState(status)
        bus.publish(ContainerEvent.ServerStatusChanged(server.id!!, status))
    }

    private fun publish(server: ServerEntity) {
        bus.publish(ContainerEvent.ServerStatusChanged(server.id!!, server.status.status))
    }
}
