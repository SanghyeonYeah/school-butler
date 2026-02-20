package com.schedulepartner.domain.ai.dto

/**
 * Gemini REST API 요청 최상위 객체
 * POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 */
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig,
    val systemInstruction: GeminiContent? = null,
)

data class GeminiContent(
    val role: String = "user",   // "user" | "model"
    val parts: List<GeminiPart>,
)

data class GeminiPart(
    val text: String,
)

/**
 * 생성 설정
 * responseMimeType = "application/json" 으로 고정 → JSON 모드 강제, 파싱 안정성 ↑
 */
data class GenerationConfig(
    val temperature: Double = 0.2,
    val maxOutputTokens: Int = 1024,
    val responseMimeType: String = "application/json",
)