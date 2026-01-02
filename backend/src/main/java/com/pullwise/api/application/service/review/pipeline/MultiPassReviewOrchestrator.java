package com.pullwise.api.application.service.review.pipeline;

import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.application.service.review.pipeline.pass.*;
import com.pullwise.api.application.service.review.pipeline.synthesis.ResultSynthesizer;
import com.pullwise.api.application.service.review.pipeline.synthesis.IssueDuplicationDetector;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.enums.*;
import com.pullwise.api.domain.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Orquestrador do pipeline de múltiplas passadas para code review.
 *
 * <p>Executa 4 passadas de análise:
 * <ol>
 *   <li><b>Passada 1 - SAST</b>: Análise estática em paralelo (40+ ferramentas)</li>
 *   <li><b>Passada 2 - LLM Primary</b>: Análise de lógica de negócio</li>
 *   <li><b>Passada 3 - Security Focus</b>: Análise profunda de segurança</li>
 *   <li><b>Passada 4 - Code Graph Impact</b>: Análise de impacto cross-file</li>
 * </ol>
 *
 * <p>As passadas 2-4 são executadas sequencialmente, cada uma usando o contexto
 * das passadas anteriores.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiPassReviewOrchestrator {

    private final SastAggregatorPass sastAggregatorPass;
    private final LlmPrimaryPass llmPrimaryPass;
    private final SecurityFocusedPass securityFocusedPass;
    private final CodeGraphImpactPass codeGraphImpactPass;
    private final ResultSynthesizer resultSynthesizer;
    private final IssueDuplicationDetector duplicationDetector;
    private final MultiModelLLMRouter llmRouter;
    private final IssueRepository issueRepository;

    /**
     * Executa o pipeline completo de múltiplas passadas.
     *
     * @param pullRequest O PR a ser analisado
     * @param review      O review a ser populado
     * @return ReviewResult com todos os issues e summary
     */
    @Transactional
    public ReviewResult executePipeline(PullRequest pullRequest, Review review) {
        String repoIdentifier = pullRequest.getProject() != null
                ? pullRequest.getProject().getName()
                : "unknown";
        log.info("Starting multi-pass pipeline for PR {}/{}", repoIdentifier, pullRequest.getPrNumber());

        long startTime = System.currentTimeMillis();
        ReviewResult result = new ReviewResult();
        result.setReviewId(review.getId());

        try {
            // ============================================
            // PASSADA 1: SAST (Paralelo)
            // ============================================
            log.debug("Pass 1/4: SAST Aggregation");
            PassResult sastResult = executeSastPass(pullRequest, review);
            result.setSastResult(sastResult);

            // ============================================
            // PASSADA 2: LLM Primary
            // ============================================
            log.debug("Pass 2/4: LLM Primary Analysis");
            PassResult llmResult = executeLlmPrimaryPass(pullRequest, review, sastResult);
            result.setLlmResult(llmResult);

            // ============================================
            // PASSADA 3: Security Focus
            // ============================================
            log.debug("Pass 3/4: Security-Focused Analysis");
            PassResult securityResult = executeSecurityPass(pullRequest, review, sastResult, llmResult);
            result.setSecurityResult(securityResult);

            // ============================================
            // PASSADA 4: Code Graph Impact
            // ============================================
            log.debug("Pass 4/4: Code Graph Impact Analysis");
            PassResult impactResult = executeImpactPass(pullRequest, review, sastResult, llmResult);
            result.setImpactResult(impactResult);

            // ============================================
            // SÍNTESE FINAL
            // ============================================
            log.debug("Synthesizing results");
            List<Issue> allIssues = result.collectAllIssues();

            // Deduplicação
            List<Issue> deduplicatedIssues = duplicationDetector.deduplicate(allIssues);
            result.setDeduplicatedIssues(deduplicatedIssues);

            // Geração de executive summary
            String executiveSummary = generateExecutiveSummary(review, deduplicatedIssues, result);
            result.setExecutiveSummary(executiveSummary);

            // Salvar issues no banco
            List<Issue> savedIssues = issueRepository.saveAll(deduplicatedIssues);
            result.setSavedIssues(savedIssues);

            // Atualizar review
            review.complete();
            // Nota: O summary é armazenado no ReviewResult, não na entidade Review
            // Para persistir o summary, precisaríamos adicionar campo na entidade

            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            result.setSuccess(true);

            log.info("Multi-pass pipeline completed in {}ms. Issues found: {}", duration, savedIssues.size());

        } catch (Exception e) {
            log.error("Error executing multi-pass pipeline", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());

            review.fail(e.getMessage());
        }

        return result;
    }

    /**
     * Executa o pipeline de forma assíncrona.
     */
    @Async
    @Transactional
    public CompletableFuture<ReviewResult> executePipelineAsync(PullRequest pullRequest, Review review) {
        return CompletableFuture.supplyAsync(() -> executePipeline(pullRequest, review));
    }

    // ========== Private Methods ==========

    /**
     * Passada 1: SAST - executa em paralelo todas as ferramentas.
     */
    private PassResult executeSastPass(PullRequest pullRequest, Review review) {
        try {
            PassResult result = sastAggregatorPass.execute(pullRequest, review);
            log.debug("SAST pass completed: {} issues found", result.getIssues().size());
            return result;
        } catch (Exception e) {
            log.warn("SAST pass failed, continuing with degraded results", e);
            return PassResult.empty("SAST pass failed: " + e.getMessage());
        }
    }

    /**
     * Passada 2: LLM Primary - análise de lógica e código.
     */
    private PassResult executeLlmPrimaryPass(PullRequest pullRequest, Review review,
                                             PassResult sastResult) {
        try {
            // Timeout de 5 minutos para análise LLM
            return executeWithTimeout(() -> llmPrimaryPass.execute(pullRequest, review, sastResult),
                    5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("LLM Primary pass failed, using fallback", e);
            return createFallbackResult("LLM Primary", sastResult);
        }
    }

    /**
     * Passada 3: Security Focus - análise focada em segurança.
     */
    private PassResult executeSecurityPass(PullRequest pullRequest, Review review,
                                           PassResult sastResult, PassResult llmResult) {
        try {
            return executeWithTimeout(() -> securityFocusedPass.execute(pullRequest, review, sastResult, llmResult),
                    3, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Security pass failed, continuing", e);
            return PassResult.empty("Security pass failed: " + e.getMessage());
        }
    }

    /**
     * Passada 4: Code Graph Impact - análise de impacto cross-file.
     */
    private PassResult executeImpactPass(PullRequest pullRequest, Review review,
                                        PassResult sastResult, PassResult llmResult) {
        try {
            return executeWithTimeout(() -> codeGraphImpactPass.execute(pullRequest, review, sastResult, llmResult),
                    2, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Impact pass failed, continuing", e);
            return PassResult.empty("Impact pass failed: " + e.getMessage());
        }
    }

    /**
     * Executa uma passada com timeout.
     */
    private PassResult executeWithTimeout(PassSupplier supplier, long timeout, TimeUnit unit) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return supplier.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get(timeout, unit);
        } catch (Exception e) {
            throw new RuntimeException("Pass execution timeout", e);
        }
    }

    /**
     * Gera o resumo executivo do review.
     */
    private String generateExecutiveSummary(Review review, List<Issue> issues, ReviewResult result) {
        return resultSynthesizer.generateSummary(review, issues, result);
    }

    /**
     * Cria resultado fallback baseado em passadas anteriores.
     */
    private PassResult createFallbackResult(String passName, PassResult previousResult) {
        PassResult fallback = new PassResult();
        fallback.setPassName(passName + " (Fallback)");
        fallback.setSuccess(false);
        fallback.setErrorMessage("Primary analysis failed, using simplified results");
        fallback.setIssues(new ArrayList<>());
        fallback.setMetadata(Map.of("fallback", true, "timestamp", LocalDateTime.now()));
        return fallback;
    }

    /**
     * Conta issues por severidade.
     */
    private long countBySeverity(List<Issue> issues, Severity severity) {
        return issues.stream()
                .filter(i -> i.getSeverity() == severity)
                .count();
    }

    // ========== DTOs ==========

    /**
     * Resultado completo do pipeline de múltiplas passadas.
     */
    @lombok.Data
    public static class ReviewResult {
        private Long reviewId;
        private PassResult sastResult;
        private PassResult llmResult;
        private PassResult securityResult;
        private PassResult impactResult;
        private List<Issue> deduplicatedIssues;
        private List<Issue> savedIssues;
        private String executiveSummary;
        private long durationMs;
        private boolean success = true;
        private String errorMessage;

        /**
         * Coleta todos os issues de todas as passadas.
         */
        public List<Issue> collectAllIssues() {
            List<Issue> all = new ArrayList<>();
            if (sastResult != null) all.addAll(sastResult.getIssues());
            if (llmResult != null) all.addAll(llmResult.getIssues());
            if (securityResult != null) all.addAll(securityResult.getIssues());
            if (impactResult != null) all.addAll(impactResult.getIssues());
            return all;
        }

        /**
         * Retorna metadata agregado de todas as passadas.
         */
        public Map<String, Object> getAggregatedMetadata() {
            Map<String, Object> metadata = new ConcurrentHashMap<>();
            if (sastResult != null && sastResult.getMetadata() != null) {
                metadata.put("sast", sastResult.getMetadata());
            }
            if (llmResult != null && llmResult.getMetadata() != null) {
                metadata.put("llm", llmResult.getMetadata());
            }
            if (securityResult != null && securityResult.getMetadata() != null) {
                metadata.put("security", securityResult.getMetadata());
            }
            if (impactResult != null && impactResult.getMetadata() != null) {
                metadata.put("impact", impactResult.getMetadata());
            }
            return metadata;
        }
    }

    /**
     * Resultado de uma passada individual.
     */
    @lombok.Data
    public static class PassResult {
        private String passName;
        private boolean success = true;
        private List<Issue> issues = new ArrayList<>();
        private Map<String, Object> metadata = new ConcurrentHashMap<>();
        private String errorMessage;
        private long durationMs;

        public static PassResult empty(String reason) {
            PassResult result = new PassResult();
            result.setSuccess(false);
            result.setErrorMessage(reason);
            return result;
        }
    }

    @FunctionalInterface
    private interface PassSupplier {
        PassResult get() throws Exception;
    }
}
