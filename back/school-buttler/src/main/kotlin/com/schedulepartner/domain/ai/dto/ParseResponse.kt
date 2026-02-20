package com.schedulepartner.domain.ai.dto

/**
 * POST /api/ai/parse 응답
 * Gemini 가 파싱한 일정 데이터
 */
data class ParseResponse(

    /** 파싱된 일정 제목. ex) "수학 복습" */
    val title: String,

    /**
     * 예정 시작 시각 (ISO-8601 LocalDateTime)
     * ex) "2026-02-12T19:00:00"
     */
    val scheduledAt: String,

    /** 예상 소요 시간 (분). 범위: 1 ~ 480 */
    val expectedMinutes: Int,

    /** 추출된 태그 목록. ex) ["수학", "복습"] */
    val tags: List<String>,

    /**
     * AI 파싱 신뢰도 (0.0 ~ 1.0)
     * 0.6 미만이면 프론트에서 사용자 재확인 유도 권장
     */
    val confidence: Double,
)