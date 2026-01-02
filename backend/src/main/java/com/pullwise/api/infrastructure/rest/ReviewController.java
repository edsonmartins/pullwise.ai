package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.CreateReviewRequest;
import com.pullwise.api.application.dto.response.*;
import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.repository.IssueRepository;
import com.pullwise.api.domain.repository.ReviewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

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
    private final ReviewOrchestrator reviewOrchestrator;

    /**
     * Lista reviews de um projeto.
     */
    @GetMapping
    public ResponseEntity<List<ReviewDTO>> listReviews(
            @RequestParam(required = false) Long projectId) {
        List<Review> reviews = projectId != null
                ? reviewRepository.findByProjectId(projectId)
                : reviewRepository.findAll();

        List<ReviewDTO> dtos = reviews.stream()
                .map(review -> {
                    List<Issue> issues = issueRepository.findByReviewId(review.getId());
                    ReviewDTO.ReviewStats stats = new ReviewDTO.ReviewStats(
                            issues.size(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.CRITICAL).count(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.HIGH).count(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.MEDIUM).count(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.LOW).count(),
                            (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.INFO).count()
                    );
                    return ReviewDTO.from(review, stats);
                })
                .toList();

        return ResponseEntity.ok(dtos);
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

        List<Issue> issues = issueRepository.findByReviewId(id);
        ReviewDTO.ReviewStats stats = new ReviewDTO.ReviewStats(
                issues.size(),
                (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.CRITICAL).count(),
                (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.HIGH).count(),
                (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.MEDIUM).count(),
                (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.LOW).count(),
                (int) issues.stream().filter(i -> i.getSeverity() == com.pullwise.api.domain.enums.Severity.INFO).count()
        );

        return ResponseEntity.ok(ReviewDTO.from(review, stats));
    }

    /**
     * Cria um novo review.
     */
    @PostMapping
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
}
