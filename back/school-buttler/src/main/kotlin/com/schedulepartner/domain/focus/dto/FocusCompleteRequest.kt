package com.schedulepartner.domain.focus.dto

/**
 * POST /api/focus/{id}/complete
 * 집중 세션 완료
 */
data class FocusCompleteRequest(
    /**
     * 실제 집중 시간 override (분)
     * null 이면 서버가 started_at 기준으로 자동 계산
     */
    val actualMinutes: Int? = null,
)