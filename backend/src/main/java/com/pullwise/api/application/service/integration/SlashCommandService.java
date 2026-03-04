package com.pullwise.api.application.service.integration;

import com.pullwise.api.application.service.config.ConfigurationResolver;
import com.pullwise.api.application.service.review.PRDescriptionService;
import com.pullwise.api.domain.constants.ConfigKeys;
import com.pullwise.api.application.service.review.ReviewOrchestrator;
import com.pullwise.api.application.service.integration.AzureDevOpsService;
import com.pullwise.api.domain.enums.Platform;
import com.pullwise.api.domain.model.PullRequest;
import com.pullwise.api.domain.model.Review;
import com.pullwise.api.domain.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço para processar slash commands em comentários de PRs.
 *
 * <p>Comandos suportados:
 * <ul>
 *   <li>{@code /pullwise review} — Dispara review completo</li>
 *   <li>{@code /pullwise improve} — Gera sugestões de melhoria com auto-fix</li>
 *   <li>{@code /pullwise describe} — Gera descrição automática do PR</li>
 *   <li>{@code /pullwise config} — Mostra configuração atual</li>
 *   <li>{@code /pullwise help} — Lista comandos disponíveis</li>
 * </ul>
 *
 * <p>Funciona em GitHub (issue_comment), GitLab (Note Hook) e BitBucket (pullrequest:comment_created).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlashCommandService {

    private static final Pattern COMMAND_PATTERN = Pattern.compile(
            "^\\s*/pullwise\\s+(\\w+)(?:\\s+(.*))?$", Pattern.MULTILINE);

    private final ReviewOrchestrator reviewOrchestrator;
    private final ConfigurationResolver configurationResolver;
    private final PullRequestRepository pullRequestRepository;
    private final PRDescriptionService prDescriptionService;
    private final GitHubService gitHubService;
    private final GitLabService gitLabService;
    private final BitBucketService bitBucketService;
    private final AzureDevOpsService azureDevOpsService;

    /**
     * Verifica se o texto contém um slash command do Pullwise.
     */
    public boolean containsCommand(String text) {
        if (text == null || text.isBlank()) return false;
        return COMMAND_PATTERN.matcher(text).find();
    }

    /**
     * Extrai o comando principal do texto.
     */
    public Optional<ParsedCommand> parseCommand(String text) {
        if (text == null || text.isBlank()) return Optional.empty();

        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.find()) {
            String command = matcher.group(1).toLowerCase();
            String args = matcher.group(2);
            return Optional.of(new ParsedCommand(command, args != null ? args.trim() : ""));
        }
        return Optional.empty();
    }

    /**
     * Executa um slash command de forma assíncrona.
     */
    @Async("reviewExecutor")
    public void executeCommand(Long pullRequestId, String commentBody, String username) {
        parseCommand(commentBody).ifPresent(cmd -> {
            log.info("Executing slash command '{}' from user '{}' on PR {}",
                    cmd.command(), username, pullRequestId);

            PullRequest pr = pullRequestRepository.findById(pullRequestId).orElse(null);
            if (pr == null) {
                log.warn("PR not found: {}", pullRequestId);
                return;
            }

            String response = switch (cmd.command()) {
                case "review" -> executeReview(pr);
                case "improve" -> executeImprove(pr);
                case "describe" -> executeDescribe(pr);
                case "config" -> executeConfig(pr);
                case "help" -> executeHelp();
                default -> String.format(
                        "> **Pullwise** — Unknown command `%s`. Use `/pullwise help` for available commands.",
                        cmd.command());
            };

            postResponse(pr, response);
        });
    }

    /**
     * Dispara review completo do PR.
     */
    private String executeReview(PullRequest pr) {
        try {
            Long projectId = pr.getProject().getId();
            boolean sastEnabled = configurationResolver.isSastEnabled(projectId);
            boolean llmEnabled = configurationResolver.isLLMEnabled(projectId);
            boolean ragEnabled = configurationResolver.isRAGEnabled(projectId);

            Review review = reviewOrchestrator.createReview(
                    pr.getId(), sastEnabled, llmEnabled, ragEnabled);
            reviewOrchestrator.startReview(review.getId());

            return "> **Pullwise** — Review started. I'll post the results when it's complete.";
        } catch (Exception e) {
            log.error("Failed to execute /review for PR #{}", pr.getPrNumber(), e);
            return "> **Pullwise** — Failed to start review. Please try again later.";
        }
    }

    /**
     * Gera sugestões de melhoria com auto-fix.
     */
    private String executeImprove(PullRequest pr) {
        try {
            Long projectId = pr.getProject().getId();
            boolean sastEnabled = configurationResolver.isSastEnabled(projectId);
            boolean llmEnabled = configurationResolver.isLLMEnabled(projectId);
            boolean ragEnabled = configurationResolver.isRAGEnabled(projectId);

            Review review = reviewOrchestrator.createReview(
                    pr.getId(), sastEnabled, llmEnabled, ragEnabled);
            reviewOrchestrator.startReview(review.getId());

            return "> **Pullwise** — Improvement analysis started. Auto-fix suggestions will be included in the review.";
        } catch (Exception e) {
            log.error("Failed to execute /improve for PR #{}", pr.getPrNumber(), e);
            return "> **Pullwise** — Failed to start improvement analysis. Please try again later.";
        }
    }

    /**
     * Gera descrição automática do PR.
     */
    private String executeDescribe(PullRequest pr) {
        try {
            prDescriptionService.generateAndPostDescription(pr);
            return "> **Pullwise** — Generating PR description. I'll post it as a comment shortly.";
        } catch (Exception e) {
            log.error("Failed to execute /describe for PR #{}", pr.getPrNumber(), e);
            return "> **Pullwise** — Failed to generate PR description. Please try again later.";
        }
    }

    /**
     * Mostra a configuração atual do projeto.
     */
    private String executeConfig(PullRequest pr) {
        try {
            Long projectId = pr.getProject().getId();
            boolean sast = configurationResolver.isSastEnabled(projectId);
            boolean llm = configurationResolver.isLLMEnabled(projectId);
            boolean rag = configurationResolver.isRAGEnabled(projectId);
            String provider = configurationResolver.getLLMProvider(projectId);
            String model = configurationResolver.getLLMModel(projectId);
            String autoPost = configurationResolver.getConfig(projectId, ConfigKeys.REVIEW_AUTO_POST);

            return String.format("""
                    > **Pullwise Configuration** for `%s`
                    > | Setting | Value |
                    > |---|---|
                    > | SAST Analysis | %s |
                    > | LLM Review | %s |
                    > | RAG Context | %s |
                    > | LLM Provider | %s |
                    > | LLM Model | %s |
                    > | Auto-post Reviews | %s |""",
                    pr.getProject().getName(),
                    sast ? "Enabled" : "Disabled",
                    llm ? "Enabled" : "Disabled",
                    rag ? "Enabled" : "Disabled",
                    provider != null ? provider : "default",
                    model != null ? model : "default",
                    "true".equals(autoPost) ? "Enabled" : "Disabled");
        } catch (Exception e) {
            log.error("Failed to execute /config for PR #{}", pr.getPrNumber(), e);
            return "> **Pullwise** — Failed to retrieve configuration.";
        }
    }

    /**
     * Lista os comandos disponíveis.
     */
    private String executeHelp() {
        return """
                > **Pullwise Commands**
                > | Command | Description |
                > |---|---|
                > | `/pullwise review` | Run a full code review on this PR |
                > | `/pullwise improve` | Generate improvement suggestions with auto-fix |
                > | `/pullwise describe` | Generate an automatic PR description |
                > | `/pullwise config` | Show current project configuration |
                > | `/pullwise help` | Show this help message |""";
    }

    /**
     * Posta resposta no PR usando o serviço da plataforma correta.
     */
    private void postResponse(PullRequest pr, String response) {
        try {
            Platform platform = pr.getPlatform();

            if (platform == Platform.GITLAB) {
                gitLabService.postMergeRequestComment(
                        pr.getProject(), pr.getPrNumber(), response);
            } else if (platform == Platform.BITBUCKET) {
                bitBucketService.postReviewComment(
                        pr.getProject(), pr.getPrId(), response);
            } else if (platform == Platform.AZURE_DEVOPS) {
                azureDevOpsService.postPullRequestComment(
                        pr.getProject(), pr.getPrNumber(), response);
            } else {
                gitHubService.postPullRequestComment(
                        pr.getProject(), pr.getPrNumber(), response);
            }
        } catch (Exception e) {
            log.error("Failed to post slash command response to PR #{}", pr.getPrNumber(), e);
        }
    }

    /**
     * Comando parseado.
     */
    public record ParsedCommand(String command, String args) {}
}
