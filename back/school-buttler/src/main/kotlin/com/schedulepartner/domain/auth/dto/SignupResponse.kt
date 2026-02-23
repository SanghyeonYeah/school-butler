package com.schedulepartner.domain.auth.dto

/**
 * POST /api/auth/signup 응답
 */
data class SignupResponse(
    val userId: Long,
    val email: String,
    val nickname: String,
)