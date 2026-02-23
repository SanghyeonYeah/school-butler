package com.schedulepartner.domain.focus.dto

/**
 * 시간 왜곡 감지 결과
 * 5회 이상 집중 데이터 축적 후 ±30% 이상 차이 시 반환
 */
data class TimeDistortionResult(
    val tag: String,
    val expectedMinutes: Int,
    val actualAvgMinutes: Int,

    /** true 이면 프론트에서 ⚠️ 뱃지 + 예상 시간 수정 유도 UI */
    val isDistorted: Boolean,

    /** 차이 퍼센트 (+57 이면 예상보다 57% 더 걸림) */
    val distortionPercent: Int,
)