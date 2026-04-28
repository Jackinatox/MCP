package com.scyed.mcp.docker

import com.github.dockerjava.api.model.Info
import com.scyed.mcp.game.Glyph

interface Provisioning {
    fun createServer(serverConfig: ServerConfig): String
    fun reinstallServer(serverId: String)

    fun getStatus(): Info
}