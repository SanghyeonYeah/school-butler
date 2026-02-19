package com.schedulepartner.domain.ai.controller

import com.schedulepartner.domain.ai.dto.ParseRequest
import com.schedulepartner.domain.ai.dto.ParseResponse
import com.schedulepartner.domain.ai.dto.SummaryRequest
import com.schedulepartner.domain.ai.dto.SummaryResponse
import com.schedulepartner.domain.ai.service.AiService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ai")
class AiController(
    private val aiService: AiService,
) {

    /**
     * POST /api/ai/parse
     * 자연어 텍스트를 일정 데이터로 파싱
     *
     * Request:  { "text": "내일 오후 7시 수학 복습 30분" }
     * Response: { "title": "수학 복습", "scheduledAt": "...", "expectedMinutes": 30, ... }
     */
    @PostMapping("/parse")
    fun parse(
        @Valid @RequestBody req: ParseRequestBody,
    ): ResponseEntity<ParseResponse> {
        val result = aiService.parseSchedule(
            ParseRequest(text = req.text, timezone = req.timezone)
        )
        return ResponseEntity.ok(result)
    }

    /**
     * POST /api/ai/summary
     * 하루 완료/미완료 데이터를 기반으로 회고 요약 생성
     */
    @PostMapping("/summary")
    fun summary(
        @Valid @RequestBody req: SummaryRequest,
    ): ResponseEntity<SummaryResponse> {
        val result = aiService.generateSummary(req)
        return ResponseEntity.ok(result)
    }
}

// ── Controller 전용 Request Body (Validation 포함) ──────────────────────────────

data class ParseRequestBody(
    @field:NotBlank(message = "파싱할 텍스트를 입력해주세요")
    @field:Size(max = 200, message = "입력은 200자 이내여야 합니다")
    val text: String,

    val timezone: String = "Asia/Seoul",
)