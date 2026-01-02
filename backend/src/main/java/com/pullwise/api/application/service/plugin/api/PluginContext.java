package com.pullwise.api.application.service.plugin.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Map;

/**
 * Contexto fornecido para um plugin durante sua inicialização.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginContext {
    /**
     * Diretório de dados do plugin para armazenamento persistente.
     */
    private Path dataDirectory;

    /**
     * Diretório de trabalho do plugin.
     */
    private Path workingDirectory;

    /**
     * Configuração do plugin do arquivo application.yml.
     */
    private Map<String, Object> configuration;

    /**
     * ID do repositório (para plugins que precisam saber o contexto).
     */
    private String repositoryId;

    /**
     * Se o plugin está em modo de desenvolvimento.
     */
    private boolean developmentMode;
}
