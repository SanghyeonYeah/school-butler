package com.schedulepartner.domain.recovery.dto

import com.schedulepartner.domain.recovery.entity.RecoveryTriggerType

/**
 * POST /api/recovery/apply 요청
 * [전체 적용] 또는 [개별 수정] 후 적용
 */
data class RecoveryApplyRequest(
    val planId: Long,

    /**
     * 적용할 항목 목록
     * 사용자가 개별 수정한 경우 수정된 시간 포함
     */
    val items: List<RecoveryApplyItem>,
)

data class RecoveryApplyItem(
    val taskId: Long,
    val startTime: String,       // "HH:mm" - 사용자가 수정 가능
    val endTime: String,
)

/**
 * POST /api/recovery/apply 응답
 */
data class RecoveryApplyResponse(
    val planId: Long,
    val appliedCount: Int,
    val message: String,
)