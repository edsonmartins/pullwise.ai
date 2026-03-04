package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.CreateReviewRequest;
import com.pullwise.api.application.dto.response.*;
import com.pullwise.api.application.service.attestation.AttestationService;
import com.pullwise.api.application.service.audit.Auditable;
import com.pullwise.api.application.service.auth.AuthorizationService;
import com.pullwise.api.application.service.review.CoverageTrackingService;
import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.domain.enums.Severity;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.repository.IssueRepository;
import com.pullwise.api.domain.repository.ProjectRepository;
import com.pullwise.api.domain.repository.ReviewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST para gerenciamento de Reviews.
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final ReviewOrchestrator reviewOrchestrator;
    private final CoverageTrackingService coverageTrackingService;
    private final AttestationService attestationService;
    private final AuthorizationService authorizationService;

    /**
     * Lista reviews de um projeto.
     */
    @GetMapping
    public ResponseEntity<List<ReviewDTO>> listReviews(
            @RequestParam(required = false) Long projectId,
            Principal principal) {
        Long userId = authorizationService.getUserId(principal);

        List<Review> reviews;
        if (projectId != null) {
            authorizationService.requireProjectAccess(userId, projectId);
            reviews = reviewRepository.findByProjectId(projectId);
        } else {
            // Filter to only reviews of projects accessible to the user
            List<Long> orgIds = authorizationService.getUserOrganizationIds(userId);
            List<Long> projectIds = orgIds.stream()
                    .flatMap(orgId -> projectRepository.findActiveByOrganizationId(orgId).stream())
                    .map(p -> p.getId())
                    .toList();
            reviews = reviewRepository.findByProjectIdIn(projectIds);
        }

        // Batch query to get severity counts in a single DB round-trip (fixes N+1)
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, ReviewDTO.ReviewStats> statsMap = buildStatsMap(reviewIds);

        List<ReviewDTO> dtos = reviews.stream()
                .map(review -> {
                    ReviewDTO.ReviewStats stats = statsMap.getOrDefault(review.getId(),
                            new ReviewDTO.ReviewStats(0, 0, 0, 0, 0, 0));
                    return ReviewDTO.from(review, stats);
                })
                .toList();

        return ResponseEntity.ok(dtos);
    }

    /**
     * Builds a map of review ID -> ReviewStats using a single batch query.
     */
    private Map<Long, ReviewDTO.ReviewStats> buildStatsMap(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) return Map.of();

        Map<Long, Map<Severity, Integer>> countsByReview = new HashMap<>();
        for (Object[] row : issueRepository.countBySeverityGroupedByReviewId(reviewIds)) {
            Long reviewId = (Long) row[0];
            Severity severity = (Severity) row[1];
            int count = ((Long) row[2]).intValue();
            countsByReview.computeIfAbsent(reviewId, k -> new HashMap<>()).put(severity, count);
        }

        Map<Long, ReviewDTO.ReviewStats> result = new HashMap<>();
        for (Map.Entry<Long, Map<Severity, Integer>> entry : countsByReview.entrySet()) {
            Map<Severity, Integer> counts = entry.getValue();
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            result.put(entry.getKey(), new ReviewDTO.ReviewStats(
                    total,
                    counts.getOrDefault(Severity.CRITICAL, 0),
                    counts.getOrDefault(Severity.HIGH, 0),
                    counts.getOrDefault(Severity.MEDIUM, 0),
                    counts.getOrDefault(Severity.LOW, 0),
                    counts.getOrDefault(Severity.INFO, 0)
            ));
        }
        return result;
    }

    /**
     * Busca um review por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReview(@PathVariable Long id) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        Map<Long, ReviewDTO.ReviewStats> statsMap = buildStatsMap(List.of(id));
        ReviewDTO.ReviewStats stats = statsMap.getOrDefault(id,
                new ReviewDTO.ReviewStats(0, 0, 0, 0, 0, 0));

        return ResponseEntity.ok(ReviewDTO.from(review, stats));
    }

    /**
     * Cria um novo review.
     */
    @PostMapping
    @Auditable(action = "CREATE_REVIEW", entityType = "Review")
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody CreateReviewRequest request) {
        Review review = reviewOrchestrator.createReview(
                request.pullRequestId(),
                request.sastEnabled(),
                request.llmEnabled(),
                request.ragEnabled()
        );

        // Iniciar processamento assíncrono
        reviewOrchestrator.startReview(review.getId());

        ReviewDTO dto = ReviewDTO.from(review, new ReviewDTO.ReviewStats(0, 0, 0, 0, 0, 0));

        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(review.getId())
                        .toUri())
                .body(dto);
    }

    /**
     * Lista issues de um review.
     */
    @GetMapping("/{id}/issues")
    public ResponseEntity<List<IssueDTO>> getReviewIssues(@PathVariable Long id) {
        List<Issue> issues = issueRepository.findByReviewId(id);
        List<IssueDTO> dtos = issues.stream()
                .map(IssueDTO::from)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    /**
     * Cancela um review.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReview(@PathVariable Long id) {
        try {
            reviewOrchestrator.cancelReview(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Marca uma issue como falso positivo.
     */
    @PostMapping("/issues/{issueId}/false-positive")
    public ResponseEntity<Void> markAsFalsePositive(@PathVariable Long issueId) {
        return issueRepository.findById(issueId)
                .map(issue -> {
                    issue.setIsFalsePositive(true);
                    issueRepository.save(issue);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marca uma issue como acknowledged (reconhecida pelo desenvolvedor).
     */
    @PostMapping("/issues/{issueId}/acknowledge")
    public ResponseEntity<Void> acknowledgeIssue(@PathVariable Long issueId) {
        return issueRepository.findById(issueId)
                .map(issue -> {
                    issue.setAcknowledged(true);
                    issueRepository.save(issue);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retorna a cobertura de review por arquivo.
     */
    @GetMapping("/{id}/coverage")
    public ResponseEntity<CoverageTrackingService.CoverageReport> getReviewCoverage(@PathVariable Long id) {
        if (!reviewRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(coverageTrackingService.getCoverageReport(id));
    }

    /**
     * Retorna a attestation de um review.
     */
    @GetMapping("/{id}/attestation")
    public ResponseEntity<AttestationService.AttestationDTO> getAttestation(@PathVariable Long id) {
        return attestationService.getAttestation(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Verifica a integridade da attestation de um review.
     */
    @PostMapping("/{id}/attestation/verify")
    public ResponseEntity<AttestationService.VerificationResult> verifyAttestation(@PathVariable Long id) {
        return ResponseEntity.ok(attestationService.verify(id));
    }
}
