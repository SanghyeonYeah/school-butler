package com.schedulepartner.domain.recovery.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.schedulepartner.common.exception.CustomException
import com.schedulepartner.common.exception.ErrorCode
import com.schedulepartner.domain.recovery.dto.*
import com.schedulepartner.domain.recovery.entity.RecoveryPlan
import com.schedulepartner.domain.recovery.entity.RecoveryTriggerType
import com.schedulepartner.domain.recovery.repository.RecoveryRepository
import com.schedulepartner.domain.task.repository.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class RecoveryService(
    private val recoveryEngine: RecoveryEngine,
    private val taskRepository: TaskRepository,
    private val recoveryRepository: RecoveryRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(RecoveryService::class.java)
    private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

    // ── 복구 플랜 생성 ─────────────────────────────────────────────────────────

    /**
     * POST /api/recovery/plan
     *
     * 트리거 조건 (기획서):
     *   1) 미완료 일정 3개 이상
     *   2) 17:00 이후
     *   3) 사용자 수동 요청
     */
    @Transactional
    fun createPlan(userId: Long, req: RecoveryPlanRequest): RecoveryPlanResponse {
        val targetDate = req.targetDate
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.now()

        val (startOfDay, endOfDay) = targetDate.let {
            it.atStartOfDay() to it.plusDays(1).atStartOfDay()
        }

        // 미완료 일정 조회
        val incompleteTasks = taskRepository.findIncompleteTasks(userId, startOfDay, endOfDay)

        // 복구 조건 검증
        val now = LocalTime.now()
        val isAfter17 = now.isAfter(LocalTime.of(17, 0))
        val hasEnoughIncomplete = incompleteTasks.size >= 3

        if (!isAfter17 && !hasEnoughIncomplete) {
            throw CustomException(
                ErrorCode.RECOVERY_001,
                "복구 조건 미충족: 미완료 일정 ${incompleteTasks.size}개 (3개 이상 필요), " +
                        "현재 시각 ${now.format(TIME_FMT)} (17:00 이후 필요)"
            )
        }

        val triggerType = when {
            isAfter17 && hasEnoughIncomplete -> RecoveryTriggerType.AFTER_17
            isAfter17 -> RecoveryTriggerType.AFTER_17
            else -> RecoveryTriggerType.INCOMPLETE_COUNT
        }

        // 복구 엔진 실행
        val bedtime = LocalTime.parse(req.bedtime, TIME_FMT)
        val startTime = now.let {
            // 현재 시각을 5분 단위로 올림
            LocalTime.of(it.hour, ((it.minute / 5) + 1) * 5 % 60)
                .let { t -> if (it.minute >= 55) LocalTime.of(it.hour + 1, 0) else t }
        }

        val planItems = recoveryEngine.buildPlan(incompleteTasks, startTime, bedtime)

        if (planItems.isEmpty()) {
            throw CustomException(ErrorCode.RECOVERY_001, "취침 시간까지 배치 가능한 일정이 없습니다")
        }

        val totalDuration = planItems
            .filter { it.itemType == RecoveryItemType.TASK }
            .sumOf { it.durationMinutes }

        val estimatedEnd = recoveryEngine.calcEstimatedEndTime(planItems)
        val bufferMinutes = recoveryEngine.calcBufferMinutes(estimatedEnd, bedtime)

        // 플랜 저장 (이력 관리)
        val planJson = objectMapper.writeValueAsString(planItems)
        val saved = recoveryRepository.save(
            RecoveryPlan(
                userId = userId,
                targetDate = targetDate,
                triggerType = triggerType,
                planJson = planJson,
                totalDuration = totalDuration,
            )
        )

        log.info("[Recovery] 플랜 생성: userId=$userId, planId=${saved.id}, items=${planItems.size}개, totalDuration=${totalDuration}분")

        return RecoveryPlanResponse(
            planId = saved.id,
            triggerType = triggerType,
            totalDuration = totalDuration,
            plan = planItems,
            estimatedEndTime = estimatedEnd,
            bufferMinutes = bufferMinutes,
        )
    }

    // ── 복구 플랜 적용 ─────────────────────────────────────────────────────────

    /**
     * POST /api/recovery/apply
     * 사용자가 [전체 적용] or [개별 수정] 후 적용
     * → 해당 Task 의 scheduledAt 을 복구 플랜 시간으로 업데이트
     * → isRecovered = true 마킹
     */
    @Transactional
    fun applyPlan(userId: Long, req: RecoveryApplyRequest): RecoveryApplyResponse {
        val plan = recoveryRepository.findByIdAndUserId(req.planId, userId)
            ?: throw CustomException(ErrorCode.RECOVERY_001, "복구 플랜을 찾을 수 없습니다")

        if (plan.isApplied) {
            throw CustomException(ErrorCode.RECOVERY_001, "이미 적용된 복구 플랜입니다")
        }

        var appliedCount = 0
        req.items.forEach { item ->
            val task = taskRepository.findByIdAndUserId(item.taskId, userId) ?: return@forEach

            // scheduledAt 을 복구 플랜 시간으로 업데이트
            val newStartTime = LocalTime.parse(item.startTime, TIME_FMT)
            task.scheduledAt = LocalDateTime.of(plan.targetDate, newStartTime)
            task.isRecovered = true
            appliedCount++
        }

        plan.isApplied = true

        log.info("[Recovery] 플랜 적용: userId=$userId, planId=${req.planId}, appliedCount=$appliedCount")

        return RecoveryApplyResponse(
            planId = req.planId,
            appliedCount = appliedCount,
            message = "${appliedCount}개의 일정이 재배치되었습니다. 할 수 있어! 💪",
        )
    }

    // ── 수동 요청 (조건 검증 없이 강제 생성) ──────────────────────────────────

    @Transactional
    fun createManualPlan(userId: Long, req: RecoveryPlanRequest): RecoveryPlanResponse {
        val targetDate = req.targetDate
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.now()

        val (startOfDay, endOfDay) = targetDate.let {
            it.atStartOfDay() to it.plusDays(1).atStartOfDay()
        }

        val incompleteTasks = taskRepository.findIncompleteTasks(userId, startOfDay, endOfDay)
        if (incompleteTasks.isEmpty()) {
            throw CustomException(ErrorCode.RECOVERY_001, "미완료 일정이 없습니다")
        }

        val bedtime = LocalTime.parse(req.bedtime, TIME_FMT)
        val startTime = LocalTime.now().let {
            LocalTime.of(it.hour, ((it.minute / 5) + 1) * 5 % 60)
        }

        val planItems = recoveryEngine.buildPlan(incompleteTasks, startTime, bedtime)
        val totalDuration = planItems
            .filter { it.itemType == RecoveryItemType.TASK }
            .sumOf { it.durationMinutes }

        val estimatedEnd = recoveryEngine.calcEstimatedEndTime(planItems)
        val bufferMinutes = recoveryEngine.calcBufferMinutes(estimatedEnd, bedtime)

        val planJson = objectMapper.writeValueAsString(planItems)
        val saved = recoveryRepository.save(
            RecoveryPlan(
                userId = userId,
                targetDate = targetDate,
                triggerType = RecoveryTriggerType.MANUAL,
                planJson = planJson,
                totalDuration = totalDuration,
            )
        )

        log.info("[Recovery] 수동 플랜 생성: userId=$userId, planId=${saved.id}")

        return RecoveryPlanResponse(
            planId = saved.id,
            triggerType = RecoveryTriggerType.MANUAL,
            totalDuration = totalDuration,
            plan = planItems,
            estimatedEndTime = estimatedEnd,
            bufferMinutes = bufferMinutes,
        )
    }
}