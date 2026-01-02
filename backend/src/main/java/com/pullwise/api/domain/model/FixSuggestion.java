package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.FixConfidence;
import com.pullwise.api.domain.enums.FixStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Sugestão de correção automática para um issue.
 *
 * <p>Gerada por LLMs ou regras customizadas,
 * pode ser aplicada automaticamente ou manualmente.
 */
@Entity
@Table(name = "fix_suggestions", indexes = {
        @Index(name = "idx_fix_review_id", columnList = "review_id"),
        @Index(name = "idx_fix_issue_id", columnList = "issue_id"),
        @Index(name = "idx_fix_status", columnList = "status"),
        @Index(name = "idx_fix_confidence", columnList = "confidence")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Review ao qual esta sugestão pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    /**
     * Issue original que esta sugestão corrige.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    /**
     * Status da sugestão.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FixStatus status = FixStatus.PENDING;

    /**
     * Nível de confiança da correção.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FixConfidence confidence;

    /**
     * Código corrigido proposto.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String fixedCode;

    /**
     * Código original (para diff).
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String originalCode;

    /**
     * Explicação da correção.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String explanation;

    /**
     * Nome do arquivo a ser modificado.
     */
    @Column(length = 500)
    private String filePath;

    /**
     * Linha inicial da mudança.
     */
    private Integer startLine;

    /**
     * Linha final da mudança.
     */
    private Integer endLine;

    /**
     * Branch onde a correção será aplicada.
     */
    @Column(length = 255)
    private String branchName;

    /**
     * Commit hash após aplicação.
     */
    @Column(length = 100)
    private String appliedCommitHash;

    /**
     * Usuário que aprovou/rejeitou a sugestão.
     */
    @Column(length = 255)
    private String reviewedBy;

    /**
     * Quando a sugestão foi revisada.
     */
    private LocalDateTime reviewedAt;

    /**
     * Quando a sugestão foi aplicada.
     */
    private LocalDateTime appliedAt;

    /**
     * Mensagem de erro se aplicação falhou.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Modelo LLM usado para gerar a sugestão.
     */
    @Column(length = 100)
    private String modelUsed;

    /**
     * Tokens de entrada usados na geração.
     */
    private Integer inputTokens;

    /**
     * Tokens de saída gerados.
     */
    private Integer outputTokens;

    /**
     * Custo estimado em dólares.
     */
    private Double estimatedCost;

    /**
     * Data de criação.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Data de atualização.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica se a sugestão pode ser aplicada automaticamente.
     */
    public boolean canAutoApply() {
        return status == FixStatus.APPROVED && confidence.canAutoApply();
    }

    /**
     * Verifica se a sugestão está pronta para aplicação.
     */
    public boolean isReadyToApply() {
        return status.canApply() && confidence.canAutoApply();
    }

    /**
     * Calcula o diff da correção.
     */
    public String getDiff() {
        if (originalCode == null || fixedCode == null) {
            return null;
        }

        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(filePath).append("\n");
        diff.append("+++ b/").append(filePath).append("\n");

        if (startLine != null) {
            diff.append("@@ -").append(startLine);
            if (endLine != null && !endLine.equals(startLine)) {
                diff.append(",").append(endLine - startLine + 1);
            }
            diff.append(" +").append(startLine);
            if (endLine != null && !endLine.equals(startLine)) {
                diff.append(",").append(endLine - startLine + 1);
            }
            diff.append(" @@\n");
        }

        // Adiciona linhas originais com -
        for (String line : originalCode.split("\n")) {
            diff.append("-").append(line).append("\n");
        }

        // Adiciona linhas corrigidas com +
        for (String line : fixedCode.split("\n")) {
            diff.append("+").append(line).append("\n");
        }

        return diff.toString();
    }

    /**
     * Retorna um resumo da correção.
     */
    public String getSummary() {
        if (explanation != null && !explanation.isEmpty()) {
            // Primeira linha da explicação
            String[] lines = explanation.split("\n");
            if (lines.length > 0 && lines[0].length() > 0) {
                return lines[0].substring(0, Math.min(100, lines[0].length()));
            }
        }

        if (fixedCode != null) {
            int lineCount = fixedCode.split("\n").length;
            return String.format("Correção de %d linha(s) em %s", lineCount,
                    filePath != null ? filePath : "arquivo");
        }

        return "Sugestão de correção";
    }
}
