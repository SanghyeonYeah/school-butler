package com.schedulepartner.domain.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * POST /api/auth/login
 */
data class LoginRequest(

    @field:NotBlank(message = "이메일을 입력해주세요")
    @field:Email(message = "올바른 이메일 형식이 아닙니다")
    val email: String,

    @field:NotBlank(message = "비밀번호를 입력해주세요")
    val password: String,
)

/**
 * POST /api/auth/login 응답
 * Access Token (30분) + Refresh Token (14일)
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val accessTokenExpiresIn: Long,     // ms
    val refreshTokenExpiresIn: Long,    // ms
    val user: UserInfo,
)