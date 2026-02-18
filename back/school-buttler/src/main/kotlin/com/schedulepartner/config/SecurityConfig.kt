package com.schedulepartner.config

import com.schedulepartner.infrastructure.jwt.JwtFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val rateLimitFilter: RateLimitFilter,
) {

    companion object {
        /** 인증 없이 접근 가능한 엔드포인트 */
        private val PUBLIC_ENDPOINTS = arrayOf(
            "/api/auth/signup",
            "/api/auth/login",
            "/actuator/health",
        )
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // CSRF: JWT Stateless 방식이므로 비활성화
            .csrf { it.disable() }

            // CORS 설정
            .cors { it.configurationSource(corsConfigurationSource()) }

            // 세션 미사용 (JWT Stateless)
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // 엔드포인트 접근 권한
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*PUBLIC_ENDPOINTS).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // Preflight
                    .anyRequest().authenticated()
            }

            // 필터 순서: RateLimit → JWT → Security
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)

            // 기본 로그인 폼 비활성화
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }

    /**
     * BCrypt: API 명세 보안 요구사항 준수
     * strength 기본값(10) 사용 - 필요 시 12로 상향 가능
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    /**
     * CORS 설정
     * - 허용 Origin 은 application.yml 의 cors.allowed-origins 로 분리 권장
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")          // 운영 환경에서는 도메인 명시
            allowedMethods = listOf("GET", "POST", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
        }
    }
}