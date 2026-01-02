package com.pullwise.api.application.service.integration;

import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço de integração com Linear.
 *
 * <p>Funcionalidades:
 * <ul>
 *   <li>Criação de issues a partir de issues de código</li>
 *   <li>Atualização de status</li>
 *   <li>Comentários em issues</li>
 *   <li>Busca de issues relacionadas</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinearService {

    @Value("${integrations.linear.api-url:https://api.linear.app/graphql}")
    private String linearApiUrl;

    @Value("${integrations.linear.api-key:}")
    private String linearApiKey;

    @Value("${integrations.linear.default-team-id:}")
    private String defaultTeamId;

    @Value("${integrations.linear.default-status:Backlog}")
    private String defaultStatus;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Cria uma issue no Linear a partir de uma issue de código.
     *
     * @param issue Issue de código
     * @param teamId ID do time Linear
     * @return ID da issue criada
     */
    public String createIssue(Issue issue, String teamId) {
        if (linearApiKey == null || linearApiKey.isBlank()) {
            log.warn("Linear integration not configured");
            return null;
        }

        String team = teamId != null ? teamId : defaultTeamId;
        if (team == null) {
            log.warn("No Linear team ID configured");
            return null;
        }

        try {
            String query = """
                    mutation CreateIssue($input: IssueCreateInput!) {
                        issueCreate(input: $input) {
                            success
                            issue {
                                id
                                identifier
                                title
                                url
                            }
                        }
                    }
                    """;

            Map<String, Object> variables = Map.of(
                    "input", buildCreateInput(issue, team)
            );

            GraphQLRequest request = new GraphQLRequest(query, variables);

            HttpHeaders headers = createHeaders();
            HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<GraphQLResponse> response = restTemplate.postForEntity(
                    linearApiUrl,
                    entity,
                    GraphQLResponse.class
            );

            if (response.getBody() != null &&
                response.getBody().data() != null &&
                response.getBody().data().issueCreate() != null) {

                IssueCreateResult result = response.getBody().data().issueCreate();
                if (result.success() && result.issue() != null) {
                    log.info("Created Linear issue {} for code issue {}",
                            result.issue().identifier(), issue.getId());

                    return result.issue().id();
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to create Linear issue for code issue {}", issue.getId(), e);
            return null;
        }
    }

    /**
     * Cria issues para todas as issues críticas de um review.
     *
     * @param review Review com issues
     * @param teamId ID do time Linear
     * @return Mapa de issue ID para Linear issue ID
     */
    public Map<Long, String> createIssuesForCritical(Review review, String teamId) {
        Map<Long, String> tickets = new HashMap<>();

        for (Issue issue : review.getIssues()) {
            if (issue.getSeverity().ordinal() >= 2 && !issue.getIsFalsePositive()) {
                String linearIssueId = createIssue(issue, teamId);
                if (linearIssueId != null) {
                    tickets.put(issue.getId(), linearIssueId);
                }
            }
        }

        log.info("Created {} Linear issues for review {}", tickets.size(), review.getId());

        return tickets;
    }

    /**
     * Adiciona comentário a uma issue.
     *
     * @param issueId ID da issue Linear
     * @param comment Comentário
     * @return true se adicionado com sucesso
     */
    public boolean addComment(String issueId, String comment) {
        if (linearApiKey == null || linearApiKey.isBlank()) {
            return false;
        }

        try {
            String query = """
                    mutation CreateComment($input: CommentCreateInput!) {
                        commentCreate(input: $input) {
                            success
                            comment {
                                id
                            }
                        }
                    }
                    """;

            Map<String, Object> input = Map.of(
                    "issueId", issueId,
                    "body", comment
            );

            Map<String, Object> variables = Map.of("input", input);

            GraphQLRequest request = new GraphQLRequest(query, variables);

            HttpHeaders headers = createHeaders();
            HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(linearApiUrl, entity, GraphQLResponse.class);

            log.debug("Added comment to Linear issue {}", issueId);

            return true;

        } catch (Exception e) {
            log.error("Failed to add comment to Linear issue {}", issueId, e);
            return false;
        }
    }

    /**
     * Atualiza status de uma issue.
     *
     * @param issueId ID da issue Linear
     * @param statusId ID do novo status
     * @return true se atualizado com sucesso
     */
    public boolean updateStatus(String issueId, String statusId) {
        if (linearApiKey == null || linearApiKey.isBlank()) {
            return false;
        }

        try {
            String query = """
                    mutation UpdateIssue($id: String!, $input: IssueUpdateInput!) {
                        issueUpdate(id: $id, input: $input) {
                            success
                            issue {
                                id
                                state {
                                    id
                                    name
                                }
                            }
                        }
                    }
                    """;

            Map<String, Object> input = Map.of("stateId", statusId);
            Map<String, Object> variables = Map.of(
                    "id", issueId,
                    "input", input
            );

            GraphQLRequest request = new GraphQLRequest(query, variables);

            HttpHeaders headers = createHeaders();
            HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(linearApiUrl, entity, GraphQLResponse.class);

            log.info("Updated Linear issue {} to status {}", issueId, statusId);

            return true;

        } catch (Exception e) {
            log.error("Failed to update status of Linear issue {}", issueId, e);
            return false;
        }
    }

    /**
     * Busca informações de uma issue.
     *
     * @param issueId ID da issue Linear
     * @return Informações da issue ou null
     */
    public LinearIssue getIssue(String issueId) {
        if (linearApiKey == null || linearApiKey.isBlank()) {
            return null;
        }

        try {
            String query = """
                    query GetIssue($id: String!) {
                        issue(id: $id) {
                            id
                            identifier
                            title
                            description
                            state {
                                id
                                name
                            }
                            priority
                            url
                        }
                    }
                    """;

            Map<String, Object> variables = Map.of("id", issueId);

            GraphQLRequest request = new GraphQLRequest(query, variables);

            HttpHeaders headers = createHeaders();
            HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<GraphQLGetResponse> response = restTemplate.postForEntity(
                    linearApiUrl,
                    entity,
                    GraphQLGetResponse.class
            );

            if (response.getBody() != null && response.getBody().data() != null) {
                return response.getBody().data().issue();
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to get Linear issue {}", issueId, e);
            return null;
        }
    }

    /**
     * Busca estados disponíveis para um time.
     *
     * @param teamId ID do time
     * @return Lista de estados
     */
    public List<LinearState> getStates(String teamId) {
        if (linearApiKey == null || linearApiKey.isBlank()) {
            return List.of();
        }

        try {
            String query = """
                    query GetStates($teamId: String!) {
                        team(id: $teamId) {
                            states {
                                nodes {
                                    id
                                    name
                                    type
                                    color
                                }
                            }
                        }
                    }
                    """;

            Map<String, Object> variables = Map.of("teamId", teamId);

            GraphQLRequest request = new GraphQLRequest(query, variables);

            HttpHeaders headers = createHeaders();
            HttpEntity<GraphQLRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<GraphQLStatesResponse> response = restTemplate.postForEntity(
                    linearApiUrl,
                    entity,
                    GraphQLStatesResponse.class
            );

            if (response.getBody() != null &&
                response.getBody().data() != null &&
                response.getBody().data().team() != null) {

                return response.getBody().data().team().states().nodes();
            }

            return List.of();

        } catch (Exception e) {
            log.error("Failed to get states for team {}", teamId, e);
            return List.of();
        }
    }

    /**
     * Verifica se a integração está configurada.
     */
    public boolean isConfigured() {
        return linearApiKey != null && !linearApiKey.isBlank();
    }

    // ========== Private Methods ==========

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + linearApiKey);

        return headers;
    }

    private Map<String, Object> buildCreateInput(Issue issue, String teamId) {
        Map<String, Object> input = new HashMap<>();
        input.put("teamId", teamId);
        input.put("title", issue.getTitle());

        // Descrição
        String description = buildDescription(issue);
        input.put("description", description);

        // Priority baseado na severidade
        String priority = switch (issue.getSeverity()) {
            case CRITICAL -> "urgent";
            case HIGH -> "high";
            case MEDIUM -> "medium";
            case LOW -> "low";
            default -> "no priority";
        };
        input.put("priority", priority);

        // Labels
        List<String> labels = new ArrayList<>();
        labels.add("pullwise");
        labels.add(issue.getType().name().toLowerCase());
        labels.add(issue.getSeverity().name().toLowerCase());
        input.put("labels", labels);

        return input;
    }

    private String buildDescription(Issue issue) {
        StringBuilder sb = new StringBuilder();

        sb.append("**Issue detected by Pullwise.ai**\n\n");

        if (issue.getDescription() != null) {
            sb.append(issue.getDescription()).append("\n\n");
        }

        if (issue.getFilePath() != null) {
            sb.append("**File:** ").append(issue.getFilePath());
            if (issue.getLineStart() != null) {
                sb.append(":").append(issue.getLineStart());
            }
            sb.append("\n");
        }

        if (issue.getRuleId() != null) {
            sb.append("**Rule:** `").append(issue.getRuleId()).append("`\n");
        }

        if (issue.getCodeSnippet() != null) {
            sb.append("**Code:**\n```");
            sb.append(issue.getCodeSnippet());
            sb.append("```\n");
        }

        sb.append("\n**Severity:** ").append(issue.getSeverity()).append("\n");
        sb.append("**Type:** ").append(issue.getType()).append("\n");
        sb.append("**Detected at:** ").append(LocalDateTime.now()).append("\n");

        return sb.toString();
    }

    // ========== DTOs ==========

    record GraphQLRequest(
            String query,
            Map<String, Object> variables
    ) {}

    record GraphQLResponse(GraphQLData data) {}

    record GraphQLData(IssueCreateResult issueCreate) {}

    record IssueCreateResult(boolean success, LinearIssue issue) {}

    record GraphQLGetResponse(GraphQLDataGet data) {}

    record GraphQLDataGet(LinearIssue issue) {}

    record GraphQLStatesResponse(GraphQLDataStates data) {}

    record GraphQLDataStates(LinearTeamData team) {}

    record LinearTeamData(LinearStates states) {}

    record LinearStates(List<LinearState> nodes) {}

    public record LinearIssue(
            String id,
            String identifier,
            String title,
            String description,
            LinearState state,
            String priority,
            String url
    ) {}

    public record LinearState(
            String id,
            String name,
            String type,
            String color
    ) {}
}
