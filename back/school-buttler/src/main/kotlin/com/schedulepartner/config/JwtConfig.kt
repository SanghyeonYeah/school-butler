package com.schedulepartner.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * application.yml 의 jwt 블록을 타입 안전하게 바인딩
 *
 * jwt:
 *   secret: ${JWT_SECRET}           # 256bit 이상 랜덤 문자열 (환경변수 주입)
 *   access-token-expiry-ms: 1800000  # 30분
 *   refresh-token-expiry-ms: 1209600000 # 14일
 */
@ConfigurationProperties(prefix = "jwt")
data class JwtConfig(
    val secret: String,
    val accessTokenExpiryMs: Long = 1_800_000L,          // 30분
    val refreshTokenExpiryMs: Long = 1_209_600_000L,     // 14일
)