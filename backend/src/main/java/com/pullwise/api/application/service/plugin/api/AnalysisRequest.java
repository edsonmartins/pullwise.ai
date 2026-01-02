package com.pullwise.api.application.service.plugin.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request de análise passado para um plugin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequest {
    /**
     * Diff completo do PR (git diff).
     */
    private String diff;

    /**
     * Lista de arquivos modificados.
     */
    private List<String> changedFiles;

    /**
     * Conteúdo dos arquivos modificados (mapeados por path).
     */
    private Map<String, String> fileContents;

    /**
     * Branch de origem.
     */
    private String sourceBranch;

    /**
     * Branch de destino.
     */
    private String targetBranch;

    /**
     * Título do PR.
     */
    private String pullRequestTitle;

    /**
     * Descrição do PR.
     */
    private String pullRequestDescription;

    /**
     * ID do repositório.
     */
    private String repositoryId;

    /**
     * URL do repositório.
     */
    private String repositoryUrl;

    /**
     * Configuração específica do plugin.
     */
    private Map<String, Object> pluginConfig;

    /**
     * Timeout em segundos para a análise.
     */
    @Builder.Default
    private int timeoutSeconds = 60;

    /**
     * Retorna o conteúdo de um arquivo específico.
     */
    public String getFileContent(String path) {
        if (fileContents == null) {
            return null;
        }
        return fileContents.get(path);
    }
}
