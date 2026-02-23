package com.schedulepartner.domain.auth.dto

/**
 * GET /api/auth/me 응답
 * TokenResponse 내부에도 포함됨
 */
data class UserInfo(
    val userId: Long,
    val email: String,
    val nickname: String,
    val concept: String,
    val defaultBedtime: String,
)