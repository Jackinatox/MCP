package com.scyed.clu.provisioning

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class ContainerAttachmentManager(private val docker: DockerClient) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val attachments = ConcurrentHashMap<UUID, ContainerAttachment>()


    fun attach(serverId: UUID, containerId: String): ContainerAttachment {
        detach(serverId)

        val stdinPipe = PipedOutputStream()
        val stdinSource = PipedInputStream(stdinPipe)

        val stdoutSink = PipedOutputStream()
        val stdoutPipe = PipedInputStream(stdoutSink)

        val callback = object : ResultCallback.Adapter<Frame>() {
            override fun onNext(frame: Frame) {
                try {
                    stdoutSink.write(frame.payload)
                    stdoutSink.flush()
                } catch (_: IOException) {
                    // pipe closed ? consumer is gone, ignore
                }
            }

            override fun onComplete() {
                runCatching { stdoutSink.close() }
                super.onComplete()
            }

            override fun onError(throwable: Throwable) {
                log.error("Attach stream error for container $containerId", throwable)
                runCatching { stdoutSink.close() }
                super.onError(throwable)
            }
        }

        docker.attachContainerCmd(containerId).withStdIn(stdinSource).withStdOut(true).withStdErr(true).withFollowStream(true).exec(callback)

        val attachment = ContainerAttachment(serverId, containerId, stdinPipe, stdoutPipe, callback)
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
        val stdout: PipedInputStream,
        private val callback: ResultCallback.Adapter<Frame>
    ) : AutoCloseable {
        override fun close() {
            runCatching { callback.close() }
            runCatching { stdin.close() }
            runCatching { stdout.close() }
        }

    }
}