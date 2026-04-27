package com.scyed.mcp.docker

import org.springframework.stereotype.Service

@Service
class DockerProvisioningService : Provisioning {
    override fun createSerevr(test: ServerConfig): String {
        System.out.println(test)
        return test.toString()
    }
}