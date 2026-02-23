package com.schedulepartner.domain.focus.repository

import com.schedulepartner.domain.focus.entity.FocusSession
import com.schedulepartner.domain.focus.entity.FocusStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface FocusRepository : JpaRepository<FocusSession, Long> {

    /** 단일 세션 조회 (타인 접근 방지) */
    fun findByIdAndUserId(id: Long, userId: Long): FocusSession?

    /** 현재 진행 중 or 일시정지 세션 조회 - 중복 시작 방지 */
    @Query("""
        SELECT f FROM FocusSession f
        WHERE f.userId = :userId
          AND f.status IN ('IN_PROGRESS', 'PAUSED')
        ORDER BY f.startedAt DESC
        LIMIT 1
    """)
    fun findActiveSession(@Param("userId") userId: Long): FocusSession?

    /** 오늘 전체 세션 조회 */
    @Query("""
        SELECT f FROM FocusSession f
        WHERE f.userId = :userId
          AND f.startedAt >= :startOfDay
          AND f.startedAt < :endOfDay
        ORDER BY f.startedAt ASC
    """)
    fun findTodaySessions(
        @Param("userId") userId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime,
    ): List<FocusSession>

    /** 오늘 완료된 세션 총 집중 시간 합계 (분) */
    @Query("""
        SELECT COALESCE(SUM(f.actualMinutes), 0) FROM FocusSession f
        WHERE f.userId = :userId
          AND f.status = 'COMPLETED'
          AND f.startedAt >= :startOfDay
          AND f.startedAt < :endOfDay
    """)
    fun sumTodayFocusMinutes(
        @Param("userId") userId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime,
    ): Int

    /** 기간별 완료 세션 목록 - 통계용 */
    @Query("""
        SELECT f FROM FocusSession f
        WHERE f.userId = :userId
          AND f.status = 'COMPLETED'
          AND f.startedAt >= :from
          AND f.startedAt < :to
        ORDER BY f.startedAt ASC
    """)
    fun findCompletedByPeriod(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<FocusSession>

    /** 특정 Task 의 완료된 세션 목록 - 시간 왜곡 감지용 */
    fun findByTaskIdAndStatusOrderByStartedAtDesc(
        taskId: Long,
        status: FocusStatus,
    ): List<FocusSession>
}