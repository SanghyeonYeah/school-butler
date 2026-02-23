package com.schedulepartner.domain.auth.repository

import com.schedulepartner.domain.auth.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean

    /** Refresh Token 으로 사용자 조회 - 토큰 재발급 검증용 */
    fun findByRefreshToken(refreshToken: String): User?

    /** Refresh Token 업데이트 - 로그인/재발급 시 */
    @Modifying
    @Query("UPDATE User u SET u.refreshToken = :token WHERE u.id = :id")
    fun updateRefreshToken(
        @Param("id") id: Long,
        @Param("token") token: String?,
    )

    /** 로그아웃 - Refresh Token 초기화 */
    @Modifying
    @Query("UPDATE User u SET u.refreshToken = null WHERE u.id = :id")
    fun clearRefreshToken(@Param("id") id: Long)
}