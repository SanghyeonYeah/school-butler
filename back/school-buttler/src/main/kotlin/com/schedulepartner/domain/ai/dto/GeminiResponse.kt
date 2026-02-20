package com.schedulepartner.domain.ai.dto

/**
 * Gemini REST API 응답 최상위 객체
 */
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?,
    val promptFeedback: PromptFeedback? = null,
    val usageMetadata: UsageMetadata? = null,
)

data class GeminiCandidate(
    val content: GeminiContent,
    val finishReason: String?,   // "STOP" | "MAX_TOKENS" | "SAFETY" | "RECITATION" | "OTHER"
    val index: Int = 0,
    val safetyRatings: List<SafetyRating>? = null,
)

data class SafetyRating(
    val category: String,
    val probability: String,     // "NEGLIGIBLE" | "LOW" | "MEDIUM" | "HIGH"
    val blocked: Boolean = false,
)

data class PromptFeedback(
    val blockReason: String? = null,   // null 이면 정상 통과
    val safetyRatings: List<SafetyRating>? = null,
)

data class UsageMetadata(
    val promptTokenCount: Int = 0,
    val candidatesTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
)