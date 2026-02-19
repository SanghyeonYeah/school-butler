package com.schedulepartner.common.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val code: String,
    val message: String,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /** 앱 커스텀 예외 (AI-001, TASK-001 등) */
    @ExceptionHandler(CustomException::class)
    fun handleCustomException(ex: CustomException): ResponseEntity<ErrorResponse> {
        log.warn("[Exception] ${ex.errorCode.code}: ${ex.message}")
        return ResponseEntity
            .status(ex.errorCode.status)
            .body(ErrorResponse(code = ex.errorCode.code, message = ex.message))
    }

    /** @Valid 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse(code = "TASK-002", message = message))
    }

    /** 그 외 예상치 못한 예외 */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("[UnhandledException]", ex)
        return ResponseEntity
            .internalServerError()
            .body(ErrorResponse(code = "COMMON-500", message = "서버 내부 오류가 발생했습니다"))
    }
}