package com.schedulepartner.domain.stats.dto

import java.time.LocalDate

/**
 * GET /api/stats/monthly
 * 특정 월 통계
 */
data class MonthlyStatsResponse(

    val year: Int,
    val month: Int,

    /** 월간 총 집중 시간 (분) */
    val monthTotalMinutes: Int,

    /** 월간 평균 완료율 */
    val avgCompletionRate: Double,

    /** 최장 스트릭 */
    val longestStreak: Int,

    /** 태그별 집중 시간 */
    val byTag: List<TagMonthlyStat>,

    /**
     * 주차별 집중 시간
     * key: 1 ~ 5 (1주차 ~ 5주차)
     */
    val byWeek: List<WeeklyMinuteStat>,

    /** 일별 완료율 히트맵 데이터 */
    val heatmap: List<DayHeatmap>,
)

data class TagMonthlyStat(
    val tag: String,
    val minutes: Int,
    val completedCount: Int,
)

data class WeeklyMinuteStat(
    val week: Int,
    val minutes: Int,
)

data class DayHeatmap(
    val date: LocalDate,

    /** 0 = 없음, 1 = 낮음, 2 = 보통, 3 = 높음 */
    val level: Int,

    val minutes: Int,
    val completionRate: Double,
)