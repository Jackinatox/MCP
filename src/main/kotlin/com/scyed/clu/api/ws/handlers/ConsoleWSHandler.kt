package com.scyed.clu.api.ws.handlers

import com.scyed.clu.infra.event.handler.WSEventPump
import org.slf4j.LoggerFactory
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class ConsoleWSHandler(private val wsEventPump: WSEventPump) : TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.trace(message.payload)
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val serverId = session.uri?.path?.substringAfterLast("/") ?: return session.close()
        if (serverId.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }
        wsEventPump.register(serverId, session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val serverId = session.uri?.path?.substringAfterLast("/") ?: return session.close()
        if (serverId.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE)
            return
        }
        wsEventPump.unregister(serverId, session)
    }
}