package com.schedulepartner.common.util

import com.schedulepartner.common.exception.CustomException
import com.schedulepartner.common.exception.ErrorCode
import org.springframework.security.core.context.SecurityContextHolder

/**
 * SecurityContext 에서 인증 정보를 꺼내는 유틸
 *
 * JwtFilter 가 SecurityContext 에 userId 를 subject 로 등록했으므로
 * 컨트롤러에서 @RequestHeader 없이 userId 를 꺼낼 수 있음
 *
 * 사용:
 *   val userId = JwtUtil.currentUserId()
 */
object JwtUtil {

    /**
     * 현재 요청의 인증된 userId 반환
     * 인증되지 않은 상태에서 호출 시 AUTH-002 예외
     */
    fun currentUserId(): Long {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw CustomException(ErrorCode.AUTH_002)

        return auth.principal.toString().toLongOrNull()
            ?: throw CustomException(ErrorCode.AUTH_002, "userId 파싱 실패: ${auth.principal}")
    }

    /**
     * 현재 요청이 인증된 상태인지 확인
     */
    fun isAuthenticated(): Boolean =
        runCatching { currentUserId() }.isSuccess
}