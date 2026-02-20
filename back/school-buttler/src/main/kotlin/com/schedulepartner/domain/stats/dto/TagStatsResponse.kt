package com.schedulepartner.domain.stats.dto

import java.time.LocalDate

/**
 * GET /api/stats/by-tag
 * 전체 기간 태그별 누적 통계
 */
data class TagStatsResponse(
    val tag: String,

    /** 해당 태그 총 일정 수 */
    val totalCount: Int,

    /** 완료 수 */
    val completedCount: Int,

    /** 완료율 */
    val completionRate: Double,

    /** 사용자가 설정한 예상 평균 시간 (분) */
    val expectedAvgMinutes: Int,

    /**
     * AI가 학습한 실제 평균 소요 시간 (분)
     * null = 5회 미만 (시간 왜곡 감지 비활성)
     */
    val actualAvgMinutes: Int?,

    /** 시간 왜곡 감지 활성 여부 */
    val isTimeDistorted: Boolean,

    /** 왜곡 차이 퍼센트 (ex: +57 이면 예상보다 57% 더 걸림) */
    val distortionPercent: Int?,
)


