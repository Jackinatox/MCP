package com.scyed.clu.db.enums

import com.fasterxml.jackson.annotation.JsonValue

class ServerState(status: ServerStatus) {
    @get:JsonValue
    var status: ServerStatus = status
        private set

    @Throws(BadServerStateException::class)
    fun started(): ServerState {
        if (this.status == ServerStatus.STARTED) {
            throw BadServerStateException(status, ServerStatus.STARTED, "Server is already started")
        }

        this.status = ServerStatus.STARTED
        return this
    }

    @Throws(BadServerStateException::class)
    fun starting(): ServerState {
        if (this.status == ServerStatus.STARTING) {
            throw BadServerStateException(status, ServerStatus.STARTING, "Server is already started")
        }

        this.status = ServerStatus.STARTING
        return this
    }

    @Throws(BadServerStateException::class)
    fun stop(): ServerState {
        if (this.status in arrayOf(ServerStatus.STOPPING, ServerStatus.STOPPED)) {
            throw BadServerStateException(status, ServerStatus.STOPPING, "Server is already stopped/stopping")
        }

        this.status = ServerStatus.STOPPING
        return this
    }

    @Throws(BadServerStateException::class)
    fun deleting(): ServerState {
        if (this.status in arrayOf(ServerStatus.INSTALLING, ServerStatus.STOPPING)) {
            throw BadServerStateException(status, ServerStatus.STOPPING, "Server cant do that now")
        }

        if (this.status == ServerStatus.STOPPING) {
            throw BadServerStateException(
                status,
                ServerStatus.STOPPING,
                "Server is starting, use kill or wait until its started"
            )
        }

        this.status = ServerStatus.DELETING
        return this;
    }

    @Throws(BadServerStateException::class)
    fun deleted(): ServerState {
        if (this.status != ServerStatus.DELETING) {
            throw BadServerStateException(status, ServerStatus.DELETED, "Server deletion hasnt started")
        }
        this.status = ServerStatus.DELETED
        return this
    }

    @Throws(BadServerStateException::class)
    fun install(): ServerState {
        if (this.status in arrayOf(ServerStatus.STARTED, ServerStatus.TRANSFERRING_LOCKED)) {
            throw BadServerStateException(status, ServerStatus.STARTED, "Server is already started")
        }

        this.status = ServerStatus.STARTED
        return this
    }

    @Throws(BadServerStateException::class)
    fun kill(): ServerState {
        if (this.status in arrayOf(
                ServerStatus.INSTALLING,
                ServerStatus.TRANSFERRING_LOCKED,
                ServerStatus.PROVISIONING
            )
        ) {
            throw BadServerStateException(status, ServerStatus.STARTED, "Server is busy")
        }

        this.status = ServerStatus.INSTALLING
        return this
    }

    @Throws(BadServerStateException::class)
    fun stopped(): ServerState {
        this.status = ServerStatus.STOPPED
        return this
    }

    @Throws(BadServerStateException::class)
    fun crashed(): ServerState {
        this.status = ServerStatus.CRASHED
        return this
    }

    @Throws(BadServerStateException::class)
    fun error(): ServerState {
        this.status = ServerStatus.ERROR
        return this
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