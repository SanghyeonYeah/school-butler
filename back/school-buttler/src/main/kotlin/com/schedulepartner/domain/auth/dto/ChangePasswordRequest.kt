package com.schedulepartner.domain.auth.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * PATCH /api/auth/password
 * 비밀번호 변경 후 전체 기기 로그아웃 처리
 */
data class ChangePasswordRequest(

    @field:NotBlank(message = "현재 비밀번호를 입력해주세요")
    val currentPassword: String,

    @field:NotBlank(message = "새 비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이내여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "비밀번호는 영문자와 숫자를 포함해야 합니다"
    )
    val newPassword: String,
)