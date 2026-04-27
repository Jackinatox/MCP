package com.scyed.mcp

import com.scyed.mcp.jpa.Server
import com.scyed.mcp.jpa.ServerbyImageName
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/server")
class ServerController {
    private final val serverRepository: ServerbyImageName

    constructor(servers: ServerbyImageName) {
        this.serverRepository = servers
    }

    @RequestMapping
    fun getAll(): Iterable<Server> {
        return serverRepository.findAll();
    }
}