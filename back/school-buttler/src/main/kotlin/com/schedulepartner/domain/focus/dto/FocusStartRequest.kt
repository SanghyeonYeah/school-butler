package com.schedulepartner.domain.focus.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * POST /api/focus/start
 * 집중 세션 시작
 */
data class FocusStartRequest(

    /** 연결할 Task ID - null 이면 자유 집중 */
    val taskId: Long? = null,

    /**
     * 목표 집중 시간 (분)
     * 25분(기본) | 50분 | 1 ~ 120 사용자 지정
     */
    @field:Min(value = 1,   message = "집중 시간은 1분 이상이어야 합니다")
    @field:Max(value = 120, message = "집중 시간은 120분 이하여야 합니다")
    val targetMinutes: Int = 25,
)