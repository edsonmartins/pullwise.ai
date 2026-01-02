package com.pullwise.api.domain.model;

import com.pullwise.api.domain.enums.IssueSource;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade que representa um problema/encontrado no código.
 */
@Entity
@Table(name = "issues", indexes = {
    @Index(name = "idx_issue_review_id", columnList = "review_id"),
    @Index(name = "idx_issue_severity", columnList = "severity"),
    @Index(name = "idx_issue_source", columnList = "source")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @Column(name = "severity", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private IssueType type;

    @Column(name = "source", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private IssueSource source;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "line_start")
    private Integer lineStart;

    @Column(name = "line_end")
    private Integer lineEnd;

    @Column(name = "rule_id", length = 100)
    private String ruleId; // ID da regra que gerou a issue (ex: java:S1135)

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion; // Sugestão de correção

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet; // Trecho de código com problema

    @Column(name = "fixed_code", columnDefinition = "TEXT")
    private String fixedCode; // Código corrigido

    @Column(name = "comment_id", length = 100)
    private String commentId; // ID do comentário postado no PR

    @Column(name = "is_false_positive")
    @Builder.Default
    private Boolean isFalsePositive = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isFalsePositive == null) {
            isFalsePositive = false;
        }
    }

    public boolean hasLocation() {
        return filePath != null && !filePath.isBlank();
    }

    public String getLocationString() {
        if (!hasLocation()) {
            return "General";
        }
        if (lineStart != null && lineEnd != null) {
            return filePath + ":" + lineStart + "-" + lineEnd;
        }
        if (lineStart != null) {
            return filePath + ":" + lineStart;
        }
        return filePath;
    }
}
