package com.schedulepartner.domain.task.dto

import jakarta.validation.constraints.*

/**
 * PATCH /api/tasks/{id}
 * 일정 수정 요청 - 모든 필드 선택적 (null 이면 기존 값 유지)
 */
data class TaskUpdateRequest(

    @field:Size(min = 1, max = 100, message = "제목은 1자 이상 100자 이내여야 합니다")
    val title: String? = null,

    @field:Size(max = 500, message = "설명은 500자 이내여야 합니다")
    val description: String? = null,

    @field:Pattern(
        regexp = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?",
        message = "시작 시각 형식은 YYYY-MM-DDTHH:mm:ss 이어야 합니다"
    )
    val scheduledAt: String? = null,

    @field:Min(value = 1, message = "예상 시간은 1분 이상이어야 합니다")
    @field:Max(value = 480, message = "예상 시간은 480분(8시간) 이하여야 합니다")
    val expectedMinutes: Int? = null,

    @field:Min(value = 1, message = "우선순위는 1 이상이어야 합니다")
    @field:Max(value = 5, message = "우선순위는 5 이하여야 합니다")
    val priority: Int? = null,

    @field:Size(max = 10, message = "태그는 최대 10개까지 가능합니다")
    val tags: List<String>? = null,
)