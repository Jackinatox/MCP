package com.scyed.clu.server

import kotlin.jvm.Throws

class ServerState(status: ServerStatus) {
    var status: ServerStatus = status
        private set

    @Throws(BadServerStateException::class)
    fun start() {
        if (this.status == ServerStatus.STARTED) {
            throw BadServerStateException(status, ServerStatus.STARTED, "Server is already started")
        }

        this.status = ServerStatus.STARTED
    }

    @Throws(BadServerStateException::class)
    fun stop() {
        if (this.status in arrayOf(ServerStatus.STOPPING, ServerStatus.STOPPED))   {
            throw BadServerStateException(status, ServerStatus.STOPPING, "Server is already stopped/stopping")
        }

        if (this.status == ServerStatus.STARTING) {
            throw BadServerStateException(
                status,
                ServerStatus.STOPPING,
                "Server is starting, use kill or wait until its started"
            )
        }

        this.status = ServerStatus.STARTED
    }

    @Throws(BadServerStateException::class)
    fun install() {
        if (this.status in arrayOf(ServerStatus.STARTED, ServerStatus.TRANSFERRING_LOCKED)) {
            throw BadServerStateException(status, ServerStatus.STARTED, "Server is already started")
        }

        this.status = ServerStatus.STARTED
    }

    @Throws(BadServerStateException::class)
    fun kill() {
        if (this.status in arrayOf(ServerStatus.INSTALLING, ServerStatus.TRANSFERRING_LOCKED, ServerStatus.PROVISIONING)) {
            throw BadServerStateException(status, ServerStatus.STARTED, "Server is busy")
        }

        this.status = ServerStatus.STOPPED
    }

    @Throws(BadServerStateException::class)
    fun error() {
        this.status = ServerStatus.ERROR
    }


    override fun toString(): String {
        return status.toString()
    }


    class BadServerStateException(
        val currentSate: ServerStatus, val newState: ServerStatus, val customMessage: String
    ) : Exception() {
        override val message: String?
            get() = "Cant transition from $currentSate to $newState because: $customMessage"
    }

}