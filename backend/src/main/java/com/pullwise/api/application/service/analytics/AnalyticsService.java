package com.pullwise.api.application.service.analytics;

import com.pullwise.api.domain.model.Organization;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.UsageRecord;
import com.pullwise.api.domain.repository.*;
import com.pullwise.api.domain.enums.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviço para análise de métricas e usage da plataforma.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final ReviewRepository reviewRepository;
    private final IssueRepository issueRepository;
    private final PullRequestRepository pullRequestRepository;
    private final UsageRecordRepository usageRecordRepository;

    /**
     * Obtém métricas gerais de uma organização em um período.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrganizationMetrics(Long organizationId, LocalDate start, LocalDate end) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        // Contagem de reviews no período
        long reviewCount = reviewRepository.findByOrganizationIdAndCreatedAtBetween(
                organizationId, startDateTime, endDateTime).size();

        // Contagem de issues encontradas
        long issuesFound = issueRepository.countByReviewOrganizationIdAndCreatedAtBetween(
                organizationId, startDateTime, endDateTime);

        // Contagem de PRs analisados
        long prsAnalyzed = pullRequestRepository.countByOrganizationIdAndCreatedAtBetween(
                organizationId, startDateTime, endDateTime);

        // Issues por severidade
        Map<String, Long> issuesBySeverity = Map.of(
                "critical", issueRepository.countByReviewOrganizationIdAndSeverityAndCreatedAtBetween(
                        organizationId, Severity.CRITICAL, startDateTime, endDateTime),
                "high", issueRepository.countByReviewOrganizationIdAndSeverityAndCreatedAtBetween(
                        organizationId, Severity.HIGH, startDateTime, endDateTime),
                "medium", issueRepository.countByReviewOrganizationIdAndSeverityAndCreatedAtBetween(
                        organizationId, Severity.MEDIUM, startDateTime, endDateTime),
                "low", issueRepository.countByReviewOrganizationIdAndSeverityAndCreatedAtBetween(
                        organizationId, Severity.LOW, startDateTime, endDateTime)
        );

        // Total de reviews da organização
        long totalReviews = reviewRepository.countByOrganizationId(organizationId);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("organizationName", organization.getName());
        metrics.put("periodStart", start);
        metrics.put("periodEnd", end);
        metrics.put("reviewCount", reviewCount);
        metrics.put("issuesFound", issuesFound);
        metrics.put("prsAnalyzed", prsAnalyzed);
        metrics.put("issuesBySeverity", issuesBySeverity);
        metrics.put("totalReviews", totalReviews);
        metrics.put("planType", organization.getPlanType().name());

        return metrics;
    }

    /**
     * Obtém métricas de um projeto específico.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProjectMetrics(Long projectId, String yearMonth) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LocalDateTime startDateTime = monthStart.atStartOfDay();
        LocalDateTime endDateTime = monthEnd.atTime(23, 59, 59);

        long reviewCount = reviewRepository.countByProjectIdAndPeriod(projectId, startDateTime, endDateTime);
        long issueCount = issueRepository.countByProjectIdAndPeriod(projectId, startDateTime, endDateTime);

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("projectId", projectId);
        metrics.put("projectName", project.getName());
        metrics.put("period", yearMonth);
        metrics.put("reviewCount", reviewCount);
        metrics.put("issueCount", issueCount);

        return metrics;
    }

    /**
     * Obtém tendência de reviews nos últimos N meses.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReviewTrend(Long organizationId, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth yearMonth = YearMonth.from(now.minusMonths(i));
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            LocalDateTime startDateTime = monthStart.atStartOfDay();
            LocalDateTime endDateTime = monthEnd.atTime(23, 59, 59);

            long count = reviewRepository.findByOrganizationIdAndCreatedAtBetween(
                    organizationId, startDateTime, endDateTime).size();

            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("period", yearMonth.format(formatter));
            dataPoint.put("count", count);
            trend.add(dataPoint);
        }

        return trend;
    }

    /**
     * Obtém breakdown de issues por tipo em um mês.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getIssueBreakdown(Long organizationId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        LocalDateTime startDateTime = monthStart.atStartOfDay();
        LocalDateTime endDateTime = monthEnd.atTime(23, 59, 59);

        Map<String, Long> breakdown = new HashMap<>();

        for (com.pullwise.api.domain.enums.IssueType type : com.pullwise.api.domain.enums.IssueType.values()) {
            long count = issueRepository.countByReviewOrganizationIdAndIssueTypeAndCreatedAtBetween(
                    organizationId, type, startDateTime, endDateTime);
            breakdown.put(type.name(), count);
        }

        return breakdown;
    }

    /**
     * Calcula custo estimado baseado no uso.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateCostMetrics(Long organizationId, int months) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        LocalDate now = LocalDate.now();
        LocalDate start = now.minusMonths(months);

        List<com.pullwise.api.domain.model.Review> reviews = reviewRepository
                .findByOrganizationIdAndCreatedAtBetween(
                        organizationId,
                        start.atStartOfDay(),
                        now.atTime(23, 59, 59)
                );

        int totalReviews = reviews.size();

        double monthlyPrice = switch (organization.getPlanType()) {
            case PRO -> 24.0;
            case ENTERPRISE -> 299.0;
            default -> 0.0;
        };

        double estimatedCost = monthlyPrice * months;
        double costPerReview = totalReviews > 0 ? estimatedCost / totalReviews : 0;

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("planType", organization.getPlanType().name());
        metrics.put("monthlyPrice", monthlyPrice);
        metrics.put("periodMonths", months);
        metrics.put("totalReviews", totalReviews);
        metrics.put("estimatedCost", estimatedCost);
        metrics.put("costPerReview", costPerReview);

        return metrics;
    }

    /**
     * Calcula custo estimado para um período específico.
     */
    @Transactional(readOnly = true)
    public double calculateEstimatedCost(Long organizationId, String yearMonth) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        YearMonth ym = YearMonth.parse(yearMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<com.pullwise.api.domain.model.Review> reviews = reviewRepository
                .findByOrganizationIdAndCreatedAtBetween(
                        organizationId,
                        monthStart.atStartOfDay(),
                        monthEnd.atTime(23, 59, 59)
                );

        double monthlyPrice = switch (organization.getPlanType()) {
            case PRO -> 24.0;
            case ENTERPRISE -> 299.0;
            default -> 0.0;
        };

        return monthlyPrice;
    }

    /**
     * Obtém top projetos por número de reviews.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopProjectsByReviews(Long organizationId, int limit) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        return organization.getProjects().stream()
                .map(project -> {
                    long reviewCount = reviewRepository.findByProjectId(project.getId()).size();
                    Map<String, Object> data = new HashMap<>();
                    data.put("projectId", project.getId());
                    data.put("projectName", project.getName());
                    data.put("reviewCount", reviewCount);
                    return data;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("reviewCount"), (Long) a.get("reviewCount")))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Obtém estatísticas de uso de LLM (tokens, chamadas, etc).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLLMUsageStats(Long organizationId, String period) {
        Long totalTokens = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                organizationId, period, "LLM_TOKENS");

        Long totalCalls = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                organizationId, period, "LLM_CALLS");

        Map<String, Object> stats = new HashMap<>();
        stats.put("period", period);
        stats.put("totalTokens", totalTokens != null ? totalTokens : 0L);
        stats.put("totalCalls", totalCalls != null ? totalCalls : 0L);

        long tokens = totalTokens != null ? totalTokens : 0L;
        double estimatedCost = (tokens / 1000.0) * 0.001;
        stats.put("estimatedCost", estimatedCost);

        return stats;
    }

    /**
     * Obtém summary de usage por período.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUsageSummary(Long organizationId, String startPeriod, String endPeriod) {
        List<Object[]> results = usageRecordRepository.summarizeByPeriodRange(
                organizationId, startPeriod, endPeriod);

        return results.stream().map(row -> {
            Map<String, Object> entry = new HashMap<>();
            entry.put("period", row[0]);
            entry.put("metricType", row[1]);
            entry.put("total", ((Number) row[2]).longValue());
            return entry;
        }).collect(Collectors.toList());
    }

    /**
     * Registra uma métrica de usage.
     */
    @Transactional
    public void recordUsage(Long organizationId, Long projectId, String metricType, long value, String period) {
        Optional<UsageRecord> existing = usageRecordRepository
                .findByOrganizationIdAndProjectIdAndPeriodAndMetricType(
                        organizationId, projectId, period, metricType);

        if (existing.isPresent()) {
            UsageRecord record = existing.get();
            record.setMetricValue(record.getMetricValue() + value);
            usageRecordRepository.save(record);
        } else {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

            UsageRecord record = UsageRecord.builder()
                    .organization(org)
                    .project(projectId != null ?
                            org.getProjects().stream()
                                    .filter(p -> p.getId().equals(projectId))
                                    .findFirst()
                                    .orElse(null) : null)
                    .metricType(metricType)
                    .metricValue(value)
                    .period(period)
                    .usageDate(LocalDate.now())
                    .build();

            usageRecordRepository.save(record);
        }

        log.debug("Recorded usage: org={}, project={}, type={}, value={}, period={}",
                organizationId, projectId, metricType, value, period);
    }
}
