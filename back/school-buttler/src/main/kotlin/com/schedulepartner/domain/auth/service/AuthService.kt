package com.schedulepartner.domain.auth.service

import com.schedulepartner.common.exception.CustomException
import com.schedulepartner.common.exception.ErrorCode
import com.schedulepartner.domain.auth.dto.*
import com.schedulepartner.domain.auth.entity.User
import com.schedulepartner.domain.auth.repository.UserRepository
import com.schedulepartner.infrastructure.jwt.JwtProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {
    private val log = LoggerFactory.getLogger(AuthService::class.java)

    // ── 회원가입 ───────────────────────────────────────────────────────────────

    @Transactional
    fun signup(req: SignupRequest): SignupResponse {
        if (userRepository.existsByEmail(req.email)) {
            throw CustomException(ErrorCode.USER_001, "이미 사용 중인 이메일입니다: ${req.email}")
        }

        val user = userRepository.save(
            User(
                email    = req.email,
                password = passwordEncoder.encode(req.password),
                nickname = req.nickname,
            )
        )

        log.info("[Auth] 회원가입: userId=${user.id}, email=${user.email}")
        return SignupResponse(userId = user.id, email = user.email, nickname = user.nickname)
    }

    // ── 로그인 ─────────────────────────────────────────────────────────────────

    @Transactional
    fun login(req: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(req.email)
            ?: throw CustomException(ErrorCode.AUTH_001, "이메일 또는 비밀번호가 올바르지 않습니다")

        if (!passwordEncoder.matches(req.password, user.password)) {
            throw CustomException(ErrorCode.AUTH_001, "이메일 또는 비밀번호가 올바르지 않습니다")
        }

        if (!user.isActive) {
            throw CustomException(ErrorCode.AUTH_002, "비활성화된 계정입니다")
        }

        val accessToken  = jwtProvider.generateAccessToken(user.id, user.email)
        val refreshToken = jwtProvider.generateRefreshToken(user.id)

        userRepository.updateRefreshToken(user.id, refreshToken)

        log.info("[Auth] 로그인: userId=${user.id}")
        return buildTokenResponse(accessToken, refreshToken, user)
    }

    // ── 토큰 재발급 ────────────────────────────────────────────────────────────

    @Transactional
    fun refresh(req: RefreshRequest): TokenResponse {
        // Refresh Token 유효성 검사
        if (!jwtProvider.validate(req.refreshToken)) {
            throw CustomException(ErrorCode.AUTH_001, "만료되거나 유효하지 않은 Refresh Token입니다")
        }

        // DB에 저장된 Refresh Token과 비교 (탈취 방지)
        val userId = jwtProvider.getUserId(req.refreshToken)
        val user = userRepository.findById(userId).orElseThrow {
            CustomException(ErrorCode.USER_001)
        }

        if (user.refreshToken != req.refreshToken) {
            // 토큰 불일치 → 탈취 의심 → 모든 토큰 무효화
            userRepository.clearRefreshToken(user.id)
            log.warn("[Auth] Refresh Token 불일치 - 탈취 의심: userId=${user.id}")
            throw CustomException(ErrorCode.AUTH_002, "유효하지 않은 Refresh Token입니다")
        }

        val newAccessToken  = jwtProvider.generateAccessToken(user.id, user.email)
        val newRefreshToken = jwtProvider.generateRefreshToken(user.id)

        userRepository.updateRefreshToken(user.id, newRefreshToken)

        log.info("[Auth] 토큰 재발급: userId=${user.id}")
        return buildTokenResponse(newAccessToken, newRefreshToken, user)
    }

    // ── 로그아웃 ───────────────────────────────────────────────────────────────

    @Transactional
    fun logout(userId: Long) {
        userRepository.clearRefreshToken(userId)
        log.info("[Auth] 로그아웃: userId=$userId")
    }

    // ── 내 정보 조회 ───────────────────────────────────────────────────────────

    fun getMe(userId: Long): UserInfo {
        val user = findUserOrThrow(userId)
        return user.toUserInfo()
    }

    // ── 프로필 수정 ────────────────────────────────────────────────────────────

    @Transactional
    fun updateProfile(userId: Long, req: UpdateProfileRequest): UserInfo {
        val user = findUserOrThrow(userId)

        req.nickname?.let { user.nickname = it }
        req.concept?.let { user.concept = it }
        req.defaultBedtime?.let { user.defaultBedtime = it }

        log.info("[Auth] 프로필 수정: userId=$userId")
        return user.toUserInfo()
    }

    // ── 비밀번호 변경 ──────────────────────────────────────────────────────────

    @Transactional
    fun changePassword(userId: Long, req: ChangePasswordRequest) {
        val user = findUserOrThrow(userId)

        if (!passwordEncoder.matches(req.currentPassword, user.password)) {
            throw CustomException(ErrorCode.AUTH_001, "현재 비밀번호가 올바르지 않습니다")
        }

        user.password = passwordEncoder.encode(req.newPassword)
        // 비밀번호 변경 시 모든 기기 로그아웃 처리
        userRepository.clearRefreshToken(user.id)

        log.info("[Auth] 비밀번호 변경: userId=$userId")
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────────────

    private fun findUserOrThrow(userId: Long): User =
        userRepository.findById(userId).orElseThrow {
            CustomException(ErrorCode.USER_001)
        }

    private fun buildTokenResponse(
        accessToken: String,
        refreshToken: String,
        user: User,
    ): TokenResponse = TokenResponse(
        accessToken           = accessToken,
        refreshToken          = refreshToken,
        accessTokenExpiresIn  = jwtProvider.getAccessTokenExpiryMs(),
        refreshTokenExpiresIn = jwtProvider.getRefreshTokenExpiryMs(),
        user                  = user.toUserInfo(),
    )

    private fun User.toUserInfo() = UserInfo(
        userId         = id,
        email          = email,
        nickname       = nickname,
        concept        = concept,
        defaultBedtime = defaultBedtime,
    )
}