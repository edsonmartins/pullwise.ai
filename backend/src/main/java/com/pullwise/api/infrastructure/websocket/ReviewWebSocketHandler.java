package com.pullwise.api.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * WebSocket handler para comunicação em tempo real com o frontend.
 * Suporta mensagens de progresso de review, issues detectadas, etc.
 */
@Component
public class ReviewWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ReviewWebSocketHandler.class);

    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    public ReviewWebSocketHandler(WebSocketService webSocketService, ObjectMapper objectMapper) {
        this.webSocketService = webSocketService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("[WebSocket] Conexão estabelecida: sessionId={}", session.getId());

        // Envia mensagem de boas-vindas
        Map<String, Object> welcome = Map.of(
            "type", "connected",
            "data", Map.of(
                "sessionId", session.getId(),
                "serverTime", System.currentTimeMillis()
            )
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("[WebSocket] Mensagem recebida: sessionId={}, payload={}", session.getId(), payload);

        try {
            Map<String, Object> msg = objectMapper.readValue(payload, Map.class);
            String type = (String) msg.get("type");
            Map<String, Object> data = (Map<String, Object>) msg.get("data");

            switch (type) {
                case "subscribe:review" -> {
                    Long reviewId = getLong(data, "reviewId");
                    if (reviewId != null) {
                        webSocketService.subscribeToTopic("/review/" + reviewId, session);
                        log.info("[WebSocket] Session {} inscrito no review {}", session.getId(), reviewId);
                    }
                }
                case "unsubscribe:review" -> {
                    Long reviewId = getLong(data, "reviewId");
                    if (reviewId != null) {
                        webSocketService.unsubscribeFromTopic("/review/" + reviewId, session);
                    }
                }
                case "subscribe:project" -> {
                    Long projectId = getLong(data, "projectId");
                    if (projectId != null) {
                        webSocketService.subscribeToTopic("/project/" + projectId, session);
                    }
                }
                case "join:org" -> {
                    String orgId = (String) data.get("orgId");
                    if (orgId != null) {
                        webSocketService.subscribeToTopic("/org/" + orgId, session);
                    }
                }
                case "ping" -> {
                    // Responde ao ping com pong
                    Map<String, Object> pong = Map.of("type", "pong");
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
                }
                default -> log.debug("[WebSocket] Tipo de mensagem não reconhecido: {}", type);
            }

        } catch (Exception e) {
            log.error("[WebSocket] Erro ao processar mensagem", e);
            sendError(session, "INVALID_MESSAGE", "Erro ao processar mensagem: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("[WebSocket] Conexão encerrada: sessionId={}, status={}", session.getId(), status);
        webSocketService.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[WebSocket] Erro de transporte: sessionId={}", session.getId(), exception);
        webSocketService.removeSession(session);
    }

    private void sendError(WebSocketSession session, String code, String message) {
        try {
            Map<String, Object> error = Map.of(
                "type", "error",
                "data", Map.of(
                    "code", code,
                    "message", message
                )
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            log.error("[WebSocket] Erro ao enviar mensagem de erro", e);
        }
    }

    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
