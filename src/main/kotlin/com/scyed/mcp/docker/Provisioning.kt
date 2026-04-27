package com.scyed.mcp.docker

interface Provisioning {
    fun createSerevr(test: ServerConfig): String
}