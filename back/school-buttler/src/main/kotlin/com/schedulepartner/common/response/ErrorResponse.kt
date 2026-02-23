package com.schedulepartner.common.response

import com.schedulepartner.common.exception.ErrorCode

/**
 * 에러 응답 전용 DTO
 * GlobalExceptionHandler 에서 사용
 *
 * {
 *   "code": "AUTH-001",
 *   "message": "토큰이 만료되었습니다",
 *   "timestamp": 1707648000000
 * }
 */
data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        fun of(errorCode: ErrorCode): ErrorResponse =
            ErrorResponse(
                code = errorCode.code,
                message = errorCode.message,
            )

        fun of(errorCode: ErrorCode, message: String): ErrorResponse =
            ErrorResponse(
                code = errorCode.code,
                message = message,
            )
    }
}