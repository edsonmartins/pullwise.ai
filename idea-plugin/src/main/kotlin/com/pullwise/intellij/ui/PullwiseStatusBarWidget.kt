package com.pullwise.intellij.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.Consumer
import com.pullwise.intellij.model.Review
import com.pullwise.intellij.service.PullwiseProjectService
import java.awt.event.MouseEvent

class PullwiseStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {

    private var statusBar: StatusBar? = null

    override fun ID(): String = "PullwiseStatusBar"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun dispose() {
        statusBar = null
    }

    override fun getText(): String {
        val service = PullwiseProjectService.getInstance(project)
        val review = service.latestReview ?: return "Pullwise"

        val stats = review.stats ?: return "Pullwise: ${review.status}"

        val parts = mutableListOf<String>()
        if (stats.criticalCount > 0) parts.add("${stats.criticalCount}C")
        if (stats.highCount > 0) parts.add("${stats.highCount}H")
        if (stats.mediumCount > 0) parts.add("${stats.mediumCount}M")
        if (stats.lowCount > 0) parts.add("${stats.lowCount}L")

        val issueText = if (parts.isNotEmpty()) parts.joinToString(" ") else "No issues"
        return "Pullwise: $issueText"
    }

    override fun getTooltipText(): String {
        val service = PullwiseProjectService.getInstance(project)
        val review = service.latestReview ?: return "Pullwise Code Review\nNo review loaded"

        val stats = review.stats
        return buildString {
            appendLine("Pullwise Code Review")
            appendLine("PR #${review.prNumber}: ${review.prTitle}")
            appendLine("Status: ${review.status}")
            if (stats != null) {
                append("Critical: ${stats.criticalCount} | High: ${stats.highCount} | ")
                append("Medium: ${stats.mediumCount} | Low: ${stats.lowCount}")
            }
        }
    }

    override fun getAlignment(): Float = 0f

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer {
            val action = ActionManager.getInstance().getAction("Pullwise.ShowIssues")
            action?.actionPerformed(
                com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
                    action,
                    null,
                    "StatusBar",
                    DataContext.EMPTY_CONTEXT
                )
            )
        }
    }

    fun refresh() {
        statusBar?.updateWidget(ID())
    }

    companion object {
        fun update(project: Project) {
            val statusBar = WindowManager.getInstance().getStatusBar(project) ?: return
            val widget = statusBar.getWidget("PullwiseStatusBar") as? PullwiseStatusBarWidget
            widget?.refresh()
        }
    }
}
