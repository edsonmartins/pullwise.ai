package com.pullwise.api.application.service.config;

import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.domain.converter.EncryptedStringConverter;
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
import java.util.Set;

/**
 * Serviço de resolução de configurações hierárquicas.
 * Escopo: ORGANIZATION > TEAM > PROJECT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationResolver {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            ConfigKeys.SONARQUBE_TOKEN, ConfigKeys.LLM_API_KEY, ConfigKeys.BITBUCKET_TOKEN,
            ConfigKeys.GITHUB_TOKEN, ConfigKeys.GITLAB_TOKEN, ConfigKeys.OPENROUTER_API_KEY
    );

    private final ConfigurationRepository configurationRepository;
    private final EncryptedStringConverter encryptedStringConverter = new EncryptedStringConverter();

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
                .map(config -> decryptIfSensitive(config));
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
                .map(config -> decryptIfSensitive(config));
    }

    private String decryptIfSensitive(Configuration config) {
        if (Boolean.TRUE.equals(config.getIsSensitive())) {
            return encryptedStringConverter.convertToEntityAttribute(config.getValue());
        }
        return config.getValue();
    }

    /**
     * Retorna o valor padrão para uma configuração.
     */
    private String getDefaultValue(String key) {
        return switch (key) {
            case ConfigKeys.SAST_ENABLED -> "true";
            case ConfigKeys.LLM_ENABLED -> "true";
            case ConfigKeys.LLM_PROVIDER -> "openrouter";
            case ConfigKeys.LLM_MODEL -> "anthropic/claude-3-haiku";
            case ConfigKeys.RAG_ENABLED -> "false";
            case ConfigKeys.REVIEW_AUTO_POST -> "true";
            case ConfigKeys.REVIEW_INCLUDE_SUMMARY -> "true";
            default -> null;
        };
    }

    /**
     * Verifica se SAST está habilitado para um projeto.
     */
    public boolean isSastEnabled(Long projectId) {
        return Boolean.parseBoolean(getConfig(projectId, ConfigKeys.SAST_ENABLED));
    }

    /**
     * Verifica se LLM está habilitado para um projeto.
     */
    public boolean isLLMEnabled(Long projectId) {
        return Boolean.parseBoolean(getConfig(projectId, ConfigKeys.LLM_ENABLED));
    }

    /**
     * Verifica se RAG está habilitado para um projeto.
     */
    public boolean isRAGEnabled(Long projectId) {
        return Boolean.parseBoolean(getConfig(projectId, ConfigKeys.RAG_ENABLED));
    }

    /**
     * Retorna o provider LLM configurado.
     */
    public String getLLMProvider(Long projectId) {
        return getConfig(projectId, ConfigKeys.LLM_PROVIDER);
    }

    /**
     * Retorna o modelo LLM configurado.
     */
    public String getLLMModel(Long projectId) {
        return getConfig(projectId, ConfigKeys.LLM_MODEL);
    }

    /**
     * Salva uma configuração no nível do projeto.
     */
    public Configuration saveProjectConfig(Project project, String key, String value) {
        boolean sensitive = SENSITIVE_KEYS.contains(key);
        String storedValue = sensitive ? encryptedStringConverter.convertToDatabaseColumn(value) : value;

        return configurationRepository.save(
                Configuration.builder()
                        .project(project)
                        .organization(project.getOrganization())
                        .scope("PROJECT")
                        .key(key)
                        .value(storedValue)
                        .valueType(inferValueType(value))
                        .isSensitive(sensitive)
                        .build()
        );
    }

    /**
     * Salva uma configuração no nível da organização.
     */
    public Configuration saveOrgConfig(Organization organization, String key, String value) {
        boolean sensitive = SENSITIVE_KEYS.contains(key);
        String storedValue = sensitive ? encryptedStringConverter.convertToDatabaseColumn(value) : value;

        return configurationRepository.save(
                Configuration.builder()
                        .organization(organization)
                        .scope("ORGANIZATION")
                        .key(key)
                        .value(storedValue)
                        .valueType(inferValueType(value))
                        .isSensitive(sensitive)
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
