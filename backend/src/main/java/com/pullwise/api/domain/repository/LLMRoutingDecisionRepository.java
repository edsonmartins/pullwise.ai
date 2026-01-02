package com.pullwise.api.domain.repository;

import com.pullwise.api.domain.model.LLMRoutingDecision;
import com.pullwise.api.domain.enums.ReviewTaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para decisões de roteamento LLM.
 */
@Repository
public interface LLMRoutingDecisionRepository extends JpaRepository<LLMRoutingDecision, Long> {

    /**
     * Busca todas as decisões para um review específico.
     */
    List<LLMRoutingDecision> findByReviewId(Long reviewId);

    /**
     * Busca decisões por tipo de tarefa.
     */
    List<LLMRoutingDecision> findByTaskType(ReviewTaskType taskType);

    /**
     * Busca decisões por modelo selecionado.
     */
    List<LLMRoutingDecision> findBySelectedModel(String modelId);

    /**
     * Calcula o custo total de um período.
     */
    @Query("SELECT SUM(d.costUsd) FROM LLMRoutingDecision d WHERE d.createdAt BETWEEN :start AND :end")
    Optional<java.math.BigDecimal> sumCostByPeriod(@Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    /**
     * Calcula o custo total por organização em um período.
     */
    @Query("SELECT SUM(d.costUsd) FROM LLMRoutingDecision d " +
           "WHERE d.review.organization.id = :orgId AND d.createdAt BETWEEN :start AND :end")
    Optional<java.math.BigDecimal> sumCostByOrganizationAndPeriod(@Param("orgId") Long orgId,
                                                                   @Param("start") LocalDateTime start,
                                                                   @Param("end") LocalDateTime end);

    /**
     * Conta uso por modelo em um período.
     */
    @Query("SELECT d.selectedModel, COUNT(d) FROM LLMRoutingDecision d " +
           "WHERE d.createdAt BETWEEN :start AND :end " +
           "GROUP BY d.selectedModel")
    List<Object[]> countUsageByModel(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    /**
     * Busca decisões das últimas N horas.
     */
    @Query("SELECT d FROM LLMRoutingDecision d WHERE d.createdAt >= :since")
    List<LLMRoutingDecision> findRecent(@Param("since") LocalDateTime since);

    /**
     * Calcula latência média por modelo.
     */
    @Query("SELECT d.selectedModel, AVG(d.latencyMs) FROM LLMRoutingDecision d " +
           "WHERE d.createdAt BETWEEN :start AND :end " +
           "GROUP BY d.selectedModel")
    List<Object[]> averageLatencyByModel(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /**
     * Calcula total de tokens processados em um período.
     */
    @Query("SELECT SUM(d.inputTokens + d.outputTokens) FROM LLMRoutingDecision d " +
           "WHERE d.createdAt BETWEEN :start AND :end")
    Optional<Long> sumTokensByPeriod(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
