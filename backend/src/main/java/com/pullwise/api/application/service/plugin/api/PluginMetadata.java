package com.pullwise.api.application.service.plugin.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Metadados de um plugin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginMetadata {
    /**
     * ID único do plugin (ex: "com.company.custom-sast").
     */
    private String id;

    /**
     * Nome do plugin.
     */
    private String name;

    /**
     * Versão do plugin.
     */
    private String version;

    /**
     * Autor do plugin.
     */
    private String author;

    /**
     * Descrição do plugin.
     */
    private String description;

    /**
     * Tipo do plugin.
     */
    private PluginType type;

    /**
     * Linguagens suportadas.
     */
    @Builder.Default
    private Set<PluginLanguage> supportedLanguages = Set.of(PluginLanguage.ALL);

    /**
     * URL da documentação.
     */
    private String documentationUrl;

    /**
     * URL do repositório do código fonte.
     */
    private String repositoryUrl;

    /**
     * Homepage do plugin.
     */
    private String homepageUrl;

    /**
     * Tags para categorização.
     */
    private Set<String> tags;

    /**
     * Mínima versão do Pullwise necessária.
     */
    private String minPullwiseVersion;

    /**
     * Data de instalação.
     */
    private LocalDateTime installedAt;

    /**
     * Se o plugin está habilitado.
     */
    @Builder.Default
    private boolean enabled = true;
}
