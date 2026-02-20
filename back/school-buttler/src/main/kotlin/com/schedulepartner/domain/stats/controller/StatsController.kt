package com.schedulepartner.domain.stats.controller

import com.schedulepartner.domain.stats.dto.*
import com.schedulepartner.domain.stats.service.StatsService
import com.schedulepartner.infrastructure.jwt.JwtProvider
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/stats")
class StatsController(
    private val statsService: StatsService,
    private val jwtProvider: JwtProvider,
) {

    /**
     * GET /api/stats/daily?date=2026-02-11
     * 특정 날짜 일간 통계 (기본값: 오늘)
     */
    @GetMapping("/daily")
    fun getDailyStats(
        @RequestHeader("Authorization") token: String,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        date: LocalDate?,
    ): ResponseEntity<DailyStatsResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(statsService.getDailyStats(userId, date ?: LocalDate.now()))
    }

    /**
     * GET /api/stats/weekly
     * 최근 7일 주간 통계
     */
    @GetMapping("/weekly")
    fun getWeeklyStats(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<WeeklyStatsResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(statsService.getWeeklyStats(userId))
    }

    /**
     * GET /api/stats/monthly?year=2026&month=2
     * 특정 월 통계 (기본값: 이번 달)
     */
    @GetMapping("/monthly")
    fun getMonthlyStats(
        @RequestHeader("Authorization") token: String,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
    ): ResponseEntity<MonthlyStatsResponse> {
        val userId = extractUserId(token)
        val today = LocalDate.now()
        return ResponseEntity.ok(
            statsService.getMonthlyStats(
                userId,
                year ?: today.year,
                month ?: today.monthValue,
            )
        )
    }

    /**
     * GET /api/stats/by-tag
     * 전체 기간 태그별 누적 통계
     * 시간 왜곡 감지(⚠️) 배지 데이터 포함
     */
    @GetMapping("/by-tag")
    fun getTagStats(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<List<TagStatsResponse>> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(statsService.getTagStats(userId))
    }

    private fun extractUserId(token: String): Long =
        jwtProvider.getUserId(token.removePrefix("Bearer ").trim())
}