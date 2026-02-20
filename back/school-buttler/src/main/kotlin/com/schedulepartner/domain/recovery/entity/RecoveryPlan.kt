package com.schedulepartner.domain.recovery.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 복구 엔진이 생성한 플랜 이력
 * - 플랜 생성 → 사용자가 [전체 적용] or [개별 수정] 선택
 * - 적용 후 isApplied = true
 */
@Entity
@Table(
    name = "recovery_plans",
    indexes = [
        Index(name = "idx_recovery_user_date", columnList = "user_id, target_date"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class RecoveryPlan(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    /** 복구 대상 날짜 */
    @Column(name = "target_date", nullable = false)
    val targetDate: LocalDate,

    /**
     * 복구 트리거 사유
     * INCOMPLETE_COUNT: 미완료 3개 이상
     * AFTER_17: 17:00 이후
     * MANUAL: 사용자 직접 요청
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    val triggerType: RecoveryTriggerType,

    /** 복구 플랜 항목들 (JSON 직렬화) */
    @Column(name = "plan_json", nullable = false, columnDefinition = "TEXT")
    val planJson: String,

    /** 총 예상 소요 시간 (분) */
    @Column(name = "total_duration", nullable = false)
    val totalDuration: Int,

    /** 사용자가 플랜을 적용했는지 여부 */
    @Column(name = "is_applied", nullable = false)
    var isApplied: Boolean = false,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)

enum class RecoveryTriggerType {
    INCOMPLETE_COUNT,   // 미완료 3개 이상
    AFTER_17,           // 17:00 이후
    MANUAL,             // 사용자 직접 요청
}