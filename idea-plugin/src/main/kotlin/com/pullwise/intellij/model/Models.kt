package com.pullwise.intellij.model

data class Review(
    val id: Long,
    val pullRequestId: Long? = null,
    val prNumber: Int = 0,
    val prTitle: String = "",
    val status: String = "",
    val createdAt: String? = null,
    val completedAt: String? = null,
    val stats: ReviewStats? = null
)

data class ReviewStats(
    val totalIssues: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val infoCount: Int = 0
)

data class Issue(
    val id: Long,
    val reviewId: Long = 0,
    val severity: String = "INFO",
    val type: String = "",
    val source: String = "",
    val title: String = "",
    val description: String? = null,
    val filePath: String? = null,
    val lineStart: Int? = null,
    val lineEnd: Int? = null,
    val ruleId: String? = null,
    val suggestion: String? = null,
    val codeSnippet: String? = null,
    val fixedCode: String? = null,
    val isFalsePositive: Boolean = false,
    val createdAt: String? = null
)

data class Project(
    val id: Long,
    val name: String = "",
    val repositoryUrl: String = "",
    val platform: String = "",
    val isActive: Boolean = true
)

data class LoginResponse(
    val token: String
)

data class ApiErrorResponse(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)

data class PaginatedResponse<T>(
    val content: List<T>? = null
)

enum class IssueSeverity(val level: Int) {
    CRITICAL(1),
    HIGH(2),
    MEDIUM(3),
    LOW(4),
    INFO(5);

    companion object {
        fun fromString(value: String): IssueSeverity {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: INFO
        }
    }
}
