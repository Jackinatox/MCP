package com.scyed.mcp.docker

import com.github.dockerjava.api.model.Info

interface Provisioning {
    fun createSerevr(test: ServerConfig): String
    fun getStatus(): Info
}