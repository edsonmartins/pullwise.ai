package com.pullwise.intellij.inspection

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFile
import com.pullwise.intellij.model.Issue
import com.pullwise.intellij.model.IssueSeverity
import com.pullwise.intellij.service.PullwiseProjectService
import com.pullwise.intellij.settings.PullwiseSettings

class PullwiseExternalAnnotator : ExternalAnnotator<PsiFile, List<Issue>>() {

    override fun collectInformation(file: PsiFile): PsiFile = file

    override fun doAnnotate(psiFile: PsiFile): List<Issue> {
        val project = psiFile.project
        val service = PullwiseProjectService.getInstance(project)

        val virtualFile = psiFile.virtualFile ?: return emptyList()
        val basePath = project.basePath ?: return emptyList()

        val relativePath = VfsUtilCore.getRelativePath(virtualFile,
            project.baseDir ?: return emptyList()) ?: return emptyList()

        // Apply severity filter
        val settings = PullwiseSettings.instance.state
        val minSeverity = IssueSeverity.fromString(settings.severityFilter)

        return service.getIssuesForFile(relativePath).filter { issue ->
            val severity = IssueSeverity.fromString(issue.severity)
            severity.level <= minSeverity.level
        }
    }

    override fun apply(file: PsiFile, issues: List<Issue>, holder: AnnotationHolder) {
        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile) ?: return

        for (issue in issues) {
            val lineStart = (issue.lineStart ?: 1) - 1
            val lineEnd = (issue.lineEnd ?: issue.lineStart ?: 1) - 1

            if (lineStart < 0 || lineStart >= document.lineCount) continue

            val startOffset = document.getLineStartOffset(lineStart.coerceIn(0, document.lineCount - 1))
            val endOffset = document.getLineEndOffset(lineEnd.coerceIn(0, document.lineCount - 1))

            val severity = mapSeverity(issue.severity)
            val message = buildMessage(issue)
            val tooltip = buildTooltip(issue)

            holder.newAnnotation(severity, message)
                .range(startOffset, endOffset)
                .tooltip(tooltip)
                .create()
        }
    }

    private fun mapSeverity(severity: String): HighlightSeverity {
        return when (severity.uppercase()) {
            "CRITICAL" -> HighlightSeverity.ERROR
            "HIGH" -> HighlightSeverity.ERROR
            "MEDIUM" -> HighlightSeverity.WARNING
            "LOW" -> HighlightSeverity.WEAK_WARNING
            "INFO" -> HighlightSeverity.INFORMATION
            else -> HighlightSeverity.INFORMATION
        }
    }

    private fun buildMessage(issue: Issue): String {
        return "[Pullwise] ${issue.title}"
    }

    private fun buildTooltip(issue: Issue): String {
        val parts = mutableListOf<String>()
        parts.add("<b>[Pullwise - ${issue.severity}]</b> ${issue.title}")

        if (!issue.description.isNullOrBlank()) {
            parts.add(issue.description)
        }
        if (!issue.suggestion.isNullOrBlank()) {
            parts.add("<b>Suggestion:</b> ${issue.suggestion}")
        }
        if (!issue.ruleId.isNullOrBlank()) {
            parts.add("<i>Rule: ${issue.ruleId}</i>")
        }

        return parts.joinToString("<br/>")
    }
}
