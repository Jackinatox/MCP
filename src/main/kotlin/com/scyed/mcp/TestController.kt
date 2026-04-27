package com.scyed.mcp

import com.scyed.mcp.docker.Provisioning
import com.scyed.mcp.docker.ServerConfig
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController {
    private final val test: ServerConfig = ServerConfig("TestName", "Java21")
    private final val provisioning: Provisioning

    constructor(provisioning: Provisioning) {
        this.provisioning = provisioning
    }

    @RequestMapping("/")
    fun index(): String {
        val result: String = provisioning.createSerevr(test)
        return result
    }
}