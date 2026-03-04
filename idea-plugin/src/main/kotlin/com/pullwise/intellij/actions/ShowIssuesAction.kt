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

class ShowIssuesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = PullwiseSettings.instance.state

        if (settings.projectId.isBlank()) {
            Notification("Pullwise", "Pullwise",
                "Please set Project ID in Settings > Pullwise first",
                NotificationType.WARNING).notify(project)
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pullwise: Fetching issues...") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Fetching latest review..."
                    val review = ApiClient.instance.getLatestReview(settings.projectId)

                    if (review == null) {
                        Notification("Pullwise", "Pullwise",
                            "No reviews found for this project",
                            NotificationType.INFORMATION).notify(project)
                        return
                    }

                    if (review.status != "completed") {
                        Notification("Pullwise", "Pullwise",
                            "Latest review is ${review.status}. Waiting for completion.",
                            NotificationType.INFORMATION).notify(project)
                        return
                    }

                    indicator.text = "Fetching issues..."
                    val issues = ApiClient.instance.getReviewIssues(review.id)

                    val service = PullwiseProjectService.getInstance(project)
                    service.updateIssues(issues, review)
                    PullwiseStatusBarWidget.update(project)

                    Notification("Pullwise", "Pullwise",
                        "${issues.size} issues loaded from review of PR #${review.prNumber}",
                        NotificationType.INFORMATION).notify(project)

                } catch (ex: Exception) {
                    Notification("Pullwise", "Pullwise",
                        "Failed to fetch issues: ${ex.message}",
                        NotificationType.ERROR).notify(project)
                }
            }
        })
    }
}
