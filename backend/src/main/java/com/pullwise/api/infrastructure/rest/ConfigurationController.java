package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.SaveIntegrationsRequest;
import com.pullwise.api.application.dto.request.UpdateConfigurationRequest;
import com.pullwise.api.application.dto.response.ConfigurationDTO;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.model.Configuration;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.User;
import com.pullwise.api.domain.repository.ConfigurationRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para gerenciamento de Configurações.
 */
@Slf4j
@RestController
@RequestMapping("/api/configurations")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationRepository configurationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ConfigurationResolver configurationResolver;

    /**
     * Salva configurações de integrações (SonarQube, OpenRouter, BitBucket).
     * Armazena como configurações na organização do usuário autenticado.
     */
    @PutMapping
    public ResponseEntity<Void> saveIntegrations(
            @Valid @RequestBody SaveIntegrationsRequest request,
            java.security.Principal principal) {

        Long userId = getUserIdFromPrincipal(principal);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Usar a primeira organização do usuário
        Organization org = user.getMemberships().stream()
                .map(com.pullwise.api.domain.model.OrganizationMember::getOrganization)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User has no organization"));

        if (request.sonarqubeUrl() != null) {
            configurationResolver.saveOrgConfig(org, "sonarqube.url", request.sonarqubeUrl());
        }
        if (request.sonarqubeToken() != null) {
            configurationResolver.saveOrgConfig(org, "sonarqube.token", request.sonarqubeToken());
        }
        if (request.openRouterKey() != null) {
            configurationResolver.saveOrgConfig(org, "llm.api_key", request.openRouterKey());
        }
        if (request.bitbucketToken() != null) {
            configurationResolver.saveOrgConfig(org, "bitbucket.token", request.bitbucketToken());
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Lista configurações de um projeto.
     */
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<ConfigurationDTO>> getProjectConfigurations(@PathVariable Long projectId) {
        List<Configuration> configs = configurationRepository.findByProjectId(projectId);
        List<ConfigurationDTO> dtos = configs.stream()
                .map(ConfigurationDTO::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Lista configurações de uma organização.
     */
    @GetMapping("/organizations/{orgId}")
    public ResponseEntity<List<ConfigurationDTO>> getOrganizationConfigurations(@PathVariable Long orgId) {
        List<Configuration> configs = configurationRepository.findByOrganizationId(orgId);
        List<ConfigurationDTO> dtos = configs.stream()
                .map(ConfigurationDTO::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Busca uma configuração específica de um projeto.
     */
    @GetMapping("/projects/{projectId}/{key}")
    public ResponseEntity<String> getProjectConfig(
            @PathVariable Long projectId,
            @PathVariable String key) {
        String value = configurationResolver.getConfig(projectId, key);
        return ResponseEntity.ok(value);
    }

    /**
     * Atualiza configurações de um projeto.
     */
    @PutMapping("/projects/{projectId}")
    public ResponseEntity<List<ConfigurationDTO>> updateProjectConfigurations(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateConfigurationRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        List<Configuration> configs = request.configurations().entrySet().stream()
                .map(entry -> configurationResolver.saveProjectConfig(
                        project,
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();

        List<ConfigurationDTO> dtos = configs.stream()
                .map(ConfigurationDTO::from)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Remove uma configuração.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable Long id) {
        if (!configurationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        configurationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retorna configurações padrão do sistema.
     */
    @GetMapping("/defaults")
    public ResponseEntity<java.util.Map<String, String>> getDefaultConfigurations() {
        java.util.Map<String, String> defaults = java.util.Map.of(
                "sast.enabled", "true",
                "llm.enabled", "true",
                "llm.provider", "openrouter",
                "llm.model", "anthropic/claude-3-haiku",
                "rag.enabled", "false",
                "review.auto_post", "true",
                "review.include_summary", "true"
        );
        return ResponseEntity.ok(defaults);
    }

    private Long getUserIdFromPrincipal(java.security.Principal principal) {
        if (principal == null) {
            throw new AuthenticationCredentialsNotFoundException(
                    "No authentication principal found");
        }
        String name = principal.getName();
        if (name != null) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException e) {
                log.debug("Principal name is not a numeric user ID: {}", name);
            }
        }
        throw new AuthenticationCredentialsNotFoundException(
                "Unable to extract user ID from authentication principal");
    }
}
