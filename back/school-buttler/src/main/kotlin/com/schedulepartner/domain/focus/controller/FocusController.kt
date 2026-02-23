package com.schedulepartner.domain.focus.controller

import com.schedulepartner.common.response.ApiResponse
import com.schedulepartner.common.util.JwtUtil
import com.schedulepartner.domain.focus.dto.*
import com.schedulepartner.domain.focus.service.FocusService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/focus")
class FocusController(
    private val focusService: FocusService,
) {

    /**
     * POST /api/focus/start
     * 집중 세션 시작
     * 이미 진행 중인 세션 있으면 FOCUS-001 에러
     */
    @PostMapping("/start")
    fun start(
        @Valid @RequestBody req: FocusStartRequest,
    ): ResponseEntity<ApiResponse<FocusStartResponse>> =
        ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(focusService.start(JwtUtil.currentUserId(), req)))

    /**
     * POST /api/focus/{id}/pause
     * 일시정지 - 자리 비울 때
     */
    @PostMapping("/{id}/pause")
    fun pause(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<FocusSessionResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(focusService.pause(JwtUtil.currentUserId(), id))
        )

    /**
     * POST /api/focus/{id}/resume
     * 재개 - 일시정지 시간 누적 후 타이머 재시작
     */
    @PostMapping("/{id}/resume")
    fun resume(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<FocusSessionResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(focusService.resume(JwtUtil.currentUserId(), id))
        )

    /**
     * POST /api/focus/{id}/complete
     * 완료 - 도파민 체인 트리거 데이터 반환
     * taskCompleted=true 면 프론트에서 1.1초 완료 애니메이션 실행
     */
    @PostMapping("/{id}/complete")
    fun complete(
        @PathVariable id: Long,
        @RequestBody(required = false) req: FocusCompleteRequest?,
    ): ResponseEntity<ApiResponse<FocusCompleteResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(
                focusService.complete(JwtUtil.currentUserId(), id, req ?: FocusCompleteRequest())
            )
        )

    /**
     * POST /api/focus/{id}/abandon
     * 포기 - 부분 집중 시간만 기록, Task 완료 처리 없음
     */
    @PostMapping("/{id}/abandon")
    fun abandon(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<FocusSessionResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(focusService.abandon(JwtUtil.currentUserId(), id))
        )

    /**
     * GET /api/focus/{id}
     * 세션 상태 조회 - 클라이언트 타이머 동기화용
     * elapsedFocusSeconds 로 타이머 재동기화 가능
     */
    @GetMapping("/{id}")
    fun getSession(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<FocusSessionResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(focusService.getSession(JwtUtil.currentUserId(), id))
        )

    /**
     * GET /api/focus/today
     * 오늘 집중 세션 전체 + 요약
     * 홈 화면 집중 시간 도넛 차트용
     */
    @GetMapping("/today")
    fun getTodaySessions(): ResponseEntity<ApiResponse<TodayFocusResponse>> =
        ResponseEntity.ok(
            ApiResponse.success(focusService.getTodaySessions(JwtUtil.currentUserId()))
        )
}