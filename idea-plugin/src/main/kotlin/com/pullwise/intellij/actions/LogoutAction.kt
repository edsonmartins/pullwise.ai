package com.pullwise.intellij.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.pullwise.intellij.auth.AuthManager
import com.pullwise.intellij.service.PullwiseProjectService

class LogoutAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        AuthManager.instance.clearToken()

        if (project != null) {
            PullwiseProjectService.getInstance(project).clearIssues()
        }

        Notification("Pullwise", "Pullwise", "Logged out of Pullwise", NotificationType.INFORMATION)
            .notify(project)
    }
}
