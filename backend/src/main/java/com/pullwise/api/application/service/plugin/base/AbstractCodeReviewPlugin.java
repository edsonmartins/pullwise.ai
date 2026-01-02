package com.pullwise.api.application.service.plugin.base;

import com.pullwise.api.application.service.plugin.api.AnalysisRequest;
import com.pullwise.api.application.service.plugin.api.AnalysisResult;
import com.pullwise.api.application.service.plugin.api.CodeReviewPlugin;
import com.pullwise.api.application.service.plugin.api.PluginContext;
import com.pullwise.api.application.service.plugin.api.PluginException;
import com.pullwise.api.application.service.plugin.api.PluginLanguage;
import com.pullwise.api.application.service.plugin.api.PluginMetadata;
import com.pullwise.api.application.service.plugin.api.PluginType;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Classe base abstrata para facilitar a criação de plugins.
 *
 * <p>Fornece implementações padrão para métodos comuns
 * e gerencia o contexto do plugin.
 */
@Slf4j
public abstract class AbstractCodeReviewPlugin implements CodeReviewPlugin {

    protected PluginContext context;
    protected Map<String, Object> config;

    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        this.config = context.getConfiguration() != null
                ? context.getConfiguration()
                : new HashMap<>();

        log.info("Initializing plugin: {}", getName());
        doInitialize();
        log.info("Plugin {} initialized successfully", getName());
    }

    /**
     * Implementação específica de inicialização do plugin.
     * Sobrescreva este método para customizar a inicialização.
     */
    protected void doInitialize() throws PluginException {
        // Default: nothing to do
    }

    @Override
    public AnalysisResult analyze(AnalysisRequest request) throws PluginException {
        if (!isEnabled()) {
            return AnalysisResult.empty(getId());
        }

        long startTime = System.currentTimeMillis();

        try {
            AnalysisResult result = doAnalyze(request);
            result.setPluginId(getId());

            long duration = System.currentTimeMillis() - startTime;
            result.setExecutionTime(java.time.Duration.ofMillis(duration));

            return result;

        } catch (Exception e) {
            log.error("Plugin {} analysis failed", getName(), e);
            return AnalysisResult.error(getId(), e.getMessage());
        }
    }

    /**
     * Implementação específica da análise do plugin.
     * Sobrescreva este método com a lógica de análise.
     */
    protected abstract AnalysisResult doAnalyze(AnalysisRequest request) throws PluginException;

    @Override
    public void shutdown() {
        log.info("Shutting down plugin: {}", getName());
        try {
            doShutdown();
        } catch (Exception e) {
            log.error("Error during plugin shutdown", e);
        }
    }

    /**
     * Implementação específica de cleanup do plugin.
     * Sobrescreva este método para liberar recursos.
     */
    protected void doShutdown() {
        // Default: nothing to do
    }

    // ========== Helpers ==========

    /**
     * Retorna uma configuração do plugin ou valor padrão.
     */
    protected String getConfigString(String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Retorna uma configuração inteira do plugin ou valor padrão.
     */
    protected int getConfigInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Retorna uma configuração booleana do plugin ou valor padrão.
     */
    protected boolean getConfigBoolean(String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }

    /**
     * Retorna o diretório de dados do plugin.
     */
    protected Path getDataDirectory() {
        return context != null ? context.getDataDirectory() : null;
    }

    /**
     * Retorna o diretório de trabalho do plugin.
     */
    protected Path getWorkingDirectory() {
        return context != null ? context.getWorkingDirectory() : null;
    }

    /**
     * Verifica se o plugin está em modo de desenvolvimento.
     */
    protected boolean isDevelopmentMode() {
        return context != null && context.isDevelopmentMode();
    }

    // ========== Defaults ==========

    @Override
    public PluginMetadata getMetadata() {
        return PluginMetadata.builder()
                .id(getId())
                .name(getName())
                .version(getVersion())
                .author(getAuthor())
                .description(getDescription())
                .type(getType())
                .supportedLanguages(getSupportedLanguages())
                .build();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String toString() {
        return getName() + " v" + getVersion() + " (" + getId() + ")";
    }
}
