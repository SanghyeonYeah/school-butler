package com.schedulepartner.domain.focus.dto

import com.schedulepartner.domain.focus.entity.FocusStatus
import java.time.LocalDateTime

/**
 * GET /api/focus/{id}, POST /api/focus/{id}/pause|resume 공통 응답
 */
data class FocusSessionResponse(
    val sessionId: Long,
    val taskId: Long?,
    val targetMinutes: Int,
    val status: FocusStatus,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val actualMinutes: Int?,
    val pauseCount: Int,
    val totalPausedSeconds: Long,

    /** 현재까지 경과한 실제 집중 시간 (초) - 클라이언트 타이머 동기화용 */
    val elapsedFocusSeconds: Long,
)