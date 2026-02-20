package com.schedulepartner.domain.stats.dto

import java.time.LocalDate

/**
 * GET /api/stats/weekly
 * 최근 7일 주간 통계
 */
data class WeeklyStatsResponse(

    /** 주간 총 집중 시간 (분) */
    val weekTotalMinutes: Int,

    /** 주간 평균 완료율 (0.0 ~ 1.0) */
    val avgCompletionRate: Double,

    /** 현재 스트릭 (연속 달성일) */
    val currentStreak: Int,

    /** 태그별 집중 시간 */
    val byTag: List<TagWeeklyStat>,

    /** 요일별 집중 시간 (S M T W T F S) */
    val daily: List<DailyMinuteStat>,
)

data class TagWeeklyStat(
    val tag: String,

    /** 태그 이 주 총 집중 시간 (분) */
    val minutes: Int,

    /** 예상 vs 실제 시간 왜곡 여부 */
    val isTimeDistorted: Boolean,

    /** 실제 평균 소요 시간 (분) - null이면 데이터 부족 */
    val actualAvgMinutes: Int?,
)

data class DailyMinuteStat(
    val date: String,            // "MM-DD"
    val minutes: Int,
    val completionRate: Double,
)