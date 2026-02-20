package com.schedulepartner.domain.recovery.dto

import com.schedulepartner.domain.recovery.entity.RecoveryTriggerType

/**
 * POST /api/recovery/plan 요청
 * 기본적으로 서버가 자동 감지하지만, 수동 요청도 가능
 */
data class RecoveryPlanRequest(
    /** 복구 대상 날짜. null이면 오늘 */
    val targetDate: String? = null,

    /** 취침 예정 시각 (HH:mm). 복구 배치 역산 기준 */
    val bedtime: String = "23:30",
)

/**
 * POST /api/recovery/plan 응답
 * 바텀시트에 표시될 복구 플랜
 */
data class RecoveryPlanResponse(
    val planId: Long,
    val triggerType: RecoveryTriggerType,
    val totalDuration: Int,

    /** 정렬된 복구 항목 목록 */
    val plan: List<RecoveryPlanItem>,

    /** 복구 완료 예상 시각 (HH:mm) */
    val estimatedEndTime: String,

    /** 취침 전 여유 시간 (분) */
    val bufferMinutes: Int,
)

/**
 * 복구 플랜 단일 항목
 * 기획서 예시:
 *   19:00→19:30 영어 (우선순위 1위)
 *   19:35→20:10 수학 (실제 평균 35분)
 *   20:15→20:20 휴식 (자동)
 */
data class RecoveryPlanItem(
    /** null이면 자동 삽입된 휴식 시간 */
    val taskId: Long?,

    val title: String,
    val startTime: String,       // "HH:mm"
    val endTime: String,         // "HH:mm"
    val durationMinutes: Int,

    /** 복구 엔진 우선순위 점수 */
    val priorityScore: Double,

    /** 항목 유형 */
    val itemType: RecoveryItemType,

    /**
     * 시간 왜곡 감지 정보
     * 실제 평균이 예상과 다른 경우 표시 (ex: "실제 평균: 47분")
     */
    val actualAvgMinutes: Int?,
)

enum class RecoveryItemType {
    TASK,       // 일반 일정
    BREAK,      // 자동 삽입 휴식
}