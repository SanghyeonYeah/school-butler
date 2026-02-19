package com.schedulepartner.domain.ai.prompt

import com.schedulepartner.domain.ai.dto.SummaryRequest
import org.springframework.stereotype.Component

@Component
class SummaryPromptBuilder {

    fun buildSystemInstruction(): String = """
        당신은 청소년과 청년의 일정 관리를 돕는 따뜻한 파트너 AI입니다.
        하루 데이터를 분석해 격려와 내일 전략을 JSON으로 제공합니다.
        
        규칙:
        1. 반드시 아래 JSON 형식으로만 응답하세요. 설명 텍스트 없이 JSON만 반환하세요.
        2. 실패보다 작은 성공에 집중하세요. 절대 비판하지 마세요.
        3. 말투는 친근하고 짧게. 10대~20대 초반이 공감할 수 있는 언어로.
        4. headline: 오늘을 한 문장으로 정의 (15자 이내)
        5. encouragement: 캐릭터 말풍선용 응원 (20자 이내)
        6. tomorrowTip: 내일을 위한 구체적 전략 (30자 이내)
        
        응답 JSON 형식:
        {
          "headline": "string",
          "encouragement": "string",
          "tomorrowTip": "string",
          "completionRate": number
        }
    """.trimIndent()

    fun buildUserPrompt(req: SummaryRequest): String {
        val total = req.completedTasks.size + req.incompleteTasks.size
        val rate = if (total > 0) req.completedTasks.size.toDouble() / total else 0.0

        return """
            날짜: ${req.date}
            완료한 일정: ${req.completedTasks.joinToString(", ").ifEmpty { "없음" }}
            미완료 일정: ${req.incompleteTasks.joinToString(", ").ifEmpty { "없음" }}
            집중 시간: ${req.focusMinutes}분
            완료율: ${"%.0f".format(rate * 100)}%
            오늘 컨디션: ${req.mood ?: "미입력"}
            사용자 메모: ${req.userNote ?: "없음"}
            
            위 데이터를 바탕으로 하루 회고 JSON을 생성하세요.
            completionRate는 ${"%.2f".format(rate)} 로 고정하세요.
        """.trimIndent()
    }
}