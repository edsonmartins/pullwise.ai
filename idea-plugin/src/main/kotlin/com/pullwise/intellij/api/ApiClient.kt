package com.pullwise.intellij.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pullwise.intellij.auth.AuthManager
import com.pullwise.intellij.model.*
import com.pullwise.intellij.settings.PullwiseSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
class ApiClient {

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val token = AuthManager.instance.getToken()
            val request = if (token != null) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }
        .build()

    private val baseUrl: String
        get() = PullwiseSettings.instance.state.apiUrl

    fun login(email: String, password: String): String {
        val body = gson.toJson(mapOf("email" to email, "password" to password))
        val request = Request.Builder()
            .url("$baseUrl/auth/login")
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw ApiException("Login failed: ${response.code}")

        val loginResponse = gson.fromJson(response.body?.string(), LoginResponse::class.java)
        AuthManager.instance.setToken(loginResponse.token)
        return loginResponse.token
    }

    fun getReviews(projectId: String): List<Review> {
        val request = Request.Builder()
            .url("$baseUrl/reviews?projectId=$projectId")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw ApiException("Failed to fetch reviews: ${response.code}")

        val body = response.body?.string() ?: return emptyList()

        // Try paginated response first, then raw list
        return try {
            val type = object : TypeToken<PaginatedResponse<Review>>() {}.type
            val paginated: PaginatedResponse<Review> = gson.fromJson(body, type)
            paginated.content ?: emptyList()
        } catch (e: Exception) {
            val type = object : TypeToken<List<Review>>() {}.type
            gson.fromJson(body, type) ?: emptyList()
        }
    }

    fun getLatestReview(projectId: String): Review? {
        val reviews = getReviews(projectId)
        return reviews.maxByOrNull { it.createdAt ?: "" }
    }

    fun getReviewIssues(reviewId: Long): List<Issue> {
        val request = Request.Builder()
            .url("$baseUrl/reviews/$reviewId/issues")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw ApiException("Failed to fetch issues: ${response.code}")

        val body = response.body?.string() ?: return emptyList()
        val type = object : TypeToken<List<Issue>>() {}.type
        return gson.fromJson(body, type) ?: emptyList()
    }

    fun triggerReview(projectId: String, prNumber: Int): Review {
        val body = gson.toJson(mapOf(
            "pullRequestId" to projectId,
            "sastEnabled" to true,
            "llmEnabled" to true,
            "ragEnabled" to false
        ))

        val request = Request.Builder()
            .url("$baseUrl/reviews")
            .post(body.toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw ApiException("Failed to trigger review: ${response.code}")

        return gson.fromJson(response.body?.string(), Review::class.java)
    }

    fun markFalsePositive(issueId: Long) {
        val request = Request.Builder()
            .url("$baseUrl/reviews/issues/$issueId/false-positive")
            .post("".toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw ApiException("Failed to mark false positive: ${response.code}")
    }

    fun acknowledgeIssue(issueId: Long) {
        val request = Request.Builder()
            .url("$baseUrl/reviews/issues/$issueId/acknowledge")
            .post("".toRequestBody(jsonMediaType))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw ApiException("Failed to acknowledge issue: ${response.code}")
    }

    class ApiException(message: String) : IOException(message)

    companion object {
        val instance: ApiClient
            get() = ApplicationManager.getApplication().getService(ApiClient::class.java)
    }
}
