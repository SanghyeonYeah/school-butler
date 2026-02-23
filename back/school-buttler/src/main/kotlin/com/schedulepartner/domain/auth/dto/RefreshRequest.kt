package com.schedulepartner.domain.auth.dto

import jakarta.validation.constraints.NotBlank

/**
 * POST /api/auth/refresh
 */
data class RefreshRequest(

    @field:NotBlank(message = "Refresh Token을 입력해주세요")
    val refreshToken: String,
)