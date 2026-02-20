package com.schedulepartner.domain.ai.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * POST /api/ai/parse
 * 자연어 텍스트를 일정 데이터로 파싱 요청
 *
 * 예시: { "text": "내일 오후 7시 수학 복습 30분" }
 */
data class ParseRequest(

    @field:NotBlank(message = "파싱할 텍스트를 입력해주세요")
    @field:Size(min = 1, max = 200, message = "입력은 1자 이상 200자 이내여야 합니다")
    val text: String,

    /** IANA 타임존 ID. 기본값: Asia/Seoul */
    val timezone: String = "Asia/Seoul",
)