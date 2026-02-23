package com.schedulepartner.domain.focus.dto

import com.schedulepartner.domain.focus.entity.FocusStatus
import java.time.LocalDateTime

/**
 * POST /api/focus/{id}/complete 응답
 * 1.1초 도파민 체인 + 시간 왜곡 감지 결과 포함
 */
data class FocusCompleteResponse(
    val sessionId: Long,
    val taskId: Long?,
    val actualMinutes: Int,
    val focusScore: Int,            // 0 ~ 100
    val status: FocusStatus,
    val endedAt: LocalDateTime,

    /** 오늘 총 집중 시간 (분) - 홈 화면 도넛 차트 업데이트용 */
    val todayTotalMinutes: Int,

    /** Task 완료 여부 - true 면 도파민 체인 트리거 */
    val taskCompleted: Boolean,

    /** 시간 왜곡 감지 결과 */
    val timeDistortion: TimeDistortionResult?,
)