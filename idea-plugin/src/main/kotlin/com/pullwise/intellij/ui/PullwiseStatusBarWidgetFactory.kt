package com.pullwise.intellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class PullwiseStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = "PullwiseStatusBar"

    override fun getDisplayName(): String = "Pullwise Code Review"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return PullwiseStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
