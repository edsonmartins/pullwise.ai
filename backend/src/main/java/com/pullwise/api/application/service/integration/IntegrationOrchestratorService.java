package com.pullwise.api.application.service.integration;

import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço orquestrador de integrações com sistemas externos (Jira, Linear).
 *
 * <p>Gerencia automaticamente a criação de tickets para issues críticas
 * detectadas durante o code review.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationOrchestratorService {

    private final JiraService jiraService;
    private final LinearService linearService;

    @Value("${integrations.auto-create-tickets:false}")
    private boolean autoCreateTickets;

    @Value("${integrations.jira.project-key:}")
    private String jiraProjectKey;

    @Value("${integrations.linear.team-id:}")
    private String linearTeamId;

    /**
     * Processa um review e cria tickets para issues críticas.
     *
     * @param review Review processado
     * @return Resultado da integração
     */
    public IntegrationResult processReview(Review review) {
        if (!autoCreateTickets) {
            log.debug("Auto-creation of tickets is disabled");
            return IntegrationResult.skipped("Auto-creation disabled");
        }

        IntegrationResult result = new IntegrationResult();

        // Processa Jira
        if (jiraService.isConfigured()) {
            try {
                Map<Long, String> jiraTickets = jiraService.createTicketsForCriticalIssues(
                        review,
                        jiraProjectKey
                );

                result.setJiraTickets(jiraTickets);
                log.info("Created {} Jira tickets for review {}", jiraTickets.size(), review.getId());

            } catch (Exception e) {
                log.error("Failed to create Jira tickets for review {}", review.getId(), e);
                result.setJiraError(e.getMessage());
            }
        }

        // Processa Linear
        if (linearService.isConfigured()) {
            try {
                Map<Long, String> linearIssues = linearService.createIssuesForCritical(
                        review,
                        linearTeamId
                );

                result.setLinearIssues(linearIssues);
                log.info("Created {} Linear issues for review {}", linearIssues.size(), review.getId());

            } catch (Exception e) {
                log.error("Failed to create Linear issues for review {}", review.getId(), e);
                result.setLinearError(e.getMessage());
            }
        }

        result.setProcessed(true);

        return result;
    }

    /**
     * Cria um ticket Jira para uma issue específica.
     *
     * @param issue Issue de código
     * @return ID do ticket criado ou null
     */
    public String createJiraTicket(Issue issue) {
        if (!jiraService.isConfigured()) {
            log.warn("Jira integration not configured");
            return null;
        }

        return jiraService.createTicket(issue, jiraProjectKey);
    }

    /**
     * Cria uma issue Linear para uma issue específica.
     *
     * @param issue Issue de código
     * @return ID da issue criada ou null
     */
    public String createLinearIssue(Issue issue) {
        if (!linearService.isConfigured()) {
            log.warn("Linear integration not configured");
            return null;
        }

        return linearService.createIssue(issue, linearTeamId);
    }

    /**
     * Adiciona comentário em ambos os sistemas para uma issue.
     *
     * @param jiraTicketId ID do ticket Jira (opcional)
     * @param linearIssueId ID da issue Linear (opcional)
     * @param comment Comentário
     */
    public void addCommentToAll(String jiraTicketId, String linearIssueId, String comment) {
        if (jiraTicketId != null && jiraService.isConfigured()) {
            jiraService.addComment(jiraTicketId, comment);
        }

        if (linearIssueId != null && linearService.isConfigured()) {
            linearService.addComment(linearIssueId, comment);
        }
    }

    /**
     * Atualiza status em ambos os sistemas.
     *
     * @param jiraTicketId ID do ticket Jira (opcional)
     * @param linearIssueId ID da issue Linear (opcional)
     * @param status Novo status
     */
    public void updateStatusAll(String jiraTicketId, String linearIssueId, String status) {
        if (jiraTicketId != null && jiraService.isConfigured()) {
            jiraService.updateStatus(jiraTicketId, status);
        }

        if (linearIssueId != null && linearService.isConfigured()) {
            // Linear usa ID de estado, não nome
            // Precisaria buscar o ID correspondente primeiro
            linearService.updateStatus(linearIssueId, status);
        }
    }

    /**
     * Verifica se pelo menos uma integração está ativa.
     */
    public boolean hasAnyIntegration() {
        return jiraService.isConfigured() || linearService.isConfigured();
    }

    /**
     * Retorna status das integrações.
     */
    public IntegrationStatus getStatus() {
        return new IntegrationStatus(
                jiraService.isConfigured(),
                linearService.isConfigured(),
                autoCreateTickets,
                jiraProjectKey,
                linearTeamId
        );
    }

    // ========== DTOs ==========

    /**
     * Resultado da integração.
     */
    public static class IntegrationResult {
        private boolean processed;
        private Map<Long, String> jiraTickets = new HashMap<>();
        private Map<Long, String> linearIssues = new HashMap<>();
        private String jiraError;
        private String linearError;
        private String skipReason;

        public static IntegrationResult skipped(String reason) {
            IntegrationResult result = new IntegrationResult();
            result.skipReason = reason;
            return result;
        }

        public boolean isProcessed() { return processed; }
        public void setProcessed(boolean processed) { this.processed = processed; }

        public Map<Long, String> getJiraTickets() { return jiraTickets; }
        public void setJiraTickets(Map<Long, String> tickets) { this.jiraTickets = tickets; }

        public Map<Long, String> getLinearIssues() { return linearIssues; }
        public void setLinearIssues(Map<Long, String> issues) { this.linearIssues = issues; }

        public String getJiraError() { return jiraError; }
        public void setJiraError(String error) { this.jiraError = error; }

        public String getLinearError() { return linearError; }
        public void setLinearError(String error) { this.linearError = error; }

        public String getSkipReason() { return skipReason; }
        public void setSkipReason(String reason) { this.skipReason = reason; }

        public boolean isSuccess() {
            return (jiraTickets.isEmpty() || jiraError == null) &&
                   (linearIssues.isEmpty() || linearError == null);
        }

        public int totalTicketsCreated() {
            return jiraTickets.size() + linearIssues.size();
        }
    }

    /**
     * Status das integrações.
     */
    public record IntegrationStatus(
            boolean jiraEnabled,
            boolean linearEnabled,
            boolean autoCreateTickets,
            String jiraProjectKey,
            String linearTeamId
    ) {}
}
