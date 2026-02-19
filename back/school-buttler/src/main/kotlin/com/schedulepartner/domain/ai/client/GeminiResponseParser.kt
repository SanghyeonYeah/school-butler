package com.schedulepartner.domain.ai.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.schedulepartner.common.exception.CustomException
import com.schedulepartner.common.exception.ErrorCode
import com.schedulepartner.domain.ai.dto.ParseResponse
import com.schedulepartner.domain.ai.dto.SummaryResponse
import org.springframework.stereotype.Component

/**
 * Gemini 응답 JSON → 앱 DTO 변환 + Validation 레이어
 * API 명세 "AI 응답 Validation 레이어 필수 적용" 요건 충족
 */
@Component
class GeminiResponseParser(
    private val objectMapper: ObjectMapper,
) {

    fun parseScheduleResponse(rawJson: String): ParseResponse {
        val cleaned = cleanJson(rawJson)
        val map = runCatching { objectMapper.readValue<Map<String, Any>>(cleaned) }
            .getOrElse { throw CustomException(ErrorCode.AI_001, "JSON 파싱 실패: $cleaned") }

        return runCatching {
            ParseResponse(
                title = requireString(map, "title"),
                scheduledAt = requireString(map, "scheduledAt").also { validateIso8601(it) },
                expectedMinutes = requireInt(map, "expectedMinutes").also {
                    require(it in 1..480) { "expectedMinutes 범위 초과: $it" }
                },
                tags = requireStringList(map, "tags"),
                confidence = requireDouble(map, "confidence").coerceIn(0.0, 1.0),
            )
        }.getOrElse {
            throw CustomException(ErrorCode.AI_001, "ParseResponse 변환 실패: ${it.message}")
        }
    }

    fun parseSummaryResponse(rawJson: String): SummaryResponse {
        val cleaned = cleanJson(rawJson)
        val map = runCatching { objectMapper.readValue<Map<String, Any>>(cleaned) }
            .getOrElse { throw CustomException(ErrorCode.AI_001, "JSON 파싱 실패: $cleaned") }

        return runCatching {
            SummaryResponse(
                headline = requireString(map, "headline").take(30),       // 초과 시 자름
                encouragement = requireString(map, "encouragement").take(40),
                tomorrowTip = requireString(map, "tomorrowTip").take(60),
                completionRate = requireDouble(map, "completionRate").coerceIn(0.0, 1.0),
            )
        }.getOrElse {
            throw CustomException(ErrorCode.AI_001, "SummaryResponse 변환 실패: ${it.message}")
        }
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────────────────

    /**
     * Gemini가 가끔 ```json ... ``` 마크다운 블록으로 감싸서 응답하는 경우 처리
     */
    private fun cleanJson(raw: String): String =
        raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()

    private fun validateIso8601(value: String) {
        runCatching {
            java.time.LocalDateTime.parse(value)
        }.getOrElse {
            throw IllegalArgumentException("ISO-8601 형식이 아닙니다: $value")
        }
    }

    private fun requireString(map: Map<String, Any>, key: String): String =
        (map[key] as? String)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("필드 누락 또는 빈 문자열: $key")

    private fun requireInt(map: Map<String, Any>, key: String): Int =
        when (val v = map[key]) {
            is Int -> v
            is Long -> v.toInt()
            is Double -> v.toInt()
            is String -> v.toIntOrNull() ?: throw IllegalArgumentException("Int 변환 실패: $key=$v")
            else -> throw IllegalArgumentException("필드 누락: $key")
        }

    private fun requireDouble(map: Map<String, Any>, key: String): Double =
        when (val v = map[key]) {
            is Double -> v
            is Int -> v.toDouble()
            is Long -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: throw IllegalArgumentException("Double 변환 실패: $key=$v")
            else -> throw IllegalArgumentException("필드 누락: $key")
        }

    @Suppress("UNCHECKED_CAST")
    private fun requireStringList(map: Map<String, Any>, key: String): List<String> =
        (map[key] as? List<*>)?.filterIsInstance<String>()
            ?: emptyList()
}