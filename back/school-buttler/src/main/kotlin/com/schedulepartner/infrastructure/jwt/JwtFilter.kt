package com.schedulepartner.infrastructure.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * JWT 인증 필터
 * - Authorization: Bearer {accessToken} 헤더 파싱
 * - 토큰 검증 후 SecurityContext 에 Authentication 등록
 *
 * TODO: JwtProvider 구현 후 로직 완성
 */
@Component
class JwtFilter(
    private val jwtProvider: JwtProvider,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null && jwtProvider.validate(token)) {
            val authentication = jwtProvider.getAuthentication(token)
            org.springframework.security.core.context.SecurityContextHolder
                .getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization") ?: return null
        return if (bearer.startsWith("Bearer ")) bearer.substring(7) else null
    }
}