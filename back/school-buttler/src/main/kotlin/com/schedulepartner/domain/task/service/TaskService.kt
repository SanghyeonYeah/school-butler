package com.schedulepartner.domain.task.service

import com.schedulepartner.common.exception.CustomException
import com.schedulepartner.common.exception.ErrorCode
import com.schedulepartner.domain.task.dto.*
import com.schedulepartner.domain.task.entity.Task
import com.schedulepartner.domain.task.entity.TaskStatus
import com.schedulepartner.domain.task.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class TaskService(
    private val taskRepository: TaskRepository,
) {
    private val log = LoggerFactory.getLogger(TaskService::class.java)

    // ── 생성 ──────────────────────────────────────────────────────────────────

    @Transactional
    fun createTask(userId: Long, req: TaskCreateRequest): TaskCreateResponse {
        val task = Task(
            userId = userId,
            title = req.title,
            description = req.description,
            scheduledAt = LocalDateTime.parse(req.scheduledAt),
            expectedMinutes = req.expectedMinutes,
            priority = req.priority,
        ).apply { setTagList(req.tags) }

        val saved = taskRepository.save(task)
        log.info("[Task] 생성: userId=$userId, taskId=${saved.id}, title=${saved.title}")

        return TaskCreateResponse(
            taskId = saved.id,
            status = saved.status,
            createdAt = saved.createdAt,
        )
    }

    // ── 조회 ──────────────────────────────────────────────────────────────────

    fun getTodayTasks(userId: Long): List<TaskResponse> {
        val (start, end) = todayRange()
        return taskRepository.findTodayTasks(userId, start, end)
            .map { TaskResponse.from(it) }
    }

    fun getTask(userId: Long, taskId: Long): TaskResponse {
        val task = findTaskOrThrow(userId, taskId)
        return TaskResponse.from(task)
    }

    // ── 수정 ──────────────────────────────────────────────────────────────────

    @Transactional
    fun updateTask(userId: Long, taskId: Long, req: TaskUpdateRequest): TaskResponse {
        val task = findTaskOrThrow(userId, taskId)

        req.title?.let { task.title = it }
        req.description?.let { task.description = it }
        req.scheduledAt?.let { task.scheduledAt = LocalDateTime.parse(it) }
        req.expectedMinutes?.let { task.expectedMinutes = it }
        req.priority?.let { task.priority = it }
        req.tags?.let { task.setTagList(it) }

        log.info("[Task] 수정: userId=$userId, taskId=$taskId")
        return TaskResponse.from(task)
    }

    // ── 삭제 ──────────────────────────────────────────────────────────────────

    @Transactional
    fun deleteTask(userId: Long, taskId: Long) {
        val task = findTaskOrThrow(userId, taskId)
        taskRepository.delete(task)
        log.info("[Task] 삭제: userId=$userId, taskId=$taskId")
    }

    // ── 완료 처리 ──────────────────────────────────────────────────────────────

    @Transactional
    fun completeTask(userId: Long, taskId: Long, actualMinutes: Int?): TaskCompleteResponse {
        val task = findTaskOrThrow(userId, taskId)

        if (task.status == TaskStatus.DONE) {
            throw CustomException(ErrorCode.TASK_002, "이미 완료된 일정입니다")
        }

        // 완료 처리
        task.complete()

        // 실제 소요 시간 기록 → 시간 왜곡 감지 데이터 축적
        if (actualMinutes != null && actualMinutes > 0) {
            val tagCompletedCount = task.getTagList()
                .flatMap { tag -> taskRepository.findCompletedByTag(userId, tag) }
                .size
            task.updateActualMinutes(actualMinutes, tagCompletedCount.coerceAtLeast(1))
        }

        // 오늘 진행률 계산
        val (start, end) = todayRange()
        val todayTasks = taskRepository.findTodayTasks(userId, start, end)
        val progressRate = if (todayTasks.isEmpty()) 0.0
        else todayTasks.count { it.status == TaskStatus.DONE }.toDouble() / todayTasks.size

        log.info("[Task] 완료: userId=$userId, taskId=$taskId, progressRate=${"%.2f".format(progressRate)}")

        return TaskCompleteResponse(
            taskId = task.id,
            status = task.status,
            completedAt = task.completedAt!!,
            todayProgressRate = progressRate,
            currentStreak = 0,  // TODO: StreakService 구현 후 연동
            timeDistortion = task.actualAvgMinutes?.let {
                TimeDistortionInfo(
                    actualAvgMinutes = it,
                    isDistorted = task.isTimeDistorted(),
                )
            },
        )
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private fun findTaskOrThrow(userId: Long, taskId: Long): Task =
        taskRepository.findByIdAndUserId(taskId, userId)
            ?: throw CustomException(ErrorCode.TASK_001)

    private fun todayRange(): Pair<LocalDateTime, LocalDateTime> {
        val today = LocalDate.now()
        return Pair(
            today.atStartOfDay(),
            today.atTime(LocalTime.MAX),
        )
    }
}