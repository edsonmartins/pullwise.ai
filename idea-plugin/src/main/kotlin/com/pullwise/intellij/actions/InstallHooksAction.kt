package com.pullwise.intellij.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.terminal.JBTerminalWidget
import com.intellij.openapi.wm.ToolWindowManager

class InstallHooksAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Open terminal and run command
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
        toolWindow?.show {
            toolWindow.activate(null)
        }

        // Execute pullwise hooks install via GeneralCommandLine
        try {
            val basePath = project.basePath ?: return
            val commandLine = GeneralCommandLine("pullwise", "hooks", "install")
                .withWorkDirectory(basePath)
            val process = commandLine.createProcess()
            process.waitFor()
        } catch (ex: Exception) {
            com.intellij.notification.Notification(
                "Pullwise", "Pullwise",
                "Failed to install hooks: ${ex.message}. Make sure 'pullwise' CLI is installed.",
                com.intellij.notification.NotificationType.ERROR
            ).notify(project)
        }
    }
}
