package com.pullwise.intellij.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.pullwise.intellij.api.ApiClient

class LoginAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        val email = Messages.showInputDialog(
            project,
            "Enter your Pullwise email:",
            "Pullwise Login",
            null
        ) ?: return

        val password = Messages.showPasswordDialog(
            project,
            "Enter your Pullwise password:",
            "Pullwise Login"
        ) ?: return

        try {
            ApiClient.instance.login(email, password)
            notify(project, "Successfully logged in to Pullwise", NotificationType.INFORMATION)
        } catch (ex: Exception) {
            notify(project, "Login failed: ${ex.message}", NotificationType.ERROR)
        }
    }

    private fun notify(project: com.intellij.openapi.project.Project?, message: String, type: NotificationType) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Pullwise Notifications")
        if (notification != null) {
            notification.createNotification(message, type).notify(project)
        } else {
            // Fallback if notification group not registered
            com.intellij.notification.Notification(
                "Pullwise", "Pullwise", message, type
            ).notify(project)
        }
    }
}
