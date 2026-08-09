package com.pullwise.api.infrastructure.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.SlashCommandService;
import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.application.service.config.RAGService;
import com.pullwise.api.domain.repository.OrganizationRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.PullRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de segurança do webhook do GitHub: secret obrigatório (fail-closed),
 * rejeição de assinatura inválida e aceite de assinatura válida.
 */
@ExtendWith(MockitoExtension.class)
class GitHubWebhookControllerTest {

    private static final String SECRET = "test-webhook-secret";

    @Mock
    private GitHubService gitHubService;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private PullRequestRepository pullRequestRepository;
    @Mock
    private ReviewOrchestrator reviewOrchestrator;
    @Mock
    private ConfigurationResolver configurationResolver;
    @Mock
    private SlashCommandService slashCommandService;
    @Mock
    private RAGService ragService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GitHubWebhookController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new GitHubWebhookController(
                gitHubService, organizationRepository, projectRepository, pullRequestRepository,
                reviewOrchestrator, configurationResolver, slashCommandService, ragService, objectMapper);
        setWebhookSecret(SECRET);
    }

    @Test
    void rejectsWebhookWhenSecretNotConfigured() throws Exception {
        setWebhookSecret("");

        ResponseEntity<Void> response = controller.handleWebhook("{}", "push", null, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void rejectsWebhookWithInvalidSignature() {
        ResponseEntity<Void> response = controller.handleWebhook("{}", "push", "sha256=invalid", new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void acceptsWebhookWithValidSignature() throws Exception {
        String payload = "{}";
        String signature = "sha256=" + hmac(payload);

        ResponseEntity<Void> response = controller.handleWebhook(payload, "unknown-event", signature, new MockHttpServletRequest());

        assertThat(response.getStatusCode().value()).isEqualTo(202);
    }

    private void setWebhookSecret(String secret) throws Exception {
        Field field = GitHubWebhookController.class.getDeclaredField("webhookSecret");
        field.setAccessible(true);
        field.set(controller, secret);
    }

    private String hmac(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
