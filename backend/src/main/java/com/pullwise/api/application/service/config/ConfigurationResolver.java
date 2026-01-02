package com.pullwise.api.application.service.config;

import com.pullwise.api.domain.model.Configuration;
import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Serviço de resolução de configurações hierárquicas.
 * Escopo: ORGANIZATION > TEAM > PROJECT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationResolver {

    private final ConfigurationRepository configurationRepository;

    /**
     * Busca uma configuração com resolução hierárquica.
     * Busca na ordem: PROJECT -> ORGANIZATION -> default
     */
    @Cacheable(value = "configurations", key = "#projectId + ':' + #key")
    public String getConfig(Long projectId, String key) {
        return getConfig(projectId, null, key);
    }

    /**
     * Busca uma configuração com resolução hierárquica incluindo team.
     */
    public String getConfig(Long projectId, Long teamId, String key) {
        // 1. Buscar no nível do projeto
        if (projectId != null) {
            Optional<String> projectValue = getProjectConfig(projectId, key);
            if (projectValue.isPresent()) {
                return projectValue.get();
            }
        }

        // 2. Buscar no nível da organização (via projeto)
        if (projectId != null) {
            Optional<String> orgValue = getOrgConfigViaProject(projectId, key);
            if (orgValue.isPresent()) {
                return orgValue.get();
            }
        }

        // 3. Retornar valor padrão
        return getDefaultValue(key);
    }

    /**
     * Busca configuração no nível do projeto.
     */
    private Optional<String> getProjectConfig(Long projectId, String key) {
        return configurationRepository.findByProjectIdAndScopeAndKey(projectId, "PROJECT", key)
                .map(Configuration::getValue);
    }

    /**
     * Busca configuração no nível da organização.
     */
    private Optional<String> getOrgConfigViaProject(Long projectId, String key) {
        // Precisaria buscar a organização do projeto primeiro
        // Simplificação: buscar diretamente por organização
        return Optional.empty();
    }

    /**
     * Busca configuração no nível da organização.
     */
    public Optional<String> getOrgConfig(Long orgId, String key) {
        return configurationRepository.findByOrganizationIdAndScopeAndKey(orgId, "ORGANIZATION", key)
                .map(Configuration::getValue);
    }

    /**
     * Retorna o valor padrão para uma configuração.
     */
    private String getDefaultValue(String key) {
        return switch (key) {
            case "sast.enabled" -> "true";
            case "llm.enabled" -> "true";
            case "llm.provider" -> "openrouter";
            case "llm.model" -> "anthropic/claude-3-haiku";
            case "rag.enabled" -> "false";
            case "review.auto_post" -> "true";
            case "review.include_summary" -> "true";
            default -> null;
        };
    }

    /**
     * Verifica se SAST está habilitado para um projeto.
     */
    public boolean isSastEnabled(Long projectId) {
        return Boolean.parseBoolean(getConfig(projectId, "sast.enabled"));
    }

    /**
     * Verifica se LLM está habilitado para um projeto.
     */
    public boolean isLLMEnabled(Long projectId) {
        return Boolean.parseBoolean(getConfig(projectId, "llm.enabled"));
    }

    /**
     * Verifica se RAG está habilitado para um projeto.
     */
    public boolean isRAGEnabled(Long projectId) {
        return Boolean.parseBoolean(getConfig(projectId, "rag.enabled"));
    }

    /**
     * Retorna o provider LLM configurado.
     */
    public String getLLMProvider(Long projectId) {
        return getConfig(projectId, "llm.provider");
    }

    /**
     * Retorna o modelo LLM configurado.
     */
    public String getLLMModel(Long projectId) {
        return getConfig(projectId, "llm.model");
    }

    /**
     * Salva uma configuração no nível do projeto.
     */
    public Configuration saveProjectConfig(Project project, String key, String value) {
        return configurationRepository.save(
                Configuration.builder()
                        .project(project)
                        .organization(project.getOrganization())
                        .scope("PROJECT")
                        .key(key)
                        .value(value)
                        .valueType(inferValueType(value))
                        .build()
        );
    }

    /**
     * Salva uma configuração no nível da organização.
     */
    public Configuration saveOrgConfig(Organization organization, String key, String value) {
        return configurationRepository.save(
                Configuration.builder()
                        .organization(organization)
                        .scope("ORGANIZATION")
                        .key(key)
                        .value(value)
                        .valueType(inferValueType(value))
                        .build()
        );
    }

    /**
     * Salva múltiplas configurações.
     */
    public void saveProjectConfigs(Project project, List<ConfigValue> configs) {
        List<Configuration> configurations = configs.stream()
                .map(config -> Configuration.builder()
                        .project(project)
                        .organization(project.getOrganization())
                        .scope("PROJECT")
                        .key(config.key())
                        .value(config.value())
                        .valueType(inferValueType(config.value()))
                        .build())
                .toList();

        configurationRepository.saveAll(configurations);
    }

    /**
     * Infere o tipo do valor.
     */
    private String inferValueType(String value) {
        if (value == null) return "STRING";
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "BOOLEAN";
        }
        try {
            Integer.parseInt(value);
            return "NUMBER";
        } catch (NumberFormatException e) {
            // Not a number
        }
        if (value.startsWith("{") || value.startsWith("[")) {
            return "JSON";
        }
        return "STRING";
    }

    public record ConfigValue(String key, String value) {}
}
