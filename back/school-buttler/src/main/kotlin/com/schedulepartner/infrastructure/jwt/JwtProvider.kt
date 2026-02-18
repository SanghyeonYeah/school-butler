package com.schedulepartner.infrastructure.jwt

import com.schedulepartner.config.JwtConfig
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.util.Date

/**
 * JWT 생성 / 검증 / 파싱 담당
 * - JJWT 라이브러리 사용
 */
@Component
class JwtProvider(
    private val jwtConfig: JwtConfig,
) {
    private val key by lazy {
        Keys.hmacShaKeyFor(jwtConfig.secret.toByteArray())
    }

    /** Access Token 생성 */
    fun generateAccessToken(userId: Long, email: String): String =
        Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + jwtConfig.accessTokenExpiryMs))
            .signWith(key)
            .compact()

    /** Refresh Token 생성 */
    fun generateRefreshToken(userId: Long): String =
        Jwts.builder()
            .subject(userId.toString())
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + jwtConfig.refreshTokenExpiryMs))
            .signWith(key)
            .compact()

    /** 토큰 유효성 검사 */
    fun validate(token: String): Boolean = runCatching { parseClaims(token) }.isSuccess

    /** 토큰 → Authentication 객체 변환 */
    fun getAuthentication(token: String): Authentication {
        val claims = parseClaims(token)
        val userId = claims.subject
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        return UsernamePasswordAuthenticationToken(userId, token, authorities)
    }

    /** userId 추출 */
    fun getUserId(token: String): Long =
        parseClaims(token).subject.toLong()

    private fun parseClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
}