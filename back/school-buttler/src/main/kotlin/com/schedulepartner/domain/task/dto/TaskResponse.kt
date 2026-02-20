package com.schedulepartner.domain.task.dto

import com.schedulepartner.domain.task.entity.Task
import com.schedulepartner.domain.task.entity.TaskStatus
import java.time.LocalDateTime

/**
 * GET /api/tasks, GET /api/tasks/{id} 등 공통 응답 DTO
 */
data class TaskResponse(

    val taskId: Long,
    val title: String,
    val description: String?,
    val scheduledAt: LocalDateTime,
    val expectedMinutes: Int,
    val priority: Int,
    val status: TaskStatus,
    val tags: List<String>,
    val completedAt: LocalDateTime?,
    val isRecovered: Boolean,
    val createdAt: LocalDateTime,

    /**
     * 시간 왜곡 감지 정보
     * null = 아직 5회 미만 데이터
     */
    val timeDistortion: TimeDistortionInfo?,
) {
    companion object {
        fun from(task: Task): TaskResponse = TaskResponse(
            taskId = task.id,
            title = task.title,
            description = task.description,
            scheduledAt = task.scheduledAt,
            expectedMinutes = task.expectedMinutes,
            priority = task.priority,
            status = task.status,
            tags = task.getTagList(),
            completedAt = task.completedAt,
            isRecovered = task.isRecovered,
            createdAt = task.createdAt,
            timeDistortion = task.actualAvgMinutes?.let {
                TimeDistortionInfo(
                    actualAvgMinutes = it,
                    isDistorted = task.isTimeDistorted(),
                )
            },
        )
    }
}

/**
 * 시간 왜곡 감지 정보
 * isDistorted = true 이면 프론트에서 ⚠️ 배지 + 수정 유도 UI 표시
 */
data class TimeDistortionInfo(
    val actualAvgMinutes: Int,
    val isDistorted: Boolean,
)

/**
 * POST /api/tasks 생성 응답 (API 명세 준수)
 */
data class TaskCreateResponse(
    val taskId: Long,
    val status: TaskStatus,
    val createdAt: LocalDateTime,
)

/**
 * POST /api/tasks/{id}/complete 완료 처리 응답
 * 1.1초 도파민 체인에 필요한 데이터 포함
 */
data class TaskCompleteResponse(
    val taskId: Long,
    val status: TaskStatus,
    val completedAt: LocalDateTime,

    /** 오늘 전체 진행률 (0.0 ~ 1.0) - 도넛 차트용 */
    val todayProgressRate: Double,

    /** 현재 스트릭 연속 일수 - 🔥 카운트용 */
    val currentStreak: Int,

    /** 시간 왜곡 감지 결과 (완료 시 실시간 업데이트) */
    val timeDistortion: TimeDistortionInfo?,
)