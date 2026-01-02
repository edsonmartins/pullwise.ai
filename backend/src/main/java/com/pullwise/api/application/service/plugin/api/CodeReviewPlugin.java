package com.pullwise.api.application.service.plugin.api;

import java.util.Set;

/**
 * Interface base para todos os plugins de code review.
 *
 * <p>Plugins podem ser escritos em Java, TypeScript ou Python.
 * Para plugins em outras linguagens, use wrappers apropriados.
 *
 * <p>Ciclo de vida:
 * <ol>
 *   <li>Descoberta do plugin (via SPI, diretório, etc)</li>
 *   <li>initialize() - Configurar o plugin</li>
 *   <li>analyze() - Executar análise (pode ser chamado múltiplas vezes)</li>
 *   <li>shutdown() - Limpar recursos</li>
 * </ol>
 */
public interface CodeReviewPlugin {

    // ========== Identificação ==========

    /**
     * Retorna o ID único do plugin.
     * <p>Formato recomendado: "com.company.plugin-name"
     *
     * @return ID do plugin
     */
    String getId();

    /**
     * Retorna o nome do plugin.
     *
     * @return Nome do plugin
     */
    String getName();

    /**
     * Retorna a versão do plugin.
     *
     * @return Versão (ex: "1.0.0")
     */
    String getVersion();

    /**
     * Retorna o autor do plugin.
     *
     * @return Autor ou empresa
     */
    String getAuthor();

    /**
     * Retorna a descrição do plugin.
     *
     * @return Descrição do que o plugin faz
     */
    String getDescription();

    // ========== Capabilities ==========

    /**
     * Retorna o tipo do plugin.
     *
     * @return Tipo do plugin
     */
    PluginType getType();

    /**
     * Retorna as linguagens suportadas pelo plugin.
     *
     * @return Conjunto de linguagens
     */
    Set<PluginLanguage> getSupportedLanguages();

    /**
     * Retorna os metadados completos do plugin.
     *
     * @return Metadados do plugin
     */
    default PluginMetadata getMetadata() {
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

    // ========== Lifecycle ==========

    /**
     * Inicializa o plugin com o contexto fornecido.
     * <p>Chamado uma vez no carregamento do plugin.
     *
     * @param context Contexto de configuração
     * @throws PluginException se a inicialização falhar
     */
    void initialize(PluginContext context) throws PluginException;

    /**
     * Executa a análise do código.
     * <p>Chamado para cada PR ou arquivo a ser analisado.
     *
     * @param request Request de análise com diff e contexto
     * @return Resultado da análise
     * @throws PluginException se a análise falhar
     */
    AnalysisResult analyze(AnalysisRequest request) throws PluginException;

    /**
     * Faz cleanup e libera recursos do plugin.
     * <p>Chamado no desligamento da aplicação.
     */
    void shutdown();

    // ========== Utilities ==========

    /**
     * Verifica se o plugin suporta uma determinada linguagem.
     *
     * @param language Linguagem a verificar
     * @return true se suporta
     */
    default boolean supportsLanguage(PluginLanguage language) {
        return getSupportedLanguages().contains(PluginLanguage.ALL) ||
               getSupportedLanguages().contains(language);
    }

    /**
     * Verifica se o plugin está habilitado.
     *
     * @return true se habilitado
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Retorna a prioridade de execução deste plugin.
     * <p>Plugins com menor prioridade executam primeiro.
     *
     * @return Prioridade (padrão: 100)
     */
    default int getPriority() {
        return 100;
    }
}
