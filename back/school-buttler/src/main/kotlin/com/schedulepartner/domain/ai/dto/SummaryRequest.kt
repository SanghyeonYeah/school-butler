package com.schedulepartner.domain.ai.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * POST /api/ai/summary
 * 하루 데이터를 기반으로 AI 회고 요약 생성 요청
 */
data class SummaryRequest(

    @field:NotBlank(message = "날짜를 입력해주세요")
    @field:Pattern(
        regexp = "\\d{4}-\\d{2}-\\d{2}",
        message = "날짜 형식은 YYYY-MM-DD 이어야 합니다"
    )
    val date: String,

    /** 완료한 일정 제목 목록 */
    val completedTasks: List<String> = emptyList(),

    /** 미완료 일정 제목 목록 */
    val incompleteTasks: List<String> = emptyList(),

    /** 오늘 총 집중 시간 (분) */
    val focusMinutes: Int = 0,

    /**
     * 사용자가 선택한 컨디션 키워드
     * ex) "최고", "보통", "피곤함", "아픔"
     */
    @field:Size(max = 20, message = "컨디션은 20자 이내여야 합니다")
    val mood: String? = null,

    /**
     * "내일의 나에게 한마디" - 사용자 직접 입력
     */
    @field:Size(max = 100, message = "메모는 100자 이내여야 합니다")
    val userNote: String? = null,
)