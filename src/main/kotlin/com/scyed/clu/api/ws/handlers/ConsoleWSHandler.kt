package com.scyed.clu.api.ws.handlers

import org.slf4j.LoggerFactory
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

class ConsoleWSHandler: TextWebSocketHandler() {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        log.trace(message.payload)
    }
}