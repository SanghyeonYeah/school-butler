package com.schedulepartner.domain.ai.client

import com.schedulepartner.common.exception.CustomException
import com.schedulepartner.common.exception.ErrorCode
import com.schedulepartner.config.GeminiProperties
import com.schedulepartner.domain.ai.dto.*
import com.schedulepartner.domain.ai.prompt.ParsePromptBuilder
import com.schedulepartner.domain.ai.prompt.SummaryPromptBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class GeminiClient(
    @Qualifier("geminiWebClient") private val webClient: WebClient,
    private val geminiProperties: GeminiProperties,
    private val parsePromptBuilder: ParsePromptBuilder,
    private val summaryPromptBuilder: SummaryPromptBuilder,
    private val geminiResponseParser: GeminiResponseParser,
) {

    /**
     * 자연어 텍스트 → 일정 파싱 요청
     */
    fun parseSchedule(text: String, timezone: String): ParseResponse {
        val request = GeminiRequest(
            systemInstruction = GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(parsePromptBuilder.buildSystemInstruction())),
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(parsePromptBuilder.buildUserPrompt(text, timezone))),
                )
            ),
            generationConfig = GenerationConfig(
                temperature = geminiProperties.temperature,
                maxOutputTokens = geminiProperties.maxOutputTokens,
                responseMimeType = "application/json",
            ),
        )

        val rawJson = callGemini(request)
        return geminiResponseParser.parseScheduleResponse(rawJson)
    }

    /**
     * 하루 데이터 → 회고 요약 요청
     */
    fun generateSummary(req: com.schedulepartner.domain.ai.dto.SummaryRequest): SummaryResponse {
        val request = GeminiRequest(
            systemInstruction = GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(summaryPromptBuilder.buildSystemInstruction())),
            ),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(summaryPromptBuilder.buildUserPrompt(req))),
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.7,                              // 회고는 좀 더 창의적으로
                maxOutputTokens = geminiProperties.maxOutputTokens,
                responseMimeType = "application/json",
            ),
        )

        val rawJson = callGemini(request)
        return geminiResponseParser.parseSummaryResponse(rawJson)
    }

    // ── 공통 Gemini 호출 ────────────────────────────────────────────────────────

    private fun callGemini(request: GeminiRequest): String {
        return webClient.post()
            .uri(geminiProperties.generateContentUrl())
            .bodyValue(request)
            .retrieve()
            .bodyToMono(GeminiResponse::class.java)
            .timeout(Duration.ofSeconds(geminiProperties.timeoutSeconds))
            .flatMap { response -> extractText(response) }
            .onErrorMap(WebClientResponseException::class.java) { ex ->
                CustomException(
                    errorCode = ErrorCode.AI_002,
                    message = "Gemini API 호출 실패: ${ex.statusCode} - ${ex.responseBodyAsString}",
                    cause = ex,
                )
            }
            .onErrorMap(java.util.concurrent.TimeoutException::class.java) { ex ->
                CustomException(ErrorCode.AI_003, cause = ex)
            }
            .onErrorMap { ex ->
                if (ex is CustomException) ex
                else CustomException(ErrorCode.AI_002, cause = ex)
            }
            .block()
            ?: throw CustomException(ErrorCode.AI_001, "Gemini 응답이 비어있습니다")
    }

    private fun extractText(response: GeminiResponse): Mono<String> {
        // 안전 필터에 의해 차단된 경우
        if (response.promptFeedback?.blockReason != null) {
            return Mono.error(
                CustomException(
                    ErrorCode.AI_001,
                    "Gemini 안전 필터에 의해 차단됨: ${response.promptFeedback.blockReason}"
                )
            )
        }

        val text = response.candidates
            ?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text

        return if (text.isNullOrBlank()) {
            Mono.error(CustomException(ErrorCode.AI_001, "Gemini 응답 본문이 비어있습니다"))
        } else {
            Mono.just(text)
        }
    }
}