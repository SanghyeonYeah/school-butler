package com.schedulepartner.domain.stats.dto

import java.time.LocalDate

/**
 * GET /api/stats/daily
 * 특정 날짜 하루 통계
 */
data class DailyStatsResponse(
    val date: LocalDate,

    /** 전체 일정 수 */
    val totalTasks: Int,

    /** 완료 일정 수 */
    val completedTasks: Int,

    /** 완료율 (0.0 ~ 1.0) */
    val completionRate: Double,

    /** 총 집중 시간 (분) */
    val focusMinutes: Int,

    /** 복구 엔진 사용 횟수 */
    val recoveryCount: Int,

    /** 태그별 완료 현황 */
    val byTag: List<TagDailyStat>,
)

data class TagDailyStat(
    val tag: String,
    val completedCount: Int,
    val totalCount: Int,
)