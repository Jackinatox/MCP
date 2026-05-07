package com.scyed.clu.console

import com.fasterxml.jackson.databind.ObjectMapper
import com.scyed.clu.api.ws.message.ConsoleMessage
import com.scyed.clu.provisioning.ContainerAttachmentManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@Component
class ConsolePump() {
    private final val logger = LoggerFactory.getLogger(javaClass)
    private final val objectMapper = ObjectMapper()
    private val attachments = ConcurrentHashMap<UUID, Thread>()
    private val sessions = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    fun start(attachment: ContainerAttachmentManager.ContainerAttachment) {
        val pumper = attachments.get(attachment.serverId)
        if (pumper != null) {
            if (pumper.isAlive) {
                pumper.interrupt()
                attachments.remove(attachment.serverId)
            }
        }
        attachments[attachment.serverId] = thread(
            start = true, isDaemon = true, name = "console-pump-${attachment.serverId}", priority = -1, block = {
                attachment.stdout.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                    sendToWS(attachment.serverId, line)
                }
            })
    }

    fun stop(serverId: UUID) {
        attachments.remove(serverId)?.interrupt()
    }

    private fun sendToWS(serverId: UUID, line: String) {
        logger.debug("{} --- {}", serverId, line)
        val message = TextMessage(objectMapper.writeValueAsString(ConsoleMessage(line)))
        sessions.get(serverId.toString())?.forEach { session -> session.sendMessage(message) }
    }

    fun register(serverId: String, session: WebSocketSession) {
        sessions.getOrPut(serverId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unregister(serverId: String, session: WebSocketSession) {
        sessions[serverId]?.remove(session)
    }

    fun getSessions(serverId: String): Set<WebSocketSession> = sessions[serverId] ?: emptySet()
}