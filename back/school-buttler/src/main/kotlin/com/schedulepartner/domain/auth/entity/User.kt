package com.schedulepartner.domain.auth.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_user_email", columnList = "email", unique = true),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(nullable = false, length = 200)
    var password: String,                   // BCrypt 해시

    @Column(nullable = false, length = 20)
    var nickname: String,

    /**
     * Refresh Token - 재발급 시 서버 검증용
     * null = 로그아웃 상태
     */
    @Column(name = "refresh_token", length = 500)
    var refreshToken: String? = null,

    /**
     * 기본 취침 시각 - 복구 엔진 배치 기준
     * 기본값: 23:30
     */
    @Column(name = "default_bedtime", nullable = false, length = 5)
    var defaultBedtime: String = "23:30",

    /**
     * 선택한 캐릭터 컨셉
     * 집중형 | 회복형 | 마무리형 | 자유형
     */
    @Column(name = "concept", length = 10)
    var concept: String = "자유형",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun logout() {
        refreshToken = null
    }
}