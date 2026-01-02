package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.service.autofix.AutoFixService;
import com.pullwise.api.application.service.autofix.GitService;
import com.pullwise.api.application.service.autofix.dto.*;
import com.pullwise.api.domain.model.FixSuggestion;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.repository.FixSuggestionRepository;
import com.pullwise.api.domain.repository.IssueRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Controller REST para Auto-Fix.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/autofix/generate - Gera sugestão de correção</li>
 *   <li>POST /api/autofix/apply - Aplica uma sugestão</li>
 *   <li>POST /api/autofix/generate-and-apply - Gera e aplica em uma operação</li>
 *   <li>POST /api/autofix/{id}/approve - Aprova uma sugestão</li>
 *   <li>POST /api/autofix/{id}/reject - Rejeita uma sugestão</li>
 *   <li>GET /api/autofix/reviews/{reviewId} - Lista sugestões de um review</li>
 *   <li>GET /api/autofix/issues/{issueId} - Lista sugestões de uma issue</li>
 *   <li>GET /api/autofix/{id} - Busca uma sugestão específica</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/autofix")
@RequiredArgsConstructor
public class AutoFixController {

    private final AutoFixService autoFixService;
    private final FixSuggestionRepository fixSuggestionRepository;
    private final IssueRepository issueRepository;

    /**
     * Gera uma sugestão de correção para uma issue.
     *
     * @param request Request com issue e conteúdo do arquivo
     * @return Resultado da geração
     */
    @PostMapping("/generate")
    public ResponseEntity<FixGenerationResponse> generateFix(
            @Valid @RequestBody FixGenerateRequest request) {

        Issue issue = issueRepository.findById(request.issueId())
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + request.issueId()));

        FixGenerationRequest genRequest = new FixGenerationRequest(
                issue,
                request.fileContent(),
                request.branchName(),
                request.timeoutSeconds(),
                request.preferredModel()
        );

        FixGenerationResult result = autoFixService.generateFix(genRequest);

        return ResponseEntity.ok(FixGenerationResponse.from(result));
    }

    /**
     * Aplica uma sugestão de correção.
     *
     * @param request Request de aplicação
     * @return Resultado da aplicação
     */
    @PostMapping("/apply")
    public ResponseEntity<FixApplicationResponse> applyFix(
            @Valid @RequestBody FixApplyRequest request) {

        FixSuggestion suggestion = fixSuggestionRepository.findById(request.suggestionId())
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found: " + request.suggestionId()));

        FixApplicationRequest applyRequest = new FixApplicationRequest(
                suggestion,
                request.appliedBy() != null ? request.appliedBy() : "api-user",
                request.createCommit() != null ? request.createCommit() : true,
                request.push() != null ? request.push() : false,
                request.authToken()
        );

        FixApplicationResult result = autoFixService.applyFix(applyRequest);

        return ResponseEntity.ok(FixApplicationResponse.from(result));
    }

    /**
     * Gera e aplica uma correção em uma única operação (para auto-fix de alta confiança).
     *
     * @param request Request com issue e conteúdo
     * @return Resultado da aplicação
     */
    @PostMapping("/generate-and-apply")
    public ResponseEntity<FixApplicationResponse> generateAndApply(
            @Valid @RequestBody FixGenerateAndApplyRequest request) {

        Issue issue = issueRepository.findById(request.issueId())
                .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + request.issueId()));

        FixApplicationResult result = autoFixService.generateAndApply(
                issue,
                request.fileContent(),
                request.authToken()
        );

        if (result.success()) {
            return ResponseEntity.ok(FixApplicationResponse.from(result));
        } else {
            return ResponseEntity.unprocessableEntity().body(FixApplicationResponse.from(result));
        }
    }

    /**
     * Aprova uma sugestão de correção.
     *
     * @param id ID da sugestão
     * @param request Request com usuário que aprovou
     * @return 200 se aprovada, 404 se não encontrada
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<FixSuggestionDTO> approveSuggestion(
            @PathVariable Long id,
            @RequestBody FixApprovalRequest request) {

        boolean approved = autoFixService.approveSuggestion(id, request.reviewedBy());

        if (!approved) {
            return ResponseEntity.notFound().build();
        }

        FixSuggestion suggestion = fixSuggestionRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(FixSuggestionDTO.from(suggestion));
    }

    /**
     * Rejeita uma sugestão de correção.
     *
     * @param id ID da sugestão
     * @param request Request com usuário e motivo
     * @return 200 se rejeitada, 404 se não encontrada
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<FixSuggestionDTO> rejectSuggestion(
            @PathVariable Long id,
            @RequestBody FixRejectionRequest request) {

        boolean rejected = autoFixService.rejectSuggestion(
                id,
                request.reviewedBy(),
                request.reason()
        );

        if (!rejected) {
            return ResponseEntity.notFound().build();
        }

        FixSuggestion suggestion = fixSuggestionRepository.findById(id).orElseThrow();
        return ResponseEntity.ok(FixSuggestionDTO.from(suggestion));
    }

    /**
     * Lista sugestões de correção de um review.
     *
     * @param reviewId ID do review
     * @return Lista de sugestões
     */
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<List<FixSuggestionDTO>> listByReview(
            @PathVariable Long reviewId,
            @RequestParam(required = false) String status) {

        List<FixSuggestion> suggestions;

        if (status != null) {
            try {
                com.pullwise.api.domain.enums.FixStatus fixStatus =
                        com.pullwise.api.domain.enums.FixStatus.valueOf(status.toUpperCase());
                suggestions = fixSuggestionRepository.findByReviewIdAndStatus(reviewId, fixStatus);
            } catch (IllegalArgumentException e) {
                suggestions = fixSuggestionRepository.findByReviewIdOrderByCreatedAtDesc(reviewId);
            }
        } else {
            suggestions = fixSuggestionRepository.findByReviewIdOrderByCreatedAtDesc(reviewId);
        }

        List<FixSuggestionDTO> dtos = suggestions.stream()
                .map(FixSuggestionDTO::from)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Lista sugestões de correção de uma issue.
     *
     * @param issueId ID da issue
     * @return Lista de sugestões
     */
    @GetMapping("/issues/{issueId}")
    public ResponseEntity<List<FixSuggestionDTO>> listByIssue(
            @PathVariable Long issueId) {

        List<FixSuggestion> suggestions = fixSuggestionRepository.findByIssueId(issueId);

        List<FixSuggestionDTO> dtos = suggestions.stream()
                .map(FixSuggestionDTO::from)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Lista sugestões prontas para aplicar de um review.
     *
     * @param reviewId ID do review
     * @return Lista de sugestões
     */
    @GetMapping("/reviews/{reviewId}/ready")
    public ResponseEntity<List<FixSuggestionDTO>> listReadyToApply(
            @PathVariable Long reviewId) {

        List<FixSuggestion> suggestions = autoFixService.getReadyToApply(reviewId);

        List<FixSuggestionDTO> dtos = suggestions.stream()
                .map(FixSuggestionDTO::from)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Busca uma sugestão específica.
     *
     * @param id ID da sugestão
     * @return Sugestão ou 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<FixSuggestionDTO> getSuggestion(@PathVariable Long id) {
        return fixSuggestionRepository.findById(id)
                .map(suggestion -> ResponseEntity.ok(FixSuggestionDTO.from(suggestion)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Valida uma correção gerada.
     *
     * @param request Request com código a validar
     * @return Resultado da validação
     */
    @PostMapping("/validate")
    public ResponseEntity<CodeValidationResponse> validateFix(
            @RequestBody FixValidateRequest request) {

        CodeValidationResult result = autoFixService.validateFix(
                request.originalCode(),
                request.fixedCode(),
                request.language()
        );

        return ResponseEntity.ok(CodeValidationResponse.from(result));
    }

    // ========== DTOs ==========

    /**
     * Request para gerar correção.
     */
    public record FixGenerateRequest(
            Long issueId,
            String fileContent,
            String branchName,
            Integer timeoutSeconds,
            String preferredModel
    ) {}

    /**
     * Request para aplicar correção.
     */
    public record FixApplyRequest(
            Long suggestionId,
            String appliedBy,
            Boolean createCommit,
            Boolean push,
            String authToken
    ) {}

    /**
     * Request para gerar e aplicar.
     */
    public record FixGenerateAndApplyRequest(
            Long issueId,
            String fileContent,
            String authToken
    ) {}

    /**
     * Request para aprovar.
     */
    public record FixApprovalRequest(
            String reviewedBy
    ) {}

    /**
     * Request para rejeitar.
     */
    public record FixRejectionRequest(
            String reviewedBy,
            String reason
    ) {}

    /**
     * Request para validar.
     */
    public record FixValidateRequest(
            String originalCode,
            String fixedCode,
            String language
    ) {}

    /**
     * Response de geração de correção.
     */
    public record FixGenerationResponse(
            boolean success,
            String fixedCode,
            String originalCode,
            String explanation,
            String confidence,
            boolean canAutoApply,
            List<String> blockingReasons,
            String modelUsed,
            int inputTokens,
            int outputTokens,
            double cost
    ) {
        public static FixGenerationResponse from(FixGenerationResult result) {
            return new FixGenerationResponse(
                    result.fixedCode() != null,
                    result.fixedCode(),
                    result.originalCode(),
                    result.explanation(),
                    result.confidence().name(),
                    result.canAutoApply(),
                    result.blockingReasons(),
                    result.modelUsed(),
                    result.inputTokens(),
                    result.outputTokens(),
                    result.cost()
            );
        }
    }

    /**
     * Response de aplicação de correção.
     */
    public record FixApplicationResponse(
            boolean success,
            String commitHash,
            String branchName,
            List<String> modifiedFiles,
            String errorMessage,
            String appliedAt
    ) {
        public static FixApplicationResponse from(FixApplicationResult result) {
            return new FixApplicationResponse(
                    result.success(),
                    result.commitHash(),
                    result.branchName(),
                    result.modifiedFiles(),
                    result.errorMessage(),
                    result.appliedAt() != null ? result.appliedAt().toString() : null
            );
        }
    }

    /**
     * Response de validação de código.
     */
    public record CodeValidationResponse(
            boolean valid,
            List<String> issues,
            String maxSeverity
    ) {
        public static CodeValidationResponse from(CodeValidationResult result) {
            return new CodeValidationResponse(
                    result.valid(),
                    result.issues(),
                    result.maxSeverity().name()
            );
        }
    }

    /**
     * DTO de sugestão de correção.
     */
    public record FixSuggestionDTO(
            Long id,
            Long reviewId,
            Long issueId,
            String status,
            String confidence,
            String summary,
            String diff,
            String filePath,
            Integer startLine,
            Integer endLine,
            String branchName,
            String commitHash,
            String reviewedBy,
            String reviewedAt,
            String appliedAt,
            String errorMessage,
            String modelUsed,
            Integer inputTokens,
            Integer outputTokens,
            Double estimatedCost,
            String createdAt,
            String updatedAt
    ) {
        public static FixSuggestionDTO from(FixSuggestion s) {
            return new FixSuggestionDTO(
                    s.getId(),
                    s.getReview() != null ? s.getReview().getId() : null,
                    s.getIssue() != null ? s.getIssue().getId() : null,
                    s.getStatus().name(),
                    s.getConfidence().name(),
                    s.getSummary(),
                    s.getDiff(),
                    s.getFilePath(),
                    s.getStartLine(),
                    s.getEndLine(),
                    s.getBranchName(),
                    s.getAppliedCommitHash(),
                    s.getReviewedBy(),
                    s.getReviewedAt() != null ? s.getReviewedAt().toString() : null,
                    s.getAppliedAt() != null ? s.getAppliedAt().toString() : null,
                    s.getErrorMessage(),
                    s.getModelUsed(),
                    s.getInputTokens(),
                    s.getOutputTokens(),
                    s.getEstimatedCost(),
                    s.getCreatedAt() != null ? s.getCreatedAt().toString() : null,
                    s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null
            );
        }
    }
}
