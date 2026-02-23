package com.schedulepartner.domain.focus.dto

import com.schedulepartner.domain.focus.entity.FocusStatus
import java.time.LocalDateTime

/**
 * POST /api/focus/start 응답
 */
data class FocusStartResponse(
    val sessionId: Long,
    val taskId: Long?,
    val targetMinutes: Int,
    val status: FocusStatus,
    val startedAt: LocalDateTime,
)