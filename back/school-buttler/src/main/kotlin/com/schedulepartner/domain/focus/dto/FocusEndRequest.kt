package com.schedulepartner.domain.focus.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

/**
 * POST /api/focus/{id}/end
 * 집중 세션 종료 (완료 or 포기 통합 엔드포인트)
 */
data class FocusEndRequest(

    /**
     * 종료 유형
     * COMPLETE : 목표 달성 완료 → 도파민 체인 트리거
     * ABANDON  : 중도 포기 → 부분 집중 시간만 기록
     */
    val endType: FocusEndType = FocusEndType.COMPLETE,

    /**
     * 실제 집중 시간 override (분)
     * null 이면 서버가 started_at 기준으로 자동 계산
     */
    @field:Min(value = 1,   message = "실제 집중 시간은 1분 이상이어야 합니다")
    @field:Max(value = 480, message = "실제 집중 시간은 480분 이하여야 합니다")
    val actualMinutes: Int? = null,
)

enum class FocusEndType {
    COMPLETE,   // 완료
    ABANDON,    // 포기
}