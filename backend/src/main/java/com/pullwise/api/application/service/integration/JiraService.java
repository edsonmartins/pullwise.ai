package com.pullwise.api.application.service.integration;

import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço de integração com Jira.
 *
 * <p>Funcionalidades:
 * <ul>
 *   <li>Criação de tickets a partir de issues críticas</li>
 *   <li>Atualização de status de tickets</li>
 *   <li>Comentários em tickets</li>
 *   <li>Busca de tickets relacionados</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraService {

    @Value("${integrations.jira.api-url:https://api.atlassian.com/ex/jira}")
    private String jiraApiUrl;

    @Value("${integrations.jira.email:}")
    private String jiraEmail;

    @Value("${integrations.jira.api-token:}")
    private String jiraApiToken;

    @Value("${integrations.jira.project-key:}")
    private String defaultProjectKey;

    @Value("${integrations.jira.default-issue-type:Bug}")
    private String defaultIssueType;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Cria um ticket no Jira a partir de uma issue de código.
     *
     * @param issue Issue de código
     * @param projectKey Chave do projeto Jira
     * @return ID do ticket criado
     */
    public String createTicket(Issue issue, String projectKey) {
        if (jiraEmail == null || jiraEmail.isBlank()) {
            log.warn("Jira integration not configured");
            return null;
        }

        String project = projectKey != null ? projectKey : defaultProjectKey;
        if (project == null) {
            log.warn("No Jira project key configured");
            return null;
        }

        try {
            String url = jiraApiUrl + "/rest/api/3/issue";

            HttpHeaders headers = createHeaders();
            JiraCreateRequest request = buildCreateRequest(issue, project);

            HttpEntity<JiraCreateRequest> entity = new HttpEntity<>(request, headers);

            JiraCreateResponse response = restTemplate.postForObject(
                    url,
                    entity,
                    JiraCreateResponse.class
            );

            log.info("Created Jira ticket {} for code issue {}", response.key(), issue.getId());

            return response.key();

        } catch (Exception e) {
            log.error("Failed to create Jira ticket for issue {}", issue.getId(), e);
            return null;
        }
    }

    /**
     * Cria tickets para todas as issues críticas de um review.
     *
     * @param review Review com issues
     * @param projectKey Chave do projeto Jira
     * @return Mapa de issue ID para ticket Jira key
     */
    public Map<Long, String> createTicketsForCriticalIssues(Review review, String projectKey) {
        Map<Long, String> tickets = new HashMap<>();

        for (Issue issue : review.getIssues()) {
            if (issue.getSeverity().ordinal() >= 2 && !issue.getIsFalsePositive()) {
                String ticketKey = createTicket(issue, projectKey);
                if (ticketKey != null) {
                    tickets.put(issue.getId(), ticketKey);
                }
            }
        }

        log.info("Created {} Jira tickets for review {}", tickets.size(), review.getId());

        return tickets;
    }

    /**
     * Adiciona comentário a um ticket.
     *
     * @param ticketKey Chave do ticket
     * @param comment Comentário
     * @return true se adicionado com sucesso
     */
    public boolean addComment(String ticketKey, String comment) {
        if (jiraEmail == null || jiraEmail.isBlank()) {
            return false;
        }

        try {
            String url = jiraApiUrl + "/rest/api/3/issue/" + ticketKey + "/comment";

            HttpHeaders headers = createHeaders();
            Map<String, Object> body = Map.of("body", Map.of(
                    "type", "doc",
                    "version", 1,
                    "content", List.of(Map.of(
                            "type", "paragraph",
                            "content", List.of(Map.of(
                                    "type", "text",
                                    "text", comment
                            ))
                    ))
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.postForObject(url, entity, String.class);

            log.debug("Added comment to Jira ticket {}", ticketKey);

            return true;

        } catch (Exception e) {
            log.error("Failed to add comment to Jira ticket {}", ticketKey, e);
            return false;
        }
    }

    /**
     * Atualiza status de um ticket.
     *
     * @param ticketKey Chave do ticket
     * @param status Novo status
     * @return true se atualizado com sucesso
     */
    public boolean updateStatus(String ticketKey, String status) {
        if (jiraEmail == null || jiraEmail.isBlank()) {
            return false;
        }

        try {
            // Primeiro precisa buscar a transição correta para o status
            String url = jiraApiUrl + "/rest/api/3/issue/" + ticketKey + "/transitions";

            HttpHeaders headers = createHeaders();

            // Busca transições disponíveis
            HttpEntity<Void> getEntity = new HttpEntity<>(headers);
            TransitionsResponse transitions = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    getEntity,
                    TransitionsResponse.class
            ).getBody();

            // Encontra a transição para o status desejado
            String transitionId = null;
            if (transitions != null && transitions.transitions() != null) {
                for (Transition t : transitions.transitions()) {
                    if (t.to().name().equalsIgnoreCase(status)) {
                        transitionId = t.id();
                        break;
                    }
                }
            }

            if (transitionId == null) {
                log.warn("No transition found for status {} on ticket {}", status, ticketKey);
                return false;
            }

            // Executa a transição
            Map<String, Object> body = Map.of(
                    "transition", Map.of("id", transitionId)
            );

            HttpEntity<Map<String, Object>> postEntity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(url, postEntity, String.class);

            log.info("Updated Jira ticket {} to status {}", ticketKey, status);

            return true;

        } catch (Exception e) {
            log.error("Failed to update status of Jira ticket {}", ticketKey, e);
            return false;
        }
    }

    /**
     * Busca informações de um ticket.
     *
     * @param ticketKey Chave do ticket
     * @return Informações do ticket ou null
     */
    public JiraTicket getTicket(String ticketKey) {
        if (jiraEmail == null || jiraEmail.isBlank()) {
            return null;
        }

        try {
            String url = jiraApiUrl + "/rest/api/3/issue/" + ticketKey;

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JiraTicket.class
            ).getBody();

        } catch (Exception e) {
            log.error("Failed to get Jira ticket {}", ticketKey, e);
            return null;
        }
    }

    /**
     * Verifica se a integração está configurada.
     */
    public boolean isConfigured() {
        return jiraEmail != null && !jiraEmail.isBlank() &&
                jiraApiToken != null && !jiraApiToken.isBlank();
    }

    // ========== Private Methods ==========

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");

        // Basic auth com email + API token
        String auth = jiraEmail + ":" + jiraApiToken;
        String encodedAuth = java.util.Base64.getEncoder()
                .encodeToString(auth.getBytes());

        headers.set("Authorization", "Basic " + encodedAuth);

        return headers;
    }

    private JiraCreateRequest buildCreateRequest(Issue issue, String projectKey) {
        // Determina o tipo de issue baseado na severidade
        String issueType = switch (issue.getSeverity()) {
            case CRITICAL -> "Bug";
            case HIGH -> "Bug";
            default -> defaultIssueType;
        };

        // Constrói a descrição
        String description = buildDescription(issue);

        // Constrói campos customizados
        Map<String, Object> fields = new HashMap<>();
        fields.put("project", Map.of("key", projectKey));
        fields.put("summary", issue.getTitle());
        fields.put("description", description);
        fields.put("issuetype", Map.of("name", issueType));

        // Priority baseado na severidade
        String priority = switch (issue.getSeverity()) {
            case CRITICAL -> "Highest";
            case HIGH -> "High";
            case MEDIUM -> "Medium";
            case LOW -> "Low";
            default -> "Low";
        };
        fields.put("priority", Map.of("name", priority));

        return new JiraCreateRequest(fields);
    }

    private String buildDescription(Issue issue) {
        StringBuilder sb = new StringBuilder();

        sb.append("*Issue detected by Pullwise.ai*\\n\\n");

        if (issue.getDescription() != null) {
            sb.append(issue.getDescription()).append("\\n\\n");
        }

        if (issue.getFilePath() != null) {
            sb.append("*File:* ").append(issue.getFilePath());
            if (issue.getLineStart() != null) {
                sb.append(":").append(issue.getLineStart());
            }
            sb.append("\\n");
        }

        if (issue.getRuleId() != null) {
            sb.append("*Rule:* ").append(issue.getRuleId()).append("\\n");
        }

        if (issue.getCodeSnippet() != null) {
            sb.append("*Code:*\\n{code:java}").append(issue.getCodeSnippet())
                    .append("{code}\\n");
        }

        sb.append("\\n*Severity:* ").append(issue.getSeverity()).append("\\n");
        sb.append("*Type:* ").append(issue.getType()).append("\\n");
        sb.append("*Detected at:* ").append(LocalDateTime.now()).append("\\n");

        return sb.toString();
    }

    // ========== DTOs ==========

    record JiraCreateRequest(Map<String, Object> fields) {}

    record JiraCreateResponse(String key, String self) {}

    record TransitionsResponse(List<Transition> transitions) {}

    record Transition(String id, TransitionTo to) {}

    record TransitionTo(String name) {}

    public record JiraTicket(
            String key,
            String self,
            JiraFields fields
    ) {}

    public record JiraFields(
            String summary,
            JiraStatus status,
            JiraPriority priority,
            JiraIssueType issuetype
    ) {}

    public record JiraStatus(String name) {}

    public record JiraPriority(String name) {}

    public record JiraIssueType(String name) {}
}
