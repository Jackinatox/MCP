package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.scyed.clu.infra.event.ContainerEvent
import com.scyed.clu.infra.event.ContainerEventBus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ContainerAttachmentManager(private val docker: DockerClient, private val bus: ContainerEventBus) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val attachments = ConcurrentHashMap<UUID, ContainerAttachment>()

    fun attach(serverId: UUID, containerId: String): ContainerAttachment {
        detach(serverId)

        val stdinPipe = PipedOutputStream()
        val stdinSource = PipedInputStream(stdinPipe)
        val lineBuffer = StringBuilder()

        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                lineBuffer.append(String(frame.payload, Charsets.UTF_8))
                var newlineIdx = lineBuffer.indexOf('\n')
                while (newlineIdx >= 0) {
                    val line = lineBuffer.substring(0, newlineIdx).trimEnd('\r')
                    lineBuffer.delete(0, newlineIdx + 1)
                    if (line.isNotEmpty()) bus.publish(ContainerEvent.ConsoleLine(serverId, line))
                    newlineIdx = lineBuffer.indexOf('\n')
                }
            }

            override fun onComplete() {
                if (lineBuffer.isNotEmpty()) {
                    bus.publish(ContainerEvent.ConsoleLine(serverId, lineBuffer.toString().trimEnd('\r')))
                    lineBuffer.clear()
                }
                bus.publish(ContainerEvent.Detached(serverId, containerId))
                super.onComplete()
            }

            override fun onError(throwable: Throwable) {
                log.error("Attach stream error for container $containerId", throwable)
                bus.publish(ContainerEvent.Detached(serverId, containerId))
                super.onError(throwable)
            }
        }

        docker.attachContainerCmd(containerId).withStdIn(stdinSource).withStdOut(true).withStdErr(true).withFollowStream(true).exec(callback)

        val attachment = ContainerAttachment(serverId, containerId, stdinPipe, callback)
        attachments[serverId] = attachment
        log.info("Attached to container $containerId for server $serverId")
        return attachment
    }

    fun detach(serverId: UUID) {
        attachments.remove(serverId)?.close()
    }

    class ContainerAttachment(
        val serverId: UUID,
        val containerId: String,
        val stdin: PipedOutputStream,
        private val callback: ResultCallback.Adapter<Frame>
    ) : AutoCloseable {
        override fun close() {
            runCatching { callback.close() }
            runCatching { stdin.close() }
        }
    }
}