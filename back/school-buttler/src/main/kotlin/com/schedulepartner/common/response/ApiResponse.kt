package com.schedulepartner.common.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 모든 API 응답의 공통 래퍼
 *
 * 성공: ApiResponse.success(data)
 * 실패: ApiResponse.error(code, message)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(success = true, data = data)

        fun <T> success(): ApiResponse<T> =
            ApiResponse(success = true)

        fun <T> error(code: String, message: String): ApiResponse<T> =
            ApiResponse(
                success = false,
                error = ErrorDetail(code = code, message = message),
            )
    }
}

data class ErrorDetail(
    val code: String,
    val message: String,
)