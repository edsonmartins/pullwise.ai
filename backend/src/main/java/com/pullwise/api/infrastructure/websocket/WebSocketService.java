package com.pullwise.api.infrastructure.websocket;

import com.pullwise.api.infrastructure.websocket.dto.FixGeneratedMessage;
import com.pullwise.api.infrastructure.websocket.dto.IssueDetectedMessage;
import com.pullwise.api.infrastructure.websocket.dto.ReviewProgressMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Serviço para gerenciar conexões WebSocket e envio de mensagens em tempo real.
 */
@Service
public class WebSocketService {

    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> topicSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public WebSocketService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Registra uma sessão WebSocket para um usuário.
     */
    public void registerUserSession(String userId, WebSocketSession session) {
        userSessions.put(userId, session);
    }

    /**
     * Remove uma sessão WebSocket.
     */
    public void removeSession(WebSocketSession session) {
        userSessions.values().remove(session);
        topicSessions.values().forEach(sessions -> sessions.remove(session));
    }

    /**
     * Inscreve uma sessão em um tópico específico.
     */
    public void subscribeToTopic(String topic, WebSocketSession session) {
        topicSessions.computeIfAbsent(topic, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    /**
     * Desinscreve uma sessão de um tópico.
     */
    public void unsubscribeFromTopic(String topic, WebSocketSession session) {
        Set<WebSocketSession> sessions = topicSessions.get(topic);
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    /**
     * Envia mensagem de progresso de review para todos os inscritos no tópico.
     */
    public void sendReviewProgress(Long reviewId, int progress, String stage, ReviewProgressMessage.Status status, String message) {
        ReviewProgressMessage.ReviewProgressData data = ReviewProgressMessage.ReviewProgressData.create(
            reviewId, progress, stage, status, message
        );
        ReviewProgressMessage msg = ReviewProgressMessage.create("review.progress", data);
        sendToTopic("/review/" + reviewId, msg);
    }

    /**
     * Envia notificação de issue detectada.
     */
    public void sendIssueDetected(Long reviewId, Long issueId, String severity, String type, String title, String filePath, Integer lineStart) {
        IssueDetectedMessage.IssueData data = new IssueDetectedMessage.IssueData(
            reviewId, issueId, severity, type, title, filePath, lineStart
        );
        IssueDetectedMessage msg = new IssueDetectedMessage("issue.detected", data, java.time.Instant.now());
        sendToTopic("/review/" + reviewId, msg);
    }

    /**
     * Envia notificação de fix gerado.
     */
    public void sendFixGenerated(Long suggestionId, Long issueId, String status, String modelUsed) {
        FixGeneratedMessage msg = FixGeneratedMessage.create(suggestionId, issueId, status, modelUsed);
        sendToTopic("/autofix", msg);
    }

    /**
     * Envia notificação de plugin status alterado.
     */
    public void sendPluginStatusChanged(String pluginId, String status, String message) {
        Map<String, Object> data = Map.of(
            "type", "plugin.status",
            "data", Map.of(
                "pluginId", pluginId,
                "status", status,
                "message", message
            ),
            "timestamp", java.time.Instant.now().toString()
        );
        sendToTopic("/plugins", data);
    }

    /**
     * Envia mensagem para todas as sessões inscritas em um tópico.
     */
    public void sendToTopic(String topic, Object message) {
        Set<WebSocketSession> sessions = topicSessions.get(topic);
        if (sessions != null) {
            String payload = toJsonString(message);
            sessions.forEach(session -> sendToSession(session, payload));
        }
    }

    /**
     * Envia mensagem para um usuário específico.
     */
    public void sendToUser(String userId, Object message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendToSession(session, message);
        }
    }

    /**
     * Envia mensagem para uma sessão específica.
     */
    private void sendToSession(WebSocketSession session, Object message) {
        sendToSession(session, toJsonString(message));
    }

    /**
     * Envia string JSON para uma sessão específica.
     */
    private void sendToSession(WebSocketSession session, String payload) {
        if (session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                // Session might be closed
                removeSession(session);
            }
        }
    }

    /**
     * Converte objeto para JSON string.
     */
    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Verifica se há sessões ativas para um tópico.
     */
    public boolean hasActiveSubscriptions(String topic) {
        Set<WebSocketSession> sessions = topicSessions.get(topic);
        return sessions != null && !sessions.isEmpty();
    }
}
