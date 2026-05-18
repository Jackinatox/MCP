package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.WaitResponse
import com.scyed.clu.infra.event.ContainerEvent
import com.scyed.clu.infra.event.ContainerEventBus
import com.scyed.clu.server.ServerStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@Component
class ContainerAttachmentManager(private val docker: DockerClient, private val bus: ContainerEventBus) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val attachments = ConcurrentHashMap<UUID, ContainerAttachment>()

    fun attach(serverId: UUID, containerId: String): ContainerAttachment {
        detach(serverId)

        val stdinPipe = PipedOutputStream()
        val stdinSource = PipedInputStream(stdinPipe)
        val lineBuffer = StringBuilder()
        // Either waitCallback (preferred) or streamCallback detach (fallback) publishes
        // the exit status — never both.
        val exitStatusPublished = AtomicBoolean(false)

        val streamCallback = object : ResultCallback.Adapter<Frame>() {
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
                publishExitStatusFallback(serverId, containerId, exitStatusPublished)
                bus.publish(ContainerEvent.Detached(serverId, containerId))
                super.onComplete()
            }

            override fun onError(throwable: Throwable) {
                log.error("Attach stream error for container $containerId", throwable)
                publishExitStatusFallback(serverId, containerId, exitStatusPublished)
                bus.publish(ContainerEvent.Detached(serverId, containerId))

                super.onError(throwable)
            }
        }

        docker.attachContainerCmd(containerId).withStdIn(stdinSource).withStdOut(true).withStdErr(true)
            .withFollowStream(true).exec(streamCallback)

        val waitCallback = object : WaitContainerResultCallback() {
            override fun onNext(waitResponse: WaitResponse?) {
                if (!exitStatusPublished.compareAndSet(false, true)) return
                val status = if (waitResponse?.statusCode == 0) ServerStatus.STOPPED else ServerStatus.ERROR
                bus.publish(ContainerEvent.ServerStatusChanged(serverId, status))
            }
        }

        docker.waitContainerCmd(containerId).exec(waitCallback)

        val attachment = ContainerAttachment(serverId, containerId, stdinPipe, streamCallback, waitCallback)
        attachments[serverId] = attachment
        log.info("Attached to container $containerId for server $serverId")
        return attachment
    }

    fun detach(serverId: UUID) {
        attachments.remove(serverId)?.close()
    }

    private fun publishExitStatusFallback(serverId: UUID, containerId: String, published: AtomicBoolean) {
        if (!published.compareAndSet(false, true)) return
        val status = runCatching {
            val state = docker.inspectContainerCmd(containerId).exec().state
            // Container still running means the stream just detached; let waitCallback handle the real exit.
            if (state?.running == true) {
                published.set(false)
                return
            }
            if (state?.exitCodeLong == 0L) ServerStatus.STOPPED else ServerStatus.ERROR
        }.getOrElse { e ->
            log.warn("Failed to inspect container $containerId after detach", e)
            ServerStatus.ERROR
        }
        bus.publish(ContainerEvent.ServerStatusChanged(serverId, status))
    }

    class ContainerAttachment(
        val serverId: UUID,
        val containerId: String,
        val stdin: PipedOutputStream,
        private val streamCallback: ResultCallback.Adapter<Frame>,
        private val waitCallback: WaitContainerResultCallback
    ) : AutoCloseable {
        override fun close() {
            runCatching {
                streamCallback.close()
                streamCallback.awaitCompletion()
            }
            runCatching { waitCallback.close() }
            runCatching { stdin.close() }
        }
    }
}