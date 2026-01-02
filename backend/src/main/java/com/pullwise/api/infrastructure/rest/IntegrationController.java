package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.service.integration.IntegrationOrchestratorService;
import com.pullwise.api.application.service.integration.JiraService;
import com.pullwise.api.application.service.integration.LinearService;
import com.pullwise.api.application.service.integration.JiraService.JiraTicket;
import com.pullwise.api.application.service.integration.LinearService.LinearIssue;
import com.pullwise.api.application.service.integration.LinearService.LinearState;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para integrações externas (Jira, Linear).
 */
@Slf4j
@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final JiraService jiraService;
    private final LinearService linearService;
    private final IntegrationOrchestratorService orchestratorService;

    /**
     * Retorna status das integrações.
     */
    @GetMapping("/status")
    public ResponseEntity<IntegrationOrchestratorService.IntegrationStatus> getStatus() {
        return ResponseEntity.ok(orchestratorService.getStatus());
    }

    // ========== Jira ==========

    /**
     * Busca um ticket Jira.
     */
    @GetMapping("/jira/{ticketKey}")
    public ResponseEntity<JiraTicket> getJiraTicket(@PathVariable String ticketKey) {
        JiraTicket ticket = jiraService.getTicket(ticketKey);

        if (ticket == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(ticket);
    }

    /**
     * Adiciona comentário em ticket Jira.
     */
    @PostMapping("/jira/{ticketKey}/comment")
    public ResponseEntity<Void> addJiraComment(
            @PathVariable String ticketKey,
            @RequestBody CommentRequest request
    ) {
        boolean success = jiraService.addComment(ticketKey, request.comment());

        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    /**
     * Atualiza status de ticket Jira.
     */
    @PutMapping("/jira/{ticketKey}/status")
    public ResponseEntity<Void> updateJiraStatus(
            @PathVariable String ticketKey,
            @RequestBody StatusUpdateRequest request
    ) {
        boolean success = jiraService.updateStatus(ticketKey, request.status());

        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    // ========== Linear ==========

    /**
     * Busca uma issue Linear.
     */
    @GetMapping("/linear/{issueId}")
    public ResponseEntity<LinearIssue> getLinearIssue(@PathVariable String issueId) {
        LinearIssue issue = linearService.getIssue(issueId);

        if (issue == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(issue);
    }

    /**
     * Busca estados disponíveis no Linear.
     */
    @GetMapping("/linear/states")
    public ResponseEntity<List<LinearState>> getLinearStates(
            @RequestParam String teamId
    ) {
        List<LinearState> states = linearService.getStates(teamId);
        return ResponseEntity.ok(states);
    }

    /**
     * Adiciona comentário em issue Linear.
     */
    @PostMapping("/linear/{issueId}/comment")
    public ResponseEntity<Void> addLinearComment(
            @PathVariable String issueId,
            @RequestBody CommentRequest request
    ) {
        boolean success = linearService.addComment(issueId, request.comment());

        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    /**
     * Atualiza status de issue Linear.
     */
    @PutMapping("/linear/{issueId}/status")
    public ResponseEntity<Void> updateLinearStatus(
            @PathVariable String issueId,
            @RequestBody StatusUpdateRequest request
    ) {
        boolean success = linearService.updateStatus(issueId, request.status());

        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    // ========== DTOs ==========

    public record CommentRequest(String comment) {}

    public record StatusUpdateRequest(String status) {}
}
