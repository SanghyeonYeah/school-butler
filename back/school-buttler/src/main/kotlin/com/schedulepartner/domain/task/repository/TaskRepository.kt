package com.schedulepartner.domain.task.repository

import com.schedulepartner.domain.task.entity.Task
import com.schedulepartner.domain.task.entity.TaskStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TaskRepository : JpaRepository<Task, Long> {

    /** 오늘 일정 전체 조회 (시작 시각 오름차순) */
    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND t.scheduledAt >= :startOfDay
          AND t.scheduledAt < :endOfDay
        ORDER BY t.scheduledAt ASC
    """)
    fun findTodayTasks(
        @Param("userId") userId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime,
    ): List<Task>

    /** 특정 날짜 미완료 일정 조회 - 복구 엔진 진입 조건 판단에 사용 */
    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND t.scheduledAt >= :startOfDay
          AND t.scheduledAt < :endOfDay
          AND t.status IN ('PENDING', 'SKIPPED')
        ORDER BY t.priority DESC, t.scheduledAt ASC
    """)
    fun findIncompleteTasks(
        @Param("userId") userId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime,
    ): List<Task>

    /** 태그별 완료 일정 조회 - 시간 왜곡 감지용 */
    @Query("""
        SELECT t FROM Task t
        WHERE t.userId = :userId
          AND t.status = 'DONE'
          AND t.tags LIKE %:tag%
        ORDER BY t.completedAt DESC
    """)
    fun findCompletedByTag(
        @Param("userId") userId: Long,
        @Param("tag") tag: String,
    ): List<Task>

    /** 기간별 완료 일정 수 - 통계용 */
    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.userId = :userId
          AND t.status = 'DONE'
          AND t.completedAt >= :from
          AND t.completedAt < :to
    """)
    fun countCompleted(
        @Param("userId") userId: Long,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): Long

    /** 기간별 전체 일정 - 회고 요약용 */
    fun findByUserIdAndScheduledAtBetweenOrderByScheduledAtAsc(
        userId: Long,
        from: LocalDateTime,
        to: LocalDateTime,
    ): List<Task>

    /** 단일 일정 조회 (userId 검증 포함, 타인 접근 방지) */
    fun findByIdAndUserId(id: Long, userId: Long): Task?

    /** 17:00 이후 미완료 일정 수 - 복구 엔진 트리거 조건 */
    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.userId = :userId
          AND t.scheduledAt >= :startOfDay
          AND t.scheduledAt < :endOfDay
          AND t.status IN ('PENDING', 'SKIPPED')
    """)
    fun countIncompleteToday(
        @Param("userId") userId: Long,
        @Param("startOfDay") startOfDay: LocalDateTime,
        @Param("endOfDay") endOfDay: LocalDateTime,
    ): Long
}