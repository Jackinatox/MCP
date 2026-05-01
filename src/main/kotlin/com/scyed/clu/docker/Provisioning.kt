package com.scyed.clu.docker

import com.github.dockerjava.api.model.Info

interface Provisioning {
    fun createServer(serverConfig: ServerConfig): String
    fun reinstallServer(serverId: String)

    fun getStatus(): Info
}