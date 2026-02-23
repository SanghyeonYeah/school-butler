package com.schedulepartner.domain.focus.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 집중 세션 - 포모도로 스타일 타이머 기록
 *
 * 흐름: START → (일시정지/재개 반복) → COMPLETED | ABANDONED
 * 완료 시 Task.actualAvgMinutes 업데이트 → 시간 왜곡 감지 데이터 축적
 */
@Entity
@Table(
    name = "focus_sessions",
    indexes = [
        Index(name = "idx_focus_user_started", columnList = "user_id, started_at"),
        Index(name = "idx_focus_task",         columnList = "task_id"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class FocusSession(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    /** 연결된 Task (null 이면 태스크 없이 자유 집중) */
    @Column(name = "task_id")
    val taskId: Long? = null,

    /** 목표 집중 시간 (분) - 25 | 50 | 사용자 지정 */
    @Column(name = "target_minutes", nullable = false)
    val targetMinutes: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: FocusStatus = FocusStatus.IN_PROGRESS,

    /** 세션 시작 시각 */
    @Column(name = "started_at", nullable = false)
    val startedAt: LocalDateTime = LocalDateTime.now(),

    /** 세션 종료 시각 (완료 or 포기 시 기록) */
    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    /**
     * 실제 집중 시간 (분) - 일시정지 시간 제외
     * 완료 시 Task.updateActualMinutes() 에 전달
     */
    @Column(name = "actual_minutes")
    var actualMinutes: Int? = null,

    /**
     * 일시정지 횟수
     * 집중도 지표: pauseCount 가 많을수록 집중 방해 요인 있음
     */
    @Column(name = "pause_count", nullable = false)
    var pauseCount: Int = 0,

    /** 마지막 일시정지 시각 (재개 시 경과 시간 계산용) */
    @Column(name = "last_paused_at")
    var lastPausedAt: LocalDateTime? = null,

    /** 누적 일시정지 시간 (초) */
    @Column(name = "total_paused_seconds", nullable = false)
    var totalPausedSeconds: Long = 0L,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
) {
    /** 일시정지 처리 */
    fun pause() {
        check(status == FocusStatus.IN_PROGRESS) { "진행 중인 세션만 일시정지할 수 있습니다" }
        status = FocusStatus.PAUSED
        lastPausedAt = LocalDateTime.now()
        pauseCount++
    }

    /** 재개 처리 - 일시정지 시간 누적 */
    fun resume() {
        check(status == FocusStatus.PAUSED) { "일시정지 상태에서만 재개할 수 있습니다" }
        lastPausedAt?.let { paused ->
            val elapsed = java.time.Duration.between(paused, LocalDateTime.now()).seconds
            totalPausedSeconds += elapsed
        }
        lastPausedAt = null
        status = FocusStatus.IN_PROGRESS
    }

    /** 완료 처리 - 실제 집중 시간 계산 */
    fun complete() {
        val now = LocalDateTime.now()
        endedAt = now

        val totalSeconds = java.time.Duration.between(startedAt, now).seconds
        val focusSeconds = totalSeconds - totalPausedSeconds
        actualMinutes = (focusSeconds / 60).toInt().coerceAtLeast(1)

        status = FocusStatus.COMPLETED
    }

    /** 포기 처리 */
    fun abandon() {
        endedAt = LocalDateTime.now()
        val totalSeconds = java.time.Duration.between(startedAt, endedAt!!).seconds
        actualMinutes = ((totalSeconds - totalPausedSeconds) / 60).toInt()
        status = FocusStatus.ABANDONED
    }

    /** 집중도 점수 (0 ~ 100) - 통계용 */
    fun focusScore(): Int {
        val actual = actualMinutes ?: return 0
        val targetSec = targetMinutes * 60
        val completionRatio = actual.toDouble() / targetMinutes
        val pausePenalty = (pauseCount * 5).coerceAtMost(30)
        return ((completionRatio * 100) - pausePenalty).toInt().coerceIn(0, 100)
    }
}

enum class FocusStatus {
    IN_PROGRESS,   // 집중 중
    PAUSED,        // 일시정지
    COMPLETED,     // 완료 (목표 시간 달성)
    ABANDONED,     // 포기
}