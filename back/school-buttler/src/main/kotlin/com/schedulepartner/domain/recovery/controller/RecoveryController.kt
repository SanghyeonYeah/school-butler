package com.schedulepartner.domain.recovery.controller

import com.schedulepartner.domain.recovery.dto.*
import com.schedulepartner.domain.recovery.service.RecoveryService
import com.schedulepartner.infrastructure.jwt.JwtProvider
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/recovery")
class RecoveryController(
    private val recoveryService: RecoveryService,
    private val jwtProvider: JwtProvider,
) {

    /**
     * POST /api/recovery/plan
     * 복구 플랜 생성
     *
     * 트리거 조건:
     *   - 미완료 일정 3개 이상 OR 17:00 이후
     *   - 조건 미충족 시 RECOVERY-001 에러
     *
     * Response: 바텀시트에 표시할 복구 플랜
     */
    @PostMapping("/plan")
    fun createPlan(
        @RequestHeader("Authorization") token: String,
        @RequestBody req: RecoveryPlanRequest,
    ): ResponseEntity<RecoveryPlanResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(recoveryService.createPlan(userId, req))
    }

    /**
     * POST /api/recovery/plan/manual
     * 수동 복구 플랜 생성 (조건 검증 없이 강제 요청)
     * 사용자가 하단 복구 버튼을 직접 탭할 때 사용
     */
    @PostMapping("/plan/manual")
    fun createManualPlan(
        @RequestHeader("Authorization") token: String,
        @RequestBody req: RecoveryPlanRequest,
    ): ResponseEntity<RecoveryPlanResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(recoveryService.createManualPlan(userId, req))
    }

    /**
     * POST /api/recovery/apply
     * 복구 플랜 적용
     *
     * [전체 적용]: 서버가 생성한 planItems 그대로 전달
     * [개별 수정]: 사용자가 수정한 startTime/endTime 전달
     */
    @PostMapping("/apply")
    fun applyPlan(
        @RequestHeader("Authorization") token: String,
        @Valid @RequestBody req: RecoveryApplyRequest,
    ): ResponseEntity<RecoveryApplyResponse> {
        val userId = extractUserId(token)
        return ResponseEntity.ok(recoveryService.applyPlan(userId, req))
    }

    private fun extractUserId(token: String): Long =
        jwtProvider.getUserId(token.removePrefix("Bearer ").trim())
}