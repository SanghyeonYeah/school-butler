package com.schedulepartner.domain.stats.service

import com.schedulepartner.domain.stats.dto.*
import com.schedulepartner.domain.stats.repository.StatsRepository
import com.schedulepartner.domain.task.entity.Task
import com.schedulepartner.domain.task.entity.TaskStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class StatsService(
    private val statsRepository: StatsRepository,
) {

    // ── 일간 통계 ──────────────────────────────────────────────────────────────

    fun getDailyStats(userId: Long, date: LocalDate): DailyStatsResponse {
        val (from, to) = date.toDayRange()
        val tasks = statsRepository.findByPeriod(userId, from, to)
        val focusMinutes = statsRepository.sumCompletedMinutes(userId, from, to)
        val recoveryCount = statsRepository.countRecoveredTasks(userId, from, to).toInt()

        val completed = tasks.count { it.status == TaskStatus.DONE }
        val tagStats = aggregateTagDaily(tasks)

        return DailyStatsResponse(
            date = date,
            totalTasks = tasks.size,
            completedTasks = completed,
            completionRate = tasks.completionRate(),
            focusMinutes = focusMinutes,
            recoveryCount = recoveryCount,
            byTag = tagStats,
        )
    }

    // ── 주간 통계 ──────────────────────────────────────────────────────────────

    fun getWeeklyStats(userId: Long): WeeklyStatsResponse {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)       // 최근 7일

        val weekTotalMinutes = statsRepository.sumCompletedMinutes(
            userId,
            weekStart.atStartOfDay(),
            today.nextDayStart(),
        )

        // 요일별 통계
        val dailyStats = (0..6).map { offset ->
            val day = weekStart.plusDays(offset.toLong())
            val (from, to) = day.toDayRange()
            val tasks = statsRepository.findByPeriod(userId, from, to)
            DailyMinuteStat(
                date = day.format(DateTimeFormatter.ofPattern("MM-dd")),
                minutes = statsRepository.sumCompletedMinutes(userId, from, to),
                completionRate = tasks.completionRate(),
            )
        }

        val avgCompletionRate = dailyStats
            .map { it.completionRate }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        // 태그별 집계
        val allTagStrings = statsRepository.findAllTagStrings(userId)
        val tags = parseAllTags(allTagStrings)
        val tagStats = tags.map { tag ->
            val completed = statsRepository.findCompletedByTag(userId, tag)
                .filter { it.completedAt?.isAfter(weekStart.atStartOfDay()) == true }
            val totalMinutes = completed.sumOf { it.expectedMinutes }
            val actualAvg = completed.mapNotNull { it.actualAvgMinutes }.average()
                .takeIf { !it.isNaN() }?.toInt()
            val expectedAvg = completed.map { it.expectedMinutes }.average()
                .takeIf { !it.isNaN() }?.toInt() ?: 0

            TagWeeklyStat(
                tag = tag,
                minutes = totalMinutes,
                isTimeDistorted = actualAvg != null &&
                        Math.abs(actualAvg - expectedAvg).toDouble() / expectedAvg >= 0.3,
                actualAvgMinutes = actualAvg,
            )
        }.filter { it.minutes > 0 }

        val streak = calculateStreak(userId, today)

        return WeeklyStatsResponse(
            weekTotalMinutes = weekTotalMinutes,
            avgCompletionRate = avgCompletionRate,
            currentStreak = streak,
            byTag = tagStats,
            daily = dailyStats,
        )
    }

    // ── 월간 통계 ──────────────────────────────────────────────────────────────

    fun getMonthlyStats(userId: Long, year: Int, month: Int): MonthlyStatsResponse {
        val monthStart = LocalDate.of(year, month, 1)
        val monthEnd = monthStart.plusMonths(1)

        val monthTotalMinutes = statsRepository.sumCompletedMinutes(
            userId,
            monthStart.atStartOfDay(),
            monthEnd.atStartOfDay(),
        )

        // 일별 히트맵
        val heatmap = generateHeatmap(userId, monthStart, monthEnd)

        val avgCompletionRate = heatmap.map { it.completionRate }.average()
            .takeIf { !it.isNaN() } ?: 0.0

        // 주차별 집계
        val byWeek = (1..5).mapNotNull { week ->
            val weekStart = monthStart.plusDays(((week - 1) * 7).toLong())
            if (weekStart >= monthEnd) return@mapNotNull null
            val weekEnd = minOf(weekStart.plusDays(7), monthEnd)
            val minutes = statsRepository.sumCompletedMinutes(
                userId,
                weekStart.atStartOfDay(),
                weekEnd.atStartOfDay(),
            )
            WeeklyMinuteStat(week = week, minutes = minutes)
        }

        // 태그별 집계
        val allTagStrings = statsRepository.findAllTagStrings(userId)
        val tags = parseAllTags(allTagStrings)
        val tagStats = tags.map { tag ->
            val completed = statsRepository.findCompletedByTag(userId, tag)
                .filter { it.completedAt?.toLocalDate()?.let { d -> d >= monthStart && d < monthEnd } == true }
            TagMonthlyStat(
                tag = tag,
                minutes = completed.sumOf { it.expectedMinutes },
                completedCount = completed.size,
            )
        }.filter { it.minutes > 0 }

        val longestStreak = calculateLongestStreak(userId, monthStart, monthEnd)

        return MonthlyStatsResponse(
            year = year,
            month = month,
            monthTotalMinutes = monthTotalMinutes,
            avgCompletionRate = avgCompletionRate,
            longestStreak = longestStreak,
            byTag = tagStats,
            byWeek = byWeek,
            heatmap = heatmap,
        )
    }

    // ── 태그별 통계 ────────────────────────────────────────────────────────────

    fun getTagStats(userId: Long): List<TagStatsResponse> {
        val allTagStrings = statsRepository.findAllTagStrings(userId)
        val tags = parseAllTags(allTagStrings)

        return tags.map { tag ->
            val all = statsRepository.findAllByTag(userId, tag)
            val completed = all.filter { it.status == TaskStatus.DONE }
            val completionRate = if (all.isEmpty()) 0.0 else completed.size.toDouble() / all.size

            val expectedAvg = all.map { it.expectedMinutes }.average()
                .takeIf { !it.isNaN() }?.toInt() ?: 0
            val actualAvg = completed.mapNotNull { it.actualAvgMinutes }.average()
                .takeIf { !it.isNaN() }?.toInt()

            val distortionPercent = if (actualAvg != null && expectedAvg > 0) {
                ((actualAvg - expectedAvg).toDouble() / expectedAvg * 100).toInt()
            } else null

            val isDistorted = actualAvg != null && expectedAvg > 0 &&
                    Math.abs(actualAvg - expectedAvg).toDouble() / expectedAvg >= 0.3

            TagStatsResponse(
                tag = tag,
                totalCount = all.size,
                completedCount = completed.size,
                completionRate = completionRate,
                expectedAvgMinutes = expectedAvg,
                actualAvgMinutes = actualAvg,
                isTimeDistorted = isDistorted,
                distortionPercent = distortionPercent,
            )
        }.sortedByDescending { it.totalCount }
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    /** 연속 달성 스트릭 계산 (오늘부터 역순) */
    private fun calculateStreak(userId: Long, from: LocalDate): Int {
        var streak = 0
        var date = from
        while (true) {
            val (start, end) = date.toDayRange()
            val tasks = statsRepository.findByPeriod(userId, start, end)
            if (tasks.isEmpty() || tasks.completionRate() < 0.5) break
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    /** 특정 월 최장 스트릭 계산 */
    private fun calculateLongestStreak(userId: Long, from: LocalDate, to: LocalDate): Int {
        var longest = 0
        var current = 0
        var date = from
        while (date < to) {
            val (start, end) = date.toDayRange()
            val tasks = statsRepository.findByPeriod(userId, start, end)
            if (tasks.isNotEmpty() && tasks.completionRate() >= 0.5) {
                current++
                longest = maxOf(longest, current)
            } else {
                current = 0
            }
            date = date.plusDays(1)
        }
        return longest
    }

    /** 일별 히트맵 데이터 생성 */
    private fun generateHeatmap(userId: Long, from: LocalDate, to: LocalDate): List<DayHeatmap> {
        val result = mutableListOf<DayHeatmap>()
        var date = from
        while (date < to) {
            val (start, end) = date.toDayRange()
            val minutes = statsRepository.sumCompletedMinutes(userId, start, end)
            val tasks = statsRepository.findByPeriod(userId, start, end)
            val rate = tasks.completionRate()
            result.add(
                DayHeatmap(
                    date = date,
                    level = when {
                        minutes == 0 -> 0
                        minutes < 30 -> 1
                        minutes < 90 -> 2
                        else -> 3
                    },
                    minutes = minutes,
                    completionRate = rate,
                )
            )
            date = date.plusDays(1)
        }
        return result
    }

    /** 태그 문자열 목록 → 고유 태그 Set */
    private fun parseAllTags(tagStrings: List<String>): Set<String> =
        tagStrings.flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    /** 태그별 일간 통계 집계 */
    private fun aggregateTagDaily(tasks: List<Task>): List<TagDailyStat> {
        val tagMap = mutableMapOf<String, Pair<Int, Int>>()  // tag → (completed, total)
        tasks.forEach { task ->
            task.getTagList().forEach { tag ->
                val (c, t) = tagMap.getOrDefault(tag, Pair(0, 0))
                val newC = if (task.status == TaskStatus.DONE) c + 1 else c
                tagMap[tag] = Pair(newC, t + 1)
            }
        }
        return tagMap.map { (tag, pair) ->
            TagDailyStat(tag = tag, completedCount = pair.first, totalCount = pair.second)
        }.sortedByDescending { it.totalCount }
    }

    // ── 확장 함수 ──────────────────────────────────────────────────────────────

    private fun LocalDate.toDayRange(): Pair<LocalDateTime, LocalDateTime> =
        Pair(this.atStartOfDay(), this.plusDays(1).atStartOfDay())

    private fun LocalDate.nextDayStart(): LocalDateTime =
        this.plusDays(1).atStartOfDay()

    private fun List<Task>.completionRate(): Double =
        if (isEmpty()) 0.0 else count { it.status == TaskStatus.DONE }.toDouble() / size
}