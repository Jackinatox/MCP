package com.scyed.mcp.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Info
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient
import org.springframework.stereotype.Service
import java.time.Duration


@Service
class DockerProvisioningService : Provisioning {
    private final val docker: DockerClient

    constructor() {
        // Get config based of environment
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()

        val httpClient: DockerHttpClient = ApacheDockerHttpClient.Builder().dockerHost(config!!.getDockerHost())
            .sslConfig(config.getSSLConfig()) // null for unix socket, that's fine
            .maxConnections(100).connectionTimeout(Duration.ofSeconds(30)).responseTimeout(Duration.ofSeconds(45))
            .build()


        docker = DockerClientImpl.getInstance(config, httpClient)
        docker.pingCmd().exec()
    }

    override fun createSerevr(test: ServerConfig): String {
        System.out.println(test)
        return test.toString()
    }

    override fun getStatus(): Info {
        return docker.infoCmd().exec()
    }
}