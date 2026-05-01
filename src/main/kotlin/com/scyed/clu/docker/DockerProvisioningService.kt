package com.scyed.clu.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.model.Info
import com.github.dockerjava.core.DefaultDockerClientConfig
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

    override fun createServer(serverConfig: ServerConfig): String {
        return "Dummy";
    }

    override fun reinstallServer(serverId: String) {
        TODO("Not yet implemented")
    }

    override fun getStatus(): Info {
        return docker.infoCmd().exec()
    }
}