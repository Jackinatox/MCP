package com.scyed.clu.infra.event

import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Component
class ContainerEventBus {
    companion object {
        val GLOBAL = UUID(0, 0)  // or just a well-known constant
    }

    // ServerId Pair<ListenerId, Listener>
    private val map: ConcurrentMap<UUID, List<Pair<UUID, (ContainerEvent) -> Unit>>> = ConcurrentHashMap()


    fun subscribe(serverId: UUID, listener: (ContainerEvent) -> Unit): UUID {
        val listenerId = UUID.randomUUID()
        map.compute(serverId) { _, existing ->
            val entry = Pair(listenerId, listener)
            if (existing == null) listOf(entry) else existing + entry
        }
        return listenerId

    }

    fun unsubscribe(listenerId: UUID) {
        map.forEach { (serverId, _) ->
            map.compute(serverId) { _, existing ->
                existing?.filterNot { (id, _) -> id == listenerId }?.takeIf { it.isNotEmpty() }
            }
        }
    }

    fun publish(event: ContainerEvent) {
        map[event.serverId]?.forEach { (_, listener) -> listener(event) }
        map[GLOBAL]?.forEach { (_, listener) -> listener(event) }
    }
}