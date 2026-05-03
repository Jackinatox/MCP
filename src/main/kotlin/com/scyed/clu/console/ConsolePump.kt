package com.scyed.clu.console

import com.scyed.clu.provisioning.ContainerAttachmentManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@Component
class ConsolePump {
    private final val logger = LoggerFactory.getLogger(javaClass)
    private val attachments = ConcurrentHashMap<UUID, Thread>()

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
    }
}