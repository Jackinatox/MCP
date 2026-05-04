package com.scyed.clu.api.ws

import com.scyed.clu.api.ws.handlers.ConsoleWSHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor

@Configuration
@EnableWebSocket
class WebSocketConfig : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(consoleWSHandler(), "/console").addInterceptors(HttpSessionHandshakeInterceptor())
    }

    @Bean
    fun consoleWSHandler(): WebSocketHandler {
        return ConsoleWSHandler()
    }
}