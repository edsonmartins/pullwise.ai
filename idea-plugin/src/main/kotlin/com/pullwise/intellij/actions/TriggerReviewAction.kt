package com.pullwise.intellij.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.pullwise.intellij.api.ApiClient
import com.pullwise.intellij.service.PullwiseProjectService
import com.pullwise.intellij.settings.PullwiseSettings
import com.pullwise.intellij.ui.PullwiseStatusBarWidget
import git4idea.repo.GitRepositoryManager

class TriggerReviewAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = PullwiseSettings.instance.state

        if (settings.projectId.isBlank()) {
            Notification("Pullwise", "Pullwise",
                "Please set Project ID in Settings > Pullwise first",
                NotificationType.WARNING).notify(project)
            return
        }

        // Detect PR number from branch name
        val prNumber = detectPrNumber(project)
        if (prNumber == null) {
            Notification("Pullwise", "Pullwise",
                "Could not detect PR number from current branch",
                NotificationType.WARNING).notify(project)
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pullwise: Triggering review...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Triggering review..."
                    val review = ApiClient.instance.triggerReview(settings.projectId, prNumber)

                    // Poll for completion
                    indicator.text = "Review in progress..."
                    val maxAttempts = 60
                    for (i in 0 until maxAttempts) {
                        if (indicator.isCanceled) return

                        Thread.sleep(5000)
                        indicator.fraction = i.toDouble() / maxAttempts

                        val latest = ApiClient.instance.getLatestReview(settings.projectId) ?: continue

                        if (latest.status == "completed") {
                            val issues = ApiClient.instance.getReviewIssues(latest.id)
                            val service = PullwiseProjectService.getInstance(project)
                            service.updateIssues(issues, latest)
                            PullwiseStatusBarWidget.update(project)

                            Notification("Pullwise", "Pullwise",
                                "Review completed: ${issues.size} issues found",
                                NotificationType.INFORMATION).notify(project)
                            return
                        }

                        if (latest.status == "failed") {
                            Notification("Pullwise", "Pullwise",
                                "Review failed", NotificationType.ERROR).notify(project)
                            return
                        }

                        indicator.text = "Review in progress... (${i + 1}/$maxAttempts)"
                    }

                    Notification("Pullwise", "Pullwise",
                        "Review timed out. Use 'Show Issues' later.",
                        NotificationType.WARNING).notify(project)

                } catch (ex: Exception) {
                    Notification("Pullwise", "Pullwise",
                        "Failed to trigger review: ${ex.message}",
                        NotificationType.ERROR).notify(project)
                }
            }
        })
    }

    private fun detectPrNumber(project: com.intellij.openapi.project.Project): Int? {
        return try {
            val repos = GitRepositoryManager.getInstance(project).repositories
            if (repos.isEmpty()) return null

            val branch = repos[0].currentBranch?.name ?: return null
            val match = Regex("(\\d+)").find(branch)
            match?.groupValues?.get(1)?.toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
