package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.response.ApiResponse;
import com.pullwise.api.application.service.llm.LLMAnalyticsService;
import com.pullwise.api.application.service.llm.model.LLMModelConfig;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.domain.enums.ReviewTaskType;
import com.pullwise.api.domain.model.LLMRoutingDecision;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller para analytics e configuração do roteamento LLM.
 */
@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
@Tag(name = "LLM Routing", description = "Analytics e configuração do roteamento multi-modelo")
public class LLMRoutingController {

    private final LLMAnalyticsService analyticsService;
    private final MultiModelLLMRouter router;
    private final LLMModelConfig config;

    @GetMapping("/routing/analytics")
    @Operation(summary = "Busca analytics de roteamento LLM")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoutingAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        Map<String, Object> analytics = analyticsService.getRoutingAnalytics(startDateTime, endDateTime);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/routing/by-model")
    @Operation(summary = "Uso por modelo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsageByModel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        List<Map<String, Object>> usageByModel = analyticsService.getUsageByModel(startDateTime, endDateTime);
        return ResponseEntity.ok(ApiResponse.success(usageByModel));
    }

    @GetMapping("/routing/decisions")
    @Operation(summary = "Histórico de decisões de roteamento")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LLMRoutingDecision>>> getRoutingDecisions(
            @RequestParam(defaultValue = "7") int days) {

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<LLMRoutingDecision> decisions = analyticsService.getRecentDecisions(since);
        return ResponseEntity.ok(ApiResponse.success(decisions));
    }

    @GetMapping("/routing/by-task")
    @Operation(summary = "Analytics por tipo de tarefa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalyticsByTask(
            @RequestParam ReviewTaskType taskType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(23, 59, 59);

        Map<String, Object> analytics = analyticsService.getAnalyticsByTask(taskType, startDateTime, endDateTime);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/config")
    @Operation(summary = "Configuração atual do router")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRouterConfig() {
        Map<String, Object> configData = Map.of(
                "strategy", config.getRouter().getStrategy(),
                "defaultModel", config.getRouter().getDefaultModel(),
                "fallbackModel", config.getRouter().getFallbackModel(),
                "models", config.getModels().keySet(),
                "costTracking", Map.of(
                        "enabled", config.getCostTracking().isEnabled(),
                        "dailyBudget", config.getCostTracking().getDailyBudget(),
                        "alertThreshold", config.getCostTracking().getAlertThreshold()
                )
        );
        return ResponseEntity.ok(ApiResponse.success(configData));
    }

    @PutMapping("/config/strategy")
    @Operation(summary = "Altera estratégia do router")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateStrategy(@RequestParam LLMModelConfig.Strategy strategy) {
        analyticsService.updateRoutingStrategy(strategy);
        return ResponseEntity.ok(ApiResponse.success("Strategy updated to " + strategy));
    }
}
