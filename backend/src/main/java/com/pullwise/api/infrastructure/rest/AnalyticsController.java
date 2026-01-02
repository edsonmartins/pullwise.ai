package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.service.analytics.AnalyticsService;
import com.pullwise.api.domain.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para Analytics e Métricas.
 */
@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UsageRecordRepository usageRecordRepository;

    /**
     * Obtém métricas agregadas de uma organização.
     */
    @GetMapping("/organizations/{organizationId}/metrics")
    public ResponseEntity<Map<String, Object>> getOrganizationMetrics(
            @PathVariable Long organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        Map<String, Object> metrics = analyticsService.getOrganizationMetrics(
                organizationId, start, end
        );

        return ResponseEntity.ok(metrics);
    }

    /**
     * Obtém métricas de um projeto específico.
     */
    @GetMapping("/projects/{projectId}/metrics")
    public ResponseEntity<Map<String, Object>> getProjectMetrics(
            @PathVariable Long projectId,
            @RequestParam String yearMonth) {

        Map<String, Object> metrics = analyticsService.getProjectMetrics(projectId, yearMonth);

        return ResponseEntity.ok(metrics);
    }

    /**
     * Obtém tendência de reviews ao longo do tempo.
     */
    @GetMapping("/organizations/{organizationId}/review-trend")
    public ResponseEntity<List<Map<String, Object>>> getReviewTrend(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "6") int months) {

        List<Map<String, Object>> trend = analyticsService.getReviewTrend(
                organizationId, months
        );

        return ResponseEntity.ok(trend);
    }

    /**
     * Obtém breakdown de issues por severidade.
     */
    @GetMapping("/organizations/{organizationId}/issue-breakdown")
    public ResponseEntity<Map<String, Long>> getIssueBreakdown(
            @PathVariable Long organizationId,
            @RequestParam String yearMonth) {

        Map<String, Long> breakdown = analyticsService.getIssueBreakdown(
                organizationId, yearMonth
        );

        return ResponseEntity.ok(breakdown);
    }

    /**
     * Calcula custo estimado com LLM.
     */
    @GetMapping("/organizations/{organizationId}/estimated-cost")
    public ResponseEntity<Map<String, Object>> getEstimatedCost(
            @PathVariable Long organizationId,
            @RequestParam String yearMonth) {

        double cost = analyticsService.calculateEstimatedCost(organizationId, yearMonth);

        return ResponseEntity.ok(Map.of(
                "organizationId", organizationId,
                "period", yearMonth,
                "estimatedCost", cost,
                "currency", "USD"
        ));
    }

    /**
     * Obtém top projetos por número de reviews.
     */
    @GetMapping("/organizations/{organizationId}/top-projects")
    public ResponseEntity<List<Map<String, Object>>> getTopProjects(
            @PathVariable Long organizationId,
            @RequestParam(defaultValue = "5") int limit) {

        List<Map<String, Object>> topProjects = analyticsService.getTopProjectsByReviews(
                organizationId, limit
        );

        return ResponseEntity.ok(topProjects);
    }

    /**
     * Obtém resumo de usage para billing.
     */
    @GetMapping("/organizations/{organizationId}/usage-summary")
    public ResponseEntity<Map<String, Object>> getUsageSummary(
            @PathVariable Long organizationId) {

        String currentPeriod = LocalDate.now().toString().substring(0, 7); // YYYY-MM

        Long reviews = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                organizationId, currentPeriod, "reviews"
        );

        Long tokens = usageRecordRepository.sumByOrganizationIdAndPeriodAndMetricType(
                organizationId, currentPeriod, "llm_tokens"
        );

        return ResponseEntity.ok(Map.of(
                "period", currentPeriod,
                "reviews", reviews,
                "tokens", tokens,
                "withinFreeLimits", reviews <= 50
        ));
    }
}
