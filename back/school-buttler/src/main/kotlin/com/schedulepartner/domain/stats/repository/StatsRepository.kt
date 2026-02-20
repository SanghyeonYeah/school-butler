package com.schedulepartner.domain.stats.repository

import com.schedulepartner.domain.task.entity.Task
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface StatsRepository : JpaRepository<Task, Long> {

    // ── 공통 기간 조회 ──────────────────────────────────────────────────────────

    /** 기간 내 전체 일정 조회 */
    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND t.scheduledAt >= :from
          AND t.scheduledAt < :to
        ORDER BY t.scheduledAt ASC
    """)
    fun findByPeriod(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Task>

    /** 기간 내 완료 일정 조회 */
    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND t.completedAt >= :from
          AND t.completedAt < :to
          AND t.status = 'DONE'
        ORDER BY t.completedAt ASC
    """)
    fun findCompletedByPeriod(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Task>

    // ── 태그별 통계 ────────────────────────────────────────────────────────────

    /** 특정 태그 전체 일정 (완료 포함) */
    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND t.tags LIKE %:tag%
        ORDER BY t.scheduledAt DESC
    """)
    fun findAllByTag(
        @Param("userId") userId: Long,
        @Param("tag") tag: String,
    ): List<Task>

    /** 특정 태그 완료 일정만 */
    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND t.tags LIKE %:tag%
          AND t.status = 'DONE'
        ORDER BY t.completedAt DESC
    """)
    fun findCompletedByTag(
        @Param("userId") userId: Long,
        @Param("tag") tag: String,
    ): List<Task>

    // ── 집계 쿼리 ──────────────────────────────────────────────────────────────

    /** 기간 내 일정별 expectedMinutes 합계 (목표 시간) */
    @Query("""
        SELECT COALESCE(SUM(t.expectedMinutes), 0) FROM Task t
        WHERE t.userId = :userId
          AND t.status = 'DONE'
          AND t.completedAt >= :from
          AND t.completedAt < :to
    """)
    fun sumCompletedMinutes(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Int

    /** 사용자의 모든 고유 태그 목록 */
    @Query("""
        SELECT DISTINCT t.tags FROM Task t
        WHERE t.userId = :userId
          AND t.tags IS NOT NULL
    """)
    fun findAllTagStrings(@Param("userId") userId: Long): List<String>

    /** 복구 엔진 사용 횟수 */
    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.userId = :userId
          AND t.isRecovered = true
          AND t.scheduledAt >= :from
          AND t.scheduledAt < :to
    """)
    fun countRecoveredTasks(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long
}