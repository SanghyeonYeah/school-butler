package com.schedulepartner.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val status: HttpStatus,
    val message: String,
) {
    // Auth
    AUTH_001("AUTH-001", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    AUTH_002("AUTH-002", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),

    // User
    USER_001("USER-001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),

    // Task
    TASK_001("TASK-001", HttpStatus.NOT_FOUND, "일정을 찾을 수 없습니다"),
    TASK_002("TASK-002", HttpStatus.BAD_REQUEST, "잘못된 일정 요청값입니다"),

    // Focus
    FOCUS_001("FOCUS-001", HttpStatus.BAD_REQUEST, "이미 진행 중인 집중 세션이 존재합니다"),

    // Recovery
    RECOVERY_001("RECOVERY-001", HttpStatus.BAD_REQUEST, "복구 조건이 충족되지 않았습니다"),

    // AI
    AI_001("AI-001", HttpStatus.INTERNAL_SERVER_ERROR, "AI 응답 파싱에 실패했습니다"),
    AI_002("AI-002", HttpStatus.BAD_GATEWAY, "Gemini API 호출에 실패했습니다"),
    AI_003("AI-003", HttpStatus.REQUEST_TIMEOUT, "Gemini API 응답 시간이 초과되었습니다"),

    // Common
    COMMON_500("COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),
}