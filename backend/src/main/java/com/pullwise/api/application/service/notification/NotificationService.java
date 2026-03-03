package com.pullwise.api.application.service.notification;

import com.pullwise.api.application.service.integration.SlackService;
import com.pullwise.api.application.service.integration.TeamsService;
import com.pullwise.api.domain.model.Issue;
import com.pullwise.api.domain.model.Review;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço de notificações que orquestra envio para múltiplos canais.
 *
 * <p>Atualmente suporta:
 * <ul>
 *   <li>Slack (via Incoming Webhooks)</li>
 *   <li>Microsoft Teams (via Incoming Webhooks com Adaptive Cards)</li>
 * </ul>
 *
 * <p>As notificações são enviadas de forma assíncrona para não bloquear o pipeline de review.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SlackService slackService;
    private final TeamsService teamsService;

    /**
     * Envia notificação de review completado para todos os canais configurados.
     * Executa de forma assíncrona.
     */
    @Async
    public void notifyReviewCompleted(Review review, List<Issue> issues) {
        Long projectId = review.getPullRequest().getProject().getId();
        log.debug("Sending review completion notifications for review {} (project {})",
                review.getId(), projectId);

        // Slack
        if (slackService.isConfiguredForProject(projectId)) {
            try {
                boolean sent = slackService.sendReviewNotification(review, issues);
                if (sent) {
                    log.info("Slack notification sent for review {}", review.getId());
                }
            } catch (Exception e) {
                log.error("Slack notification failed for review {}: {}", review.getId(), e.getMessage());
            }
        }

        // Microsoft Teams
        if (teamsService.isConfiguredForProject(projectId)) {
            try {
                boolean sent = teamsService.sendReviewNotification(review, issues);
                if (sent) {
                    log.info("Teams notification sent for review {}", review.getId());
                }
            } catch (Exception e) {
                log.error("Teams notification failed for review {}: {}", review.getId(), e.getMessage());
            }
        }
    }
}
