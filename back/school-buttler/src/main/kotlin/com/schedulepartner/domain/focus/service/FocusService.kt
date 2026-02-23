package com.schedulepartner.domain.focus.service

import com.schedulepartner.common.exception.CustomException
import com.schedulepartner.common.exception.ErrorCode
import com.schedulepartner.common.util.DateUtil
import com.schedulepartner.domain.focus.dto.*
import com.schedulepartner.domain.focus.entity.FocusSession
import com.schedulepartner.domain.focus.entity.FocusStatus
import com.schedulepartner.domain.focus.repository.FocusRepository
import com.schedulepartner.domain.task.entity.TaskStatus
import com.schedulepartner.domain.task.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class FocusService(
    private val focusRepository: FocusRepository,
    private val taskRepository: TaskRepository,
) {
    private val log = LoggerFactory.getLogger(FocusService::class.java)

    // ── 세션 시작 ──────────────────────────────────────────────────────────────

    @Transactional
    fun start(userId: Long, req: FocusStartRequest): FocusStartResponse {
        // 이미 진행 중인 세션 있으면 중복 시작 방지
        focusRepository.findActiveSession(userId)?.let {
            throw CustomException(ErrorCode.FOCUS_001, "이미 진행 중인 집중 세션이 있습니다 (sessionId=${it.id})")
        }

        // Task 존재 여부 검증
        req.taskId?.let { taskId ->
            taskRepository.findByIdAndUserId(taskId, userId)
                ?: throw CustomException(ErrorCode.TASK_001, "일정을 찾을 수 없습니다")
        }

        val session = focusRepository.save(
            FocusSession(
                userId        = userId,
                taskId        = req.taskId,
                targetMinutes = req.targetMinutes,
            )
        )

        log.info("[Focus] 세션 시작: userId=$userId, sessionId=${session.id}, target=${req.targetMinutes}분")

        return FocusStartResponse(
            sessionId     = session.id,
            taskId        = session.taskId,
            targetMinutes = session.targetMinutes,
            status        = session.status,
            startedAt     = session.startedAt,
        )
    }

    // ── 일시정지 ───────────────────────────────────────────────────────────────

    @Transactional
    fun pause(userId: Long, sessionId: Long): FocusSessionResponse {
        val session = findSessionOrThrow(userId, sessionId)
        session.pause()
        log.info("[Focus] 일시정지: userId=$userId, sessionId=$sessionId, pauseCount=${session.pauseCount}")
        return session.toResponse()
    }

    // ── 재개 ───────────────────────────────────────────────────────────────────

    @Transactional
    fun resume(userId: Long, sessionId: Long): FocusSessionResponse {
        val session = findSessionOrThrow(userId, sessionId)
        session.resume()
        log.info("[Focus] 재개: userId=$userId, sessionId=$sessionId")
        return session.toResponse()
    }

    // ── 완료 ───────────────────────────────────────────────────────────────────

    @Transactional
    fun complete(userId: Long, sessionId: Long, req: FocusCompleteRequest): FocusCompleteResponse {
        val session = findSessionOrThrow(userId, sessionId)

        if (session.status == FocusStatus.COMPLETED) {
            throw CustomException(ErrorCode.FOCUS_001, "이미 완료된 세션입니다")
        }

        // 완료 처리 (actualMinutes 자동 계산)
        session.complete()

        // 클라이언트 override 값 있으면 덮어쓰기
        req.actualMinutes?.let { session.actualMinutes = it }

        val actualMinutes = session.actualMinutes ?: 0

        // 오늘 총 집중 시간
        val (startOfDay, endOfDay) = DateUtil.dayRange(session.startedAt.toLocalDate())
        val todayTotalMinutes = focusRepository.sumTodayFocusMinutes(userId, startOfDay, endOfDay)

        // Task 연동 처리
        var taskCompleted = false
        var timeDistortion: TimeDistortionResult? = null

        session.taskId?.let { taskId ->
            val task = taskRepository.findByIdAndUserId(taskId, userId)
            if (task != null) {
                // 완료된 세션 수로 이동 평균 업데이트
                val completedCount = focusRepository
                    .findByTaskIdAndStatusOrderByStartedAtDesc(taskId, FocusStatus.COMPLETED)
                    .size
                task.updateActualMinutes(actualMinutes, completedCount.coerceAtLeast(1))

                // 5회 이상 데이터 축적 시 시간 왜곡 감지
                if (completedCount >= 5 && task.isTimeDistorted()) {
                    val avgMin = task.actualAvgMinutes!!
                    val diff = ((avgMin - task.expectedMinutes).toDouble() / task.expectedMinutes * 100).toInt()
                    timeDistortion = TimeDistortionResult(
                        tag               = task.getTagList().firstOrNull() ?: "",
                        expectedMinutes   = task.expectedMinutes,
                        actualAvgMinutes  = avgMin,
                        isDistorted       = true,
                        distortionPercent = diff,
                    )
                }

                // Task 완료 처리 (목표 집중 시간 달성 시)
                if (actualMinutes >= task.expectedMinutes && task.status != TaskStatus.DONE) {
                    task.complete()
                    taskCompleted = true
                }
            }
        }

        log.info(
            "[Focus] 완료: userId=$userId, sessionId=$sessionId, " +
                    "actual=${actualMinutes}분, score=${session.focusScore()}, taskCompleted=$taskCompleted"
        )

        return FocusCompleteResponse(
            sessionId         = session.id,
            taskId            = session.taskId,
            actualMinutes     = actualMinutes,
            focusScore        = session.focusScore(),
            status            = session.status,
            endedAt           = session.endedAt!!,
            todayTotalMinutes = todayTotalMinutes,
            taskCompleted     = taskCompleted,
            timeDistortion    = timeDistortion,
        )
    }

    // ── 포기 ───────────────────────────────────────────────────────────────────

    @Transactional
    fun abandon(userId: Long, sessionId: Long): FocusSessionResponse {
        val session = findSessionOrThrow(userId, sessionId)
        session.abandon()
        log.info("[Focus] 포기: userId=$userId, sessionId=$sessionId, actual=${session.actualMinutes}분")
        return session.toResponse()
    }

    // ── 조회 ───────────────────────────────────────────────────────────────────

    fun getSession(userId: Long, sessionId: Long): FocusSessionResponse =
        findSessionOrThrow(userId, sessionId).toResponse()

    fun getTodaySessions(userId: Long): TodayFocusResponse {
        val (startOfDay, endOfDay) = DateUtil.dayRange(java.time.LocalDate.now())
        val sessions = focusRepository.findTodaySessions(userId, startOfDay, endOfDay)
        val completed = sessions.filter { it.status == FocusStatus.COMPLETED }

        return TodayFocusResponse(
            sessions          = sessions.map { it.toResponse() },
            totalFocusMinutes = completed.sumOf { it.actualMinutes ?: 0 },
            totalSessions     = sessions.size,
            completedSessions = completed.size,
            avgFocusScore     = completed.map { it.focusScore() }.average()
                .takeIf { !it.isNaN() }?.toInt() ?: 0,
        )
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private fun findSessionOrThrow(userId: Long, sessionId: Long): FocusSession =
        focusRepository.findByIdAndUserId(sessionId, userId)
            ?: throw CustomException(ErrorCode.FOCUS_001, "집중 세션을 찾을 수 없습니다")

    private fun FocusSession.toResponse(): FocusSessionResponse {
        val elapsedFocusSeconds = when (status) {
            FocusStatus.IN_PROGRESS -> {
                val total = Duration.between(startedAt, LocalDateTime.now()).seconds
                (total - totalPausedSeconds).coerceAtLeast(0)
            }
            FocusStatus.PAUSED -> {
                val total = Duration.between(startedAt, lastPausedAt ?: LocalDateTime.now()).seconds
                (total - totalPausedSeconds).coerceAtLeast(0)
            }
            else -> (actualMinutes?.toLong() ?: 0L) * 60
        }

        return FocusSessionResponse(
            sessionId           = id,
            taskId              = taskId,
            targetMinutes       = targetMinutes,
            status              = status,
            startedAt           = startedAt,
            endedAt             = endedAt,
            actualMinutes       = actualMinutes,
            pauseCount          = pauseCount,
            totalPausedSeconds  = totalPausedSeconds,
            elapsedFocusSeconds = elapsedFocusSeconds,
        )
    }
}