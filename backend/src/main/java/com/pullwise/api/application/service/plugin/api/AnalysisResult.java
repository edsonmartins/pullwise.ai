package com.pullwise.api.application.service.plugin.api;

import com.pullwise.api.domain.model.Issue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Resultado da análise de um plugin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {
    /**
     * ID do plugin que gerou este resultado.
     */
    private String pluginId;

    /**
     * Issues encontrados pelo plugin.
     */
    private List<Issue> issues;

    /**
     * Metadados adicionais da análise.
     */
    private Map<String, Object> metadata;

    /**
     * Tempo de execução da análise.
     */
    private Duration executionTime;

    /**
     * Se a análise foi executada com sucesso.
     */
    @Builder.Default
    private boolean success = true;

    /**
     * Mensagem de erro se a análise falhou.
     */
    private String errorMessage;

    /**
     * Timestamp da análise.
     */
    @Builder.Default
    private LocalDateTime analyzedAt = LocalDateTime.now();

    /**
     * Cria um resultado de erro.
     */
    public static AnalysisResult error(String pluginId, String errorMessage) {
        return AnalysisResult.builder()
                .pluginId(pluginId)
                .success(false)
                .errorMessage(errorMessage)
                .issues(List.of())
                .build();
    }

    /**
     * Cria um resultado vazio.
     */
    public static AnalysisResult empty(String pluginId) {
        return AnalysisResult.builder()
                .pluginId(pluginId)
                .success(true)
                .issues(List.of())
                .build();
    }
}
