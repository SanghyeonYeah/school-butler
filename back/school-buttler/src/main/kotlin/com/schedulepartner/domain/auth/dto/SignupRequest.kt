package com.schedulepartner.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * POST /api/auth/signup
 */
data class SignupRequest(

    @field:NotBlank(message = "이메일을 입력해주세요")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요")
    @field:Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이내여야 합니다")
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
        message = "비밀번호는 영문자와 숫자를 포함해야 합니다"
    )
    val password: String,

    @field:NotBlank(message = "닉네임을 입력해주세요")
    @field:Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이내여야 합니다")
    val nickname: String,
)