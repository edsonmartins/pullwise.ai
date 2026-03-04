package com.pullwise.api.application.service.review;

import com.pullwise.api.application.service.integration.AzureDevOpsService;
import com.pullwise.api.application.service.integration.BitBucketService;
import com.pullwise.api.application.service.integration.GitHubService;
import com.pullwise.api.application.service.integration.GitLabService;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.enums.ReviewTaskType;
import com.pullwise.api.domain.model.PullRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço para geração automática de descrição de PR usando LLM.
 *
 * <p>Analisa os diffs do PR e gera uma descrição estruturada contendo:
 * <ul>
 *   <li>Resumo das mudanças (o que e por quê)</li>
 *   <li>Tipo de mudança (feature, bugfix, refactor, etc.)</li>
 *   <li>Lista de arquivos modificados com breve descrição</li>
 *   <li>Impacto e riscos potenciais</li>
 * </ul>
 *
 * <p>Integrado com o slash command {@code /pullwise describe}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PRDescriptionService {

    private final MultiModelLLMRouter llmRouter;
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private final BitBucketService bitBucketService;
    private final AzureDevOpsService azureDevOpsService;

    /**
     * Gera e posta a descrição do PR de forma assíncrona.
     */
    @Async("reviewExecutor")
    public void generateAndPostDescription(PullRequest pr) {
        try {
            // 1. Buscar diffs
            List<GitHubService.FileDiff> diffs = fetchDiffs(pr);

            if (diffs.isEmpty()) {
                log.warn("No diffs found for PR #{}, skipping description generation", pr.getPrNumber());
                return;
            }

            // 2. Gerar descrição via LLM
            String description = generateDescription(pr, diffs);

            if (description == null || description.isBlank()) {
                log.warn("LLM returned empty description for PR #{}", pr.getPrNumber());
                return;
            }

            // 3. Postar como comentário no PR (não atualiza a descrição do PR diretamente)
            postDescription(pr, description);

            log.info("Generated and posted PR description for PR #{}", pr.getPrNumber());

        } catch (Exception e) {
            log.error("Failed to generate PR description for PR #{}: {}", pr.getPrNumber(), e.getMessage());
        }
    }

    /**
     * Gera a descrição do PR baseada nos diffs.
     */
    public String generateDescription(PullRequest pr, List<GitHubService.FileDiff> diffs) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(pr, diffs);

        try {
            var response = llmRouter.execute(
                    ReviewTaskType.BUG_DETECTION, // Reutilizar task type existente
                    systemPrompt,
                    userPrompt
            );

            return response.content();
        } catch (Exception e) {
            log.error("LLM failed to generate description: {}", e.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt() {
        return """
            You are a technical writer that generates clear, concise pull request descriptions.

            Based on the code diffs provided, generate a PR description with the following structure:

            ## Summary
            A 2-3 sentence summary of what this PR does and why.

            ## Changes
            - Bullet points describing each significant change

            ## Type
            One of: Feature, Bug Fix, Refactor, Documentation, Test, Configuration, Dependencies

            ## Files Changed
            Brief description of key files and what changed in each.

            ## Potential Risks
            Any risks or areas that need careful review (or "None identified" if low risk).

            Keep the description professional and focused. Do not speculate about things not evident from the code.
            Write in the same language as the code comments (default to English).
            """;
    }

    private String buildUserPrompt(PullRequest pr, List<GitHubService.FileDiff> diffs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a PR description for the following changes:\n\n");

        if (pr.getTitle() != null) {
            sb.append("**PR Title**: ").append(pr.getTitle()).append("\n");
        }
        if (pr.getSourceBranch() != null) {
            sb.append("**Branch**: ").append(pr.getSourceBranch())
                    .append(" → ").append(pr.getTargetBranch()).append("\n\n");
        }

        // File summary
        sb.append("**Files** (").append(diffs.size()).append(" changed):\n");
        int totalAdded = 0, totalRemoved = 0;
        for (GitHubService.FileDiff diff : diffs) {
            sb.append(String.format("- `%s` (+%d/-%d) [%s]\n",
                    diff.filename(), diff.additions(), diff.deletions(), diff.status()));
            totalAdded += diff.additions();
            totalRemoved += diff.deletions();
        }
        sb.append(String.format("\nTotal: +%d/-%d lines\n\n", totalAdded, totalRemoved));

        // Diffs (limited to avoid token overflow)
        sb.append("**Diffs**:\n");
        int diffsIncluded = 0;
        for (GitHubService.FileDiff diff : diffs) {
            if (diff.patch() != null && !diff.patch().isBlank() && diffsIncluded < 15) {
                sb.append("\n### ").append(diff.filename()).append("\n");
                sb.append("```diff\n");
                String patch = diff.patch().length() > 2000
                        ? diff.patch().substring(0, 2000) + "\n... (truncated)"
                        : diff.patch();
                sb.append(patch).append("\n```\n");
                diffsIncluded++;
            }
        }

        return sb.toString();
    }

    private List<GitHubService.FileDiff> fetchDiffs(PullRequest pr) {
        if (pr.getPlatform() == Platform.GITLAB) {
            return gitLabService.fetchMergeRequestDiffs(pr.getProject(), pr.getPrNumber());
        } else if (pr.getPlatform() == Platform.BITBUCKET) {
            return bitBucketService.fetchPullRequestDiffs(pr.getProject(), pr.getPrNumber());
        } else if (pr.getPlatform() == Platform.AZURE_DEVOPS) {
            return azureDevOpsService.fetchPullRequestDiffs(pr.getProject(), pr.getPrNumber());
        } else {
            return gitHubService.fetchPullRequestDiffs(pr.getProject(), pr.getPrNumber());
        }
    }

    private void postDescription(PullRequest pr, String description) {
        String comment = "## 📝 Pullwise — Auto-Generated PR Description\n\n" + description +
                "\n\n---\n*Generated by [Pullwise](https://pullwise.ai) via `/pullwise describe`*";

        if (pr.getPlatform() == Platform.GITLAB) {
            gitLabService.postMergeRequestComment(pr.getProject(), pr.getPrNumber(), comment);
        } else if (pr.getPlatform() == Platform.BITBUCKET) {
            bitBucketService.postReviewComment(pr.getProject(), pr.getPrId(), comment);
        } else if (pr.getPlatform() == Platform.AZURE_DEVOPS) {
            azureDevOpsService.postPullRequestComment(pr.getProject(), pr.getPrNumber(), comment);
        } else {
            gitHubService.postPullRequestComment(pr.getProject(), pr.getPrNumber(), comment);
        }
    }
}
