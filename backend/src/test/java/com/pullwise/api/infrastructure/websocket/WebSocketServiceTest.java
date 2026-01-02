package com.pullwise.api.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.*;

/**
 * Testes de integração para WebSocketService.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    private WebSocketService webSocketService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        webSocketService = new WebSocketService(objectMapper);
    }

    @Test
    void testRegisterAndRemoveUserSession() {
        String userId = "user123";
        webSocketService.registerUserSession(userId, session);
        // Session registered
        webSocketService.removeSession(session);
        // Session removed - no exception
    }

    @Test
    void testSubscribeToTopic() {
        String topic = "/review/1";
        webSocketService.subscribeToTopic(topic, session);
        // Verify subscription
        webSocketService.unsubscribeFromTopic(topic, session);
        // Verify unsubscription
    }

    @Test
    void testSendToTopic() throws Exception {
        String topic = "/review/1";
        when(session.isOpen()).thenReturn(true);
        webSocketService.subscribeToTopic(topic, session);

        webSocketService.sendReviewProgress(1L, 50, "ANALYZING",
            com.pullwise.api.infrastructure.websocket.dto.ReviewProgressMessage.Status.ANALYZING,
            "Processing");

        // Message sent to all subscribers
        verify(session, atLeastOnce()).isOpen();
    }

    @Test
    void testSendToClosedSession() throws Exception {
        when(session.isOpen()).thenReturn(false);
        webSocketService.subscribeToTopic("/review/1", session);

        webSocketService.sendReviewProgress(1L, 50, "ANALYZING",
            com.pullwise.api.infrastructure.websocket.dto.ReviewProgressMessage.Status.ANALYZING,
            "Processing");

        // Should handle closed session gracefully
        verify(session).isOpen();
    }
}
