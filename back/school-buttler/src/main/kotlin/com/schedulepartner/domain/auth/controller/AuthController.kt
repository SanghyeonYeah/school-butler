package com.schedulepartner.domain.auth.controller

import com.schedulepartner.common.response.ApiResponse
import com.schedulepartner.common.util.JwtUtil
import com.schedulepartner.domain.auth.dto.*
import com.schedulepartner.domain.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {

    /**
     * POST /api/auth/signup
     * 회원가입
     */
    @PostMapping("/signup")
    fun signup(
        @Valid @RequestBody req: SignupRequest,
    ): ResponseEntity<ApiResponse<SignupResponse>> {
        val result = authService.signup(req)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result))
    }

    /**
     * POST /api/auth/login
     * 로그인 - JWT Access + Refresh Token 발급
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody req: LoginRequest,
    ): ResponseEntity<ApiResponse<TokenResponse>> =
        ResponseEntity.ok(ApiResponse.success(authService.login(req)))

    /**
     * POST /api/auth/refresh
     * Access Token 재발급
     */
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody req: RefreshRequest,
    ): ResponseEntity<ApiResponse<TokenResponse>> =
        ResponseEntity.ok(ApiResponse.success(authService.refresh(req)))

    /**
     * POST /api/auth/logout
     * 로그아웃 - 서버 Refresh Token 무효화
     */
    @PostMapping("/logout")
    fun logout(): ResponseEntity<ApiResponse<Unit>> {
        authService.logout(JwtUtil.currentUserId())
        return ResponseEntity.ok(ApiResponse.success())
    }

    /**
     * GET /api/auth/me
     * 내 정보 조회
     */
    @GetMapping("/me")
    fun getMe(): ResponseEntity<ApiResponse<UserInfo>> =
        ResponseEntity.ok(
            ApiResponse.success(authService.getMe(JwtUtil.currentUserId()))
        )

    /**
     * PATCH /api/auth/me
     * 프로필 수정 (닉네임 / 컨셉 / 취침 시각)
     */
    @PatchMapping("/me")
    fun updateProfile(
        @Valid @RequestBody req: UpdateProfileRequest,
    ): ResponseEntity<ApiResponse<UserInfo>> =
        ResponseEntity.ok(
            ApiResponse.success(
                authService.updateProfile(JwtUtil.currentUserId(), req)
            )
        )

    /**
     * PATCH /api/auth/password
     * 비밀번호 변경 - 변경 후 전체 기기 로그아웃
     */
    @PatchMapping("/password")
    fun changePassword(
        @Valid @RequestBody req: ChangePasswordRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        authService.changePassword(JwtUtil.currentUserId(), req)
        return ResponseEntity.ok(ApiResponse.success())
    }
}