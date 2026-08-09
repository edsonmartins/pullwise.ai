package com.pullwise.api.infrastructure.websocket;

import com.pullwise.api.application.service.auth.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Testes do interceptor de autenticação do WebSocket.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private WebSocketHandler wsHandler;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService);
    }

    @Test
    void acceptsValidTokenAndStoresUserId() {
        when(jwtService.isTokenValid("valid-token")).thenReturn(true);
        when(jwtService.extractSubject("valid-token")).thenReturn("user-42");

        Map<String, Object> attributes = new HashMap<>();
        boolean accepted = interceptor.beforeHandshake(
                handshakeRequest("token=valid-token"), new ServletServerHttpResponse(new MockHttpServletResponse()),
                wsHandler, attributes);

        assertThat(accepted).isTrue();
        assertThat(attributes).containsEntry("userId", "user-42");
    }

    @Test
    void rejectsMissingToken() {
        Map<String, Object> attributes = new HashMap<>();
        boolean accepted = interceptor.beforeHandshake(
                handshakeRequest(null), new ServletServerHttpResponse(new MockHttpServletResponse()),
                wsHandler, attributes);

        assertThat(accepted).isFalse();
    }

    @Test
    void rejectsInvalidToken() {
        when(jwtService.isTokenValid("expired-token")).thenReturn(false);

        Map<String, Object> attributes = new HashMap<>();
        boolean accepted = interceptor.beforeHandshake(
                handshakeRequest("token=expired-token"), new ServletServerHttpResponse(new MockHttpServletResponse()),
                wsHandler, attributes);

        assertThat(accepted).isFalse();
        assertThat(attributes).isEmpty();
    }

    private ServletServerHttpRequest handshakeRequest(String queryString) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        servletRequest.setScheme("http");
        servletRequest.setServerName("localhost");
        servletRequest.setQueryString(queryString);
        return new ServletServerHttpRequest(servletRequest);
    }
}
