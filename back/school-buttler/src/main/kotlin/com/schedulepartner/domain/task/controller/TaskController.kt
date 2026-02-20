package com.schedulepartner.domain.task.controller

import com.schedulepartner.domain.task.dto.*
import com.schedulepartner.domain.task.service.TaskService
import com.schedulepartner.infrastructure.jwt.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/tasks")
class TaskController(
    private val taskService: TaskService,
    private val jwtProvider: JwtProvider,
) {

    /**
     * POST /api/tasks
     * 일정 생성
     */
    @PostMapping
    fun createTask(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody req: TaskCreateRequest,
    ): ResponseEntity<TaskCreateResponse> {
        val userId = extractUserId(token)
        val result = taskService.createTask(userId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    /**
     * GET /api/tasks/today
     * 오늘 일정 전체 조회
     */
    @GetMapping("/today")
    fun getTodayTasks(
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<List<TaskResponse>> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(taskService.getTodayTasks(userId))
    }

    /**
     * GET /api/tasks/{id}
     * 단일 일정 조회
     */
    @GetMapping("/{id}")
    fun getTask(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
    ): ResponseEntity<TaskResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(taskService.getTask(userId, id))
    }

    /**
     * PATCH /api/tasks/{id}
     * 일정 수정 (변경할 필드만 전송)
     */
    @PatchMapping("/{id}")
    fun updateTask(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
        @Valid @RequestBody req: TaskUpdateRequest,
    ): ResponseEntity<TaskResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(taskService.updateTask(userId, id, req))
    }

    /**
     * DELETE /api/tasks/{id}
     * 일정 삭제
     */
    @DeleteMapping("/{id}")
    fun deleteTask(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
    ): ResponseEntity<Unit> {
        val userId = extractUserId(token)
        taskService.deleteTask(userId, id)
        return ResponseEntity.noContent().build()
    }

    /**
     * POST /api/tasks/{id}/complete
     * 일정 완료 처리 → 1.1초 도파민 체인 트리거용 데이터 반환
     *
     * Body (선택): { "actualMinutes": 42 }
     * actualMinutes 전달 시 시간 왜곡 감지 데이터 축적
     */
    @PostMapping("/{id}/complete")
    fun completeTask(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
        @RequestBody(required = false) body: CompleteRequestBody?,
    ): ResponseEntity<TaskCompleteResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(taskService.completeTask(userId, id, body?.actualMinutes))
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private fun extractUserId(token: String): Long =
        jwtProvider.getUserId(token.removePrefix("Bearer ").trim())
}

data class CompleteRequestBody(
    val actualMinutes: Int? = null,
)