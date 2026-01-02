package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.UpdateConfigurationRequest;
import com.pullwise.api.application.dto.response.ConfigurationDTO;
import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.domain.model.Configuration;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.ConfigurationRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    private final ConfigurationResolver configurationResolver;

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
}
