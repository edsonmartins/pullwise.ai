package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.ReviewTaskType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de decisões de roteamento do Multi-Model LLM Router.
 *
 * <p>Entidade usada para:
 * - Analytics: quais modelos são mais usados
 * - Custos: tracking de gastos por modelo
 * - Performance: latência por tipo de tarefa
 * - Otimização: dados para treinar/ajustar o router
 *
 * @see com.pullwise.api.application.service.llm.MultiModelLLMRouter
 */
@Entity
@Table(name = "llm_routing_decisions", indexes = {
    @Index(name = "idx_lrd_review", columnList = "review_id"),
    @Index(name = "idx_lrd_task", columnList = "task_type"),
    @Index(name = "idx_lrd_model", columnList = "selected_model"),
    @Index(name = "idx_lrd_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LLMRoutingDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Review associado a esta decisão (opcional, pode ser null para requests standalone).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    private Review review;

    /**
     * Tipo de tarefa que foi roteada.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 50)
    private ReviewTaskType taskType;

    /**
     * Modelo LLM selecionado (ex: "anthropic/claude-3.5-sonnet").
     */
    @Column(name = "selected_model", nullable = false, length = 100)
    private String selectedModel;

    /**
     * Provedor do modelo (ex: OPENROUTER, OLLAMA).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private LLMProvider provider;

    /**
     * Número de tokens de entrada enviados ao modelo.
     */
    @Column(name = "input_tokens")
    private Integer inputTokens;

    /**
     * Número de tokens de saída gerados pelo modelo.
     */
    @Column(name = "output_tokens")
    private Integer outputTokens;

    /**
     * Custo estimado desta chamada em USD.
     */
    @Column(name = "cost_usd", precision = 10, scale = 4)
    private BigDecimal costUsd;

    /**
     * Latência da chamada em milissegundos.
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * JSON com detalhes adicionais da decisão de roteamento.
     * Pode incluir: razão da escolha, alternativas consideradas, score de confiança, etc.
     */
    @Lob
    @Column(name = "reasoning")
    private String reasoning;

    /**
     * Data de criação do registro.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * Enum para provedores de LLM.
     */
    public enum LLMProvider {
        OPENROUTER, ANTHROPIC, OPENAI, OLLAMA, BEDROCK, AZURE_OPENAI
    }

    /**
     * Calcula o total de tokens processados.
     */
    public Integer getTotalTokens() {
        if (inputTokens == null && outputTokens == null) {
            return null;
        }
        return (inputTokens != null ? inputTokens : 0) + (outputTokens != null ? outputTokens : 0);
    }

    /**
     * Calcula o custo por 1K tokens.
     */
    public BigDecimal getCostPer1kTokens() {
        if (costUsd == null || getTotalTokens() == null || getTotalTokens() == 0) {
            return BigDecimal.ZERO;
        }
        return costUsd.multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(getTotalTokens()), 6, BigDecimal.ROUND_HALF_UP);
    }
}
