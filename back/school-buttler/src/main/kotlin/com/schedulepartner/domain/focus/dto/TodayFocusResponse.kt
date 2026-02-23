package com.schedulepartner.domain.focus.dto

/**
 * GET /api/focus/today
 * 오늘 집중 세션 목록 + 요약
 * 홈 화면 집중 시간 도넛 차트용
 */
data class TodayFocusResponse(
    val sessions: List<FocusSessionResponse>,
    val totalFocusMinutes: Int,
    val totalSessions: Int,
    val completedSessions: Int,
    val avgFocusScore: Int,
)