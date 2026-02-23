package com.schedulepartner.common.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 날짜/시간 공통 유틸
 * - 하루 범위, 주간 범위, 월간 범위 계산
 * - 포매터 상수 제공
 */
object DateUtil {

    // ── 포매터 ──────────────────────────────────────────────────────────────────
    val DATE_FMT: DateTimeFormatter       = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val TIME_FMT: DateTimeFormatter       = DateTimeFormatter.ofPattern("HH:mm")
    val DATETIME_FMT: DateTimeFormatter   = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val DISPLAY_DATE: DateTimeFormatter   = DateTimeFormatter.ofPattern("MM-dd")
    val DISPLAY_MONTH: DateTimeFormatter  = DateTimeFormatter.ofPattern("yyyy년 MM월")

    // ── 하루 범위 ───────────────────────────────────────────────────────────────

    /** 오늘 00:00:00 */
    fun startOfToday(): LocalDateTime = LocalDate.now().atStartOfDay()

    /** 오늘 23:59:59.999 */
    fun endOfToday(): LocalDateTime = LocalDate.now().atTime(LocalTime.MAX)

    /** 특정 날짜 시작 ~ 다음 날 시작 (반열린 구간 [from, to)) */
    fun dayRange(date: LocalDate): Pair<LocalDateTime, LocalDateTime> =
        date.atStartOfDay() to date.plusDays(1).atStartOfDay()

    fun dayRange(date: String): Pair<LocalDateTime, LocalDateTime> =
        dayRange(LocalDate.parse(date, DATE_FMT))

    // ── 주간 범위 ───────────────────────────────────────────────────────────────

    /** 최근 7일 범위 (오늘 포함) */
    fun recentWeekRange(): Pair<LocalDateTime, LocalDateTime> =
        LocalDate.now().minusDays(6).atStartOfDay() to
                LocalDate.now().plusDays(1).atStartOfDay()

    // ── 월간 범위 ───────────────────────────────────────────────────────────────

    /** 특정 연월의 시작 ~ 다음 달 시작 */
    fun monthRange(year: Int, month: Int): Pair<LocalDateTime, LocalDateTime> {
        val start = LocalDate.of(year, month, 1)
        return start.atStartOfDay() to start.plusMonths(1).atStartOfDay()
    }

    /** 이번 달 범위 */
    fun currentMonthRange(): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDate.now()
        return monthRange(now.year, now.monthValue)
    }

    // ── 17:00 판단 ──────────────────────────────────────────────────────────────

    /** 지금이 17:00 이후인지 */
    fun isAfter17(): Boolean = LocalTime.now().isAfter(LocalTime.of(17, 0))

    /** 특정 시각이 17:00 이후인지 */
    fun isAfter17(time: LocalTime): Boolean = time.isAfter(LocalTime.of(17, 0))

    // ── 파싱 ────────────────────────────────────────────────────────────────────

    fun parseDateTime(raw: String): LocalDateTime =
        LocalDateTime.parse(raw, DATETIME_FMT)

    fun parseDate(raw: String): LocalDate =
        LocalDate.parse(raw, DATE_FMT)

    fun parseTime(raw: String): LocalTime =
        LocalTime.parse(raw, TIME_FMT)

    // ── 포매팅 ───────────────────────────────────────────────────────────────────

    fun LocalDateTime.toDisplayDate(): String = format(DISPLAY_DATE)
    fun LocalDate.toDisplayDate(): String     = format(DISPLAY_DATE)
    fun LocalTime.toDisplayTime(): String     = format(TIME_FMT)
    fun LocalDateTime.toIso(): String         = format(DATETIME_FMT)

    // ── 현재 시각 5분 단위 올림 (복구 엔진 시작 시각 계산용) ─────────────────────

    fun ceilToFiveMinutes(time: LocalTime = LocalTime.now()): LocalTime {
        val minute = time.minute
        val remainder = minute % 5
        return if (remainder == 0) time.withSecond(0).withNano(0)
        else time.plusMinutes((5 - remainder).toLong()).withSecond(0).withNano(0)
    }
}