package com.pullwise.api.application.service.llm;

import com.pullwise.api.application.service.llm.model.LLMModelConfig;
import com.pullwise.api.domain.model.LLMRoutingDecision;
import com.pullwise.api.domain.repository.LLMRoutingDecisionRepository;
import com.pullwise.api.domain.enums.ReviewTaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service para analytics do roteamento LLM.
 *
 * <p>Fornece métricas sobre:
 * - Uso por modelo
 * - Custos por período
 * - Latência média
 * - Volume de tokens
 */
@Service
@RequiredArgsConstructor
public class LLMAnalyticsService {

    private final LLMRoutingDecisionRepository decisionRepository;
    private final LLMModelConfig config;

    /**
     * Retorna analytics geral de roteamento para um período.
     */
    public Map<String, Object> getRoutingAnalytics(LocalDateTime start, LocalDateTime end) {
        // Custos
        BigDecimal totalCost = decisionRepository.sumCostByPeriod(start, end)
                .orElse(BigDecimal.ZERO);

        // Tokens
        Long totalTokens = decisionRepository.sumTokensByPeriod(start, end)
                .orElse(0L);

        // Total de chamadas
        List<LLMRoutingDecision> decisions = decisionRepository.findAll().stream()
                .filter(d -> d.getCreatedAt().isAfter(start) && d.getCreatedAt().isBefore(end))
                .toList();

        long totalCalls = decisions.size();

        // Latência média
        double avgLatency = decisions.stream()
                .filter(d -> d.getLatencyMs() != null)
                .mapToInt(LLMRoutingDecision::getLatencyMs)
                .average()
                .orElse(0.0);

        // Uso por tipo de tarefa
        Map<ReviewTaskType, Long> callsByTask = decisions.stream()
                .filter(d -> d.getTaskType() != null)
                .collect(Collectors.groupingBy(
                        LLMRoutingDecision::getTaskType,
                        Collectors.counting()
                ));

        return Map.of(
                "period", Map.of("start", start, "end", end),
                "totalCost", totalCost,
                "totalTokens", totalTokens,
                "totalCalls", totalCalls,
                "avgLatencyMs", Math.round(avgLatency),
                "callsByTask", callsByTask,
                "avgCostPerCall", totalCalls > 0
                        ? totalCost.divide(BigDecimal.valueOf(totalCalls), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO
        );
    }

    /**
     * Retorna uso por modelo em um período.
     */
    public List<Map<String, Object>> getUsageByModel(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = decisionRepository.countUsageByModel(start, end);

        return results.stream()
                .map(row -> {
                    String modelId = (String) row[0];
                    Long count = (Long) row[1];

                    // Busca configuração do modelo
                    var modelConfig = config.findModelById(modelId);

                    Map<String, Object> result = new HashMap<>();
                    result.put("modelId", modelId);
                    result.put("count", count);
                    result.put("provider", modelConfig.map(m -> m.getProvider().name()).orElse("UNKNOWN"));
                    result.put("isLocal", modelConfig.map(LLMModelConfig.ModelConfig::isLocal).orElse(false));
                    result.put("costPer1kTokens", modelConfig.map(LLMModelConfig.ModelConfig::getCostPer1kTokens).orElse(0.0));
                    return result;
                })
                .sorted((a, b) -> ((Long) b.get("count")).compareTo((Long) a.get("count")))
                .collect(Collectors.toList());
    }

    /**
     * Retorna decisões recentes de roteamento.
     */
    public List<LLMRoutingDecision> getRecentDecisions(LocalDateTime since) {
        return decisionRepository.findRecent(since);
    }

    /**
     * Retorna analytics para um tipo específico de tarefa.
     */
    public Map<String, Object> getAnalyticsByTask(ReviewTaskType taskType, LocalDateTime start, LocalDateTime end) {
        List<LLMRoutingDecision> taskDecisions = decisionRepository.findByTaskType(taskType).stream()
                .filter(d -> d.getCreatedAt().isAfter(start) && d.getCreatedAt().isBefore(end))
                .toList();

        // Uso por modelo para esta tarefa
        Map<String, Long> usageByModel = taskDecisions.stream()
                .collect(Collectors.groupingBy(
                        LLMRoutingDecision::getSelectedModel,
                        Collectors.counting()
                ));

        // Custo total
        BigDecimal totalCost = taskDecisions.stream()
                .filter(d -> d.getCostUsd() != null)
                .map(LLMRoutingDecision::getCostUsd)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Latência média
        double avgLatency = taskDecisions.stream()
                .filter(d -> d.getLatencyMs() != null)
                .mapToInt(LLMRoutingDecision::getLatencyMs)
                .average()
                .orElse(0.0);

        return Map.of(
                "taskType", taskType,
                "taskName", taskType.getDisplayName(),
                "totalCalls", taskDecisions.size(),
                "totalCost", totalCost,
                "avgLatencyMs", Math.round(avgLatency),
                "usageByModel", usageByModel,
                "qualityPriority", taskType.getQualityPriority(),
                "canUseLocalModel", taskType.canUseLocalModel()
        );
    }

    /**
     * Atualiza a estratégia de roteamento.
     *
     * <p>Em uma implementação completa, isso atualizaria o config em tempo de execução.
     * Por ora, apenas registra a mudança desejada.
     */
    public void updateRoutingStrategy(LLMModelConfig.Strategy strategy) {
        // Em uma implementação com config dinâmica, atualizaria aqui
        // Por enquanto, assume que será atualizado via application.yml e reload
        config.getRouter().setStrategy(strategy);
    }

    /**
     * Retorna o custo total de uma organização em um período.
     */
    public BigDecimal getCostByOrganization(Long organizationId, LocalDateTime start, LocalDateTime end) {
        return decisionRepository.sumCostByOrganizationAndPeriod(organizationId, start, end)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Verifica se o orçamento diário foi excedido.
     */
    public boolean isDailyBudgetExceeded(LocalDateTime date) {
        LocalDateTime start = date.toLocalDate().atStartOfDay();
        LocalDateTime end = date.toLocalDate().atTime(23, 59, 59);

        BigDecimal todayCost = decisionRepository.sumCostByPeriod(start, end)
                .orElse(BigDecimal.ZERO);

        double threshold = config.getCostTracking().getDailyBudget() * config.getCostTracking().getAlertThreshold();

        return todayCost.compareTo(BigDecimal.valueOf(threshold)) > 0;
    }

    /**
     * Retorna latência média por modelo em um período.
     */
    public Map<String, Double> getAverageLatencyByModel(LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = decisionRepository.averageLatencyByModel(start, end);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Double) row[1]
                ));
    }
}
