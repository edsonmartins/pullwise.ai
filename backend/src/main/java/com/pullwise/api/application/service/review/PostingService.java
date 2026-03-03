package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.GitLabService;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.PullRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço para postar comentários de review no PR.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostingService {

    private final GitHubService gitHubService;
    private final BitBucketService bitBucketService;
    private final GitLabService gitLabService;

    @Value("${pullwise.review.post-as-comment:true}")
    private boolean postAsComment;

    @Value("${pullwise.review.include-summary:true}")
    private boolean includeSummary;

    /**
     * Posta o comentário do review no PR.
     */
    public String postReviewComment(PullRequest pr, List<Issue> issues) {
        if (!postAsComment) {
            log.info("Posting disabled, skipping comment for PR {}", pr.getPrNumber());
            return null;
        }

        String comment = formatReviewComment(issues);

        try {
            String commentId;
            if (pr.getPlatform() == Platform.BITBUCKET) {
                commentId = bitBucketService.postReviewComment(
                        pr.getProject(),
                        (long) pr.getPrNumber(),
                        comment
                );
            } else if (pr.getPlatform() == Platform.GITLAB) {
                commentId = gitLabService.postMergeRequestComment(
                        pr.getProject(),
                        pr.getPrNumber(),
                        comment
                );
            } else {
                commentId = gitHubService.postPullRequestComment(
                        pr.getProject(),
                        pr.getPrNumber(),
                        comment
                );
            }

            log.info("Posted review comment {} for PR {}", commentId, pr.getPrNumber());
            return commentId;

        } catch (Exception e) {
            log.error("Failed to post comment for PR {}", pr.getPrNumber(), e);
            return null;
        }
    }

    /**
     * Formata o comentário do review em Markdown.
     */
    private String formatReviewComment(List<Issue> issues) {
        StringBuilder sb = new StringBuilder();

        sb.append("## 🤖 Pullwise Review Report\n\n");

        if (includeSummary) {
            sb.append(formatSummary(issues));
        }

        sb.append(formatIssuesBySeverity(issues));

        sb.append("\n---\n");
        sb.append("*Powered by [Pullwise.ai](https://pullwise.ai)*");

        return sb.toString();
    }

    /**
     * Formata o resumo do review.
     */
    private String formatSummary(List<Issue> issues) {
        Map<com.pullwise.api.domain.enums.Severity, Long> counts = issues.stream()
                .collect(Collectors.groupingBy(
                        Issue::getSeverity,
                        Collectors.counting()
                ));

        long critical = counts.getOrDefault(com.pullwise.api.domain.enums.Severity.CRITICAL, 0L);
        long high = counts.getOrDefault(com.pullwise.api.domain.enums.Severity.HIGH, 0L);
        long medium = counts.getOrDefault(com.pullwise.api.domain.enums.Severity.MEDIUM, 0L);
        long low = counts.getOrDefault(com.pullwise.api.domain.enums.Severity.LOW, 0L);
        long info = counts.getOrDefault(com.pullwise.api.domain.enums.Severity.INFO, 0L);

        StringBuilder sb = new StringBuilder();
        sb.append("### Summary\n\n");
        sb.append("| Severity | Count |\n");
        sb.append("|----------|-------|\n");
        sb.append(String.format("| 🔴 Critical | %d |\n", critical));
        sb.append(String.format("| 🟠 High | %d |\n", high));
        sb.append(String.format("| 🟡 Medium | %d |\n", medium));
        sb.append(String.format("| 🟢 Low | %d |\n", low));
        sb.append(String.format("| 🔵 Info | %d |\n", info));
        sb.append(String.format("| **Total** | **%d** |\n\n", issues.size()));

        return sb.toString();
    }

    /**
     * Formata as issues agrupadas por severidade.
     */
    private String formatIssuesBySeverity(List<Issue> issues) {
        StringBuilder sb = new StringBuilder();

        Map<com.pullwise.api.domain.enums.Severity, List<Issue>> grouped = issues.stream()
                .collect(Collectors.groupingBy(Issue::getSeverity));

        for (com.pullwise.api.domain.enums.Severity severity : List.of(
                com.pullwise.api.domain.enums.Severity.CRITICAL,
                com.pullwise.api.domain.enums.Severity.HIGH,
                com.pullwise.api.domain.enums.Severity.MEDIUM,
                com.pullwise.api.domain.enums.Severity.LOW,
                com.pullwise.api.domain.enums.Severity.INFO
        )) {
            List<Issue> severityIssues = grouped.get(severity);
            if (severityIssues == null || severityIssues.isEmpty()) {
                continue;
            }

            sb.append(formatSeveritySection(severity, severityIssues));
        }

        return sb.toString();
    }

    /**
     * Formata uma seção de severidade.
     */
    private String formatSeveritySection(com.pullwise.api.domain.enums.Severity severity, List<Issue> issues) {
        StringBuilder sb = new StringBuilder();

        String emoji = switch (severity) {
            case CRITICAL -> "🔴";
            case HIGH -> "🟠";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
            case INFO -> "🔵";
        };

        sb.append(String.format("### %s %s (%d)\n\n", emoji, severity.getLabel(), issues.size()));

        for (Issue issue : issues) {
            sb.append(formatIssue(issue));
        }

        return sb.toString();
    }

    /**
     * Formata uma issue individual.
     */
    private String formatIssue(Issue issue) {
        StringBuilder sb = new StringBuilder();

        String location = issue.hasLocation() ? issue.getLocationString() : "General";
        String source = issue.getSource().getDisplayName();

        sb.append(String.format("#### **%s** [%s]\n", issue.getTitle(), source));
        sb.append(String.format("> %s\n\n", issue.getDescription()));

        if (issue.getSuggestion() != null && !issue.getSuggestion().isBlank()) {
            sb.append(String.format("**💡 Suggestion:** %s\n\n", issue.getSuggestion()));
        }

        if (issue.hasLocation()) {
            sb.append(String.format("`%s`\n\n", location));
        }

        return sb.toString();
    }
}
