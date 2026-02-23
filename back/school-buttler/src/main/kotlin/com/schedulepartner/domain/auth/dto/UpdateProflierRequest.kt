package com.schedulepartner.domain.auth.dto

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * PATCH /api/auth/me
 * 변경할 필드만 전송 (null이면 기존 값 유지)
 */
data class UpdateProfileRequest(

    @field:Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이내여야 합니다")
    val nickname: String? = null,

    @field:Pattern(
        regexp = "집중형|회복형|마무리형|자유형",
        message = "컨셉은 집중형/회복형/마무리형/자유형 중 하나여야 합니다"
    )
    val concept: String? = null,

    @field:Pattern(
        regexp = "\\d{2}:\\d{2}",
        message = "취침 시각 형식은 HH:mm 이어야 합니다"
    )
    val defaultBedtime: String? = null,
)