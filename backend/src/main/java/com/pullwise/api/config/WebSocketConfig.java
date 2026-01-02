package com.pullwise.api.config;

import com.pullwise.api.infrastructure.websocket.ReviewWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuração de WebSocket para comunicação em tempo real.
 * Suporta ambos: STOMP (para legado) e WebSocket nativo (para V2).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ReviewWebSocketHandler reviewWebSocketHandler;

    public WebSocketConfig(ReviewWebSocketHandler reviewWebSocketHandler) {
        this.reviewWebSocketHandler = reviewWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Endpoint WebSocket principal para V2 (compatível com Socket.io client)
        registry.addHandler(reviewWebSocketHandler, "/ws")
                .setAllowedOrigins("*");
    }
}
