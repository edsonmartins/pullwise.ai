package com.pullwise.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa configurações hierárquicas.
 * Escopo: ORGANIZATION > TEAM > PROJECT
 */
@Entity
@Table(name = "configurations", uniqueConstraints = {
    @UniqueConstraint(name = "uk_config_project_scope_key", columnNames = {"project_id", "scope", "config_key"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false, length = 20)
    private String scope; // ORGANIZATION, TEAM, PROJECT

    @Column(name = "config_key", nullable = false, length = 100)
    private String key;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "value_type", length = 20)
    @Builder.Default
    private String valueType = "STRING"; // STRING, BOOLEAN, NUMBER, JSON

    @Column(name = "is_sensitive")
    @Builder.Default
    private Boolean isSensitive = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (valueType == null) {
            valueType = "STRING";
        }
        if (isSensitive == null) {
            isSensitive = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Métodos auxiliares para configs comuns
    public static Configuration sastEnabled(String scope, boolean enabled) {
        return Configuration.builder()
                .scope(scope)
                .key("sast.enabled")
                .value(String.valueOf(enabled))
                .valueType("BOOLEAN")
                .build();
    }

    public static Configuration llmEnabled(String scope, boolean enabled) {
        return Configuration.builder()
                .scope(scope)
                .key("llm.enabled")
                .value(String.valueOf(enabled))
                .valueType("BOOLEAN")
                .build();
    }

    public static Configuration llmProvider(String scope, String provider) {
        return Configuration.builder()
                .scope(scope)
                .key("llm.provider")
                .value(provider)
                .valueType("STRING")
                .build();
    }

    public static Configuration llmModel(String scope, String model) {
        return Configuration.builder()
                .scope(scope)
                .key("llm.model")
                .value(model)
                .valueType("STRING")
                .build();
    }

    public static Configuration ragEnabled(String scope, boolean enabled) {
        return Configuration.builder()
                .scope(scope)
                .key("rag.enabled")
                .value(String.valueOf(enabled))
                .valueType("BOOLEAN")
                .build();
    }

    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(value);
    }

    public Integer getIntValue() {
        return Integer.parseInt(value);
    }
}
