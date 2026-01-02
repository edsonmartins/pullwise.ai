package com.pullwise.api.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade que representa o registro de uso para billing.
 */
@Entity
@Table(name = "usage_records", indexes = {
    @Index(name = "idx_usage_org_period", columnList = "organization_id,period"),
    @Index(name = "idx_usage_date", columnList = "usage_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "period", nullable = false, length = 7)
    private String period; // Formato: YYYY-MM

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // REVIEWS, LLM_TOKENS, SAST_SCANS, FILES_SCANNED

    @Column(name = "metric_value", nullable = false)
    private Long metricValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (usageDate == null) {
            usageDate = LocalDate.now();
        }
        if (period == null) {
            period = usageDate.toString().substring(0, 7); // YYYY-MM
        }
    }

    // Tipos de métricas
    public static final String METRIC_REVIEWS = "REVIEWS";
    public static final String METRIC_LLM_TOKENS = "LLM_TOKENS";
    public static final String METRIC_SAST_SCANS = "SAST_SCANS";
    public static final String METRIC_FILES_SCANNED = "FILES_SCANNED";
    public static final String METRIC_LINES_ANALYZED = "LINES_ANALYZED";

    public static UsageRecord of(Organization organization, Project project,
                                  LocalDate date, String metricType, long value) {
        return UsageRecord.builder()
                .organization(organization)
                .project(project)
                .usageDate(date)
                .period(date.toString().substring(0, 7))
                .metricType(metricType)
                .metricValue(value)
                .build();
    }
}
