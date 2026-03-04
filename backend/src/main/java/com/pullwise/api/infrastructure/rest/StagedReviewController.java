package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.StagedReviewRequest;
import com.pullwise.api.application.service.review.StagedReviewService;
import com.pullwise.api.application.service.review.StagedReviewService.StagedIssue;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para review de staged changes (pre-commit).
 * Execução síncrona com timeout de 30s — desenvolvedor está esperando no terminal.
 */
@Slf4j
@RestController
@RequestMapping("/api/reviews/staged")
@RequiredArgsConstructor
public class StagedReviewController {

    private final StagedReviewService stagedReviewService;

    /**
     * Analisa staged changes enviados pelo CLI pre-commit hook.
     * Retorna issues encontradas de forma síncrona.
     */
    @PostMapping
    public ResponseEntity<StagedReviewResponse> reviewStaged(
            @Valid @RequestBody StagedReviewRequest request) {
        log.info("Staged review request: {} files, diff size: {} chars",
                request.filePaths() != null ? request.filePaths().size() : 0,
                request.diff().length());

        List<StagedIssue> issues = stagedReviewService.analyzeStaged(
                request.diff(),
                request.filePaths(),
                request.commitMessage()
        );

        boolean hasBlockingIssues = issues.stream()
                .anyMatch(i -> i.severity().name().equals("CRITICAL") || i.severity().name().equals("HIGH"));

        StagedReviewResponse response = new StagedReviewResponse(
                issues.size(),
                hasBlockingIssues,
                issues.stream().map(StagedReviewResponse.IssueItem::from).toList()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Response para staged review.
     */
    public record StagedReviewResponse(
            int totalIssues,
            boolean hasBlockingIssues,
            List<IssueItem> issues
    ) {
        public record IssueItem(
                String filePath,
                String severity,
                String type,
                String title,
                String description,
                Integer lineNumber,
                String suggestion
        ) {
            public static IssueItem from(StagedIssue issue) {
                return new IssueItem(
                        issue.filePath(),
                        issue.severity().name(),
                        issue.type().name(),
                        issue.title(),
                        issue.description(),
                        issue.lineNumber(),
                        issue.suggestion()
                );
            }
        }
    }
}
