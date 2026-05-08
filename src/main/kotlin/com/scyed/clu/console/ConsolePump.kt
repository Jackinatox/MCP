package com.scyed.clu.console

import com.fasterxml.jackson.databind.ObjectMapper
import com.scyed.clu.api.ws.message.ConsoleMessage
import com.scyed.clu.infra.event.ContainerEvent
import com.scyed.clu.infra.event.ContainerEventBus
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ConsolePump(private val bus: ContainerEventBus) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()
    private val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    @PostConstruct
    fun init() {
        bus.subscribe(ContainerEventBus.GLOBAL) { event ->
            if (event is ContainerEvent.ConsoleLine) {
                sendToWS(event.serverId, event.line)
            }
        }
    }

    private fun sendToWS(serverId: UUID, line: String) {
        logger.debug("{} --- {}", serverId, line)
        val message = TextMessage(objectMapper.writeValueAsString(ConsoleMessage(line)))
        sessions[serverId.toString()]?.forEach { session -> session.sendMessage(message) }
    }

    fun register(serverId: String, session: WebSocketSession) {
        sessions.getOrPut(serverId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unregister(serverId: String, session: WebSocketSession) {
        sessions[serverId]?.remove(session)
    }

    fun getSessions(serverId: String): Set<WebSocketSession> = sessions[serverId] ?: emptySet()
}