package com.pullwise.intellij.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.pullwise.intellij.model.Issue
import com.pullwise.intellij.model.Review

@Service(Service.Level.PROJECT)
class PullwiseProjectService(private val project: Project) {

    var issues: List<Issue> = emptyList()
        private set

    var latestReview: Review? = null
        private set

    fun updateIssues(newIssues: List<Issue>, review: Review? = null) {
        issues = newIssues
        latestReview = review
        restartAnnotations()
    }

    fun clearIssues() {
        issues = emptyList()
        latestReview = null
        restartAnnotations()
    }

    fun getIssuesForFile(relativePath: String): List<Issue> {
        return issues.filter { issue ->
            issue.filePath != null && (
                issue.filePath == relativePath
                || issue.filePath.endsWith("/$relativePath")
                || relativePath.endsWith("/${issue.filePath}")
                || issue.filePath.replace("\\", "/") == relativePath.replace("\\", "/")
            )
        }
    }

    private fun restartAnnotations() {
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    companion object {
        fun getInstance(project: Project): PullwiseProjectService {
            return project.getService(PullwiseProjectService::class.java)
        }
    }
}
