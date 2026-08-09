package com.pullwise.api.config;

import com.pullwise.api.infrastructure.websocket.ReviewWebSocketHandler;
import com.pullwise.api.infrastructure.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * Configuração de WebSocket para comunicação em tempo real.
 * Suporta ambos: STOMP (para legado) e WebSocket nativo (para V2).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ReviewWebSocketHandler reviewWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(ReviewWebSocketHandler reviewWebSocketHandler,
                           WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.reviewWebSocketHandler = reviewWebSocketHandler;
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Endpoint WebSocket principal para V2 (compatível com Socket.io client)
        registry.addHandler(reviewWebSocketHandler, "/ws")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins(
                        "http://localhost:3000",
                        "http://localhost:3001",
                        "http://localhost:3002",
                        "https://pullwise.ai",
                        "https://www.pullwise.ai"
                );
    }
}
