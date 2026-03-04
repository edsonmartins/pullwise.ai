package com.pullwise.intellij.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.pullwise.intellij.service.PullwiseProjectService
import com.pullwise.intellij.ui.PullwiseStatusBarWidget

class ClearDiagnosticsAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        PullwiseProjectService.getInstance(project).clearIssues()
        PullwiseStatusBarWidget.update(project)

        Notification("Pullwise", "Pullwise",
            "Pullwise diagnostics cleared",
            NotificationType.INFORMATION).notify(project)
    }
}
