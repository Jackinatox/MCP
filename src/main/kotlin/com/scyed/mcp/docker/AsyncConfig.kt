package com.scyed.mcp.docker

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

class AsyncConfig {
    @Configuration
    @EnableAsync
    class AsyncConfig {
        @Bean(name = ["provisioningExecutor"])
        fun provisioningExecutor(): Executor =
            ThreadPoolTaskExecutor().apply {
                corePoolSize = 4
                maxPoolSize = 8
                queueCapacity = 100
                setThreadNamePrefix("provisioning-")
                initialize()
            }
    }
}