package com.schedulepartner.domain.ai.service

import com.schedulepartner.domain.ai.client.GeminiClient
import com.schedulepartner.domain.ai.dto.ParseRequest
import com.schedulepartner.domain.ai.dto.ParseResponse
import com.schedulepartner.domain.ai.dto.SummaryRequest
import com.schedulepartner.domain.ai.dto.SummaryResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AiService(
    private val geminiClient: GeminiClient,
) {
    private val log = LoggerFactory.getLogger(AiService::class.java)

    /**
     * 자연어 일정 파싱
     * ex) "내일 오후 7시 수학 복습 30분" → ParseResponse
     */
    fun parseSchedule(req: ParseRequest): ParseResponse {
        log.info("[AI] 자연어 파싱 요청: text='${req.text}', timezone=${req.timezone}")
        val result = geminiClient.parseSchedule(req.text, req.timezone)
        log.info("[AI] 파싱 완료: title='${result.title}', confidence=${result.confidence}")

        // confidence 낮으면 경고 로그 (프론트에서 재확인 유도 가능)
        if (result.confidence < 0.6) {
            log.warn("[AI] 파싱 신뢰도 낮음 (${result.confidence}): '${req.text}'")
        }

        return result
    }

    /**
     * 하루 회고 요약 생성
     */
    fun generateSummary(req: SummaryRequest): SummaryResponse {
        log.info("[AI] 회고 요약 요청: date=${req.date}, 완료=${req.completedTasks.size}개, 미완료=${req.incompleteTasks.size}개")
        val result = geminiClient.generateSummary(req)
        log.info("[AI] 회고 생성 완료: headline='${result.headline}'")
        return result
    }
}