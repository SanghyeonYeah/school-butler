package com.schedulepartner.domain.task.entity

/**
 * 일정 상태
 * PENDING     → 예정 (기본값)
 * IN_PROGRESS → 진행 중 (집중 세션 시작 시)
 * DONE        → 완료 (체크 시 → 1.1초 도파민 체인 트리거)
 * SKIPPED     → 건너뜀 (복구 엔진이 재배치 후 원본 상태)
 */
enum class TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    SKIPPED,
}