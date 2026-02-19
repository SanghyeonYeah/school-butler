package com.schedulepartner.domain.ai.prompt

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class ParsePromptBuilder {

    fun buildSystemInstruction(): String = """
        당신은 일정 파싱 전문 AI입니다.
        사용자의 자연어 입력을 분석해 일정 정보를 JSON으로 추출합니다.
        
        규칙:
        1. 반드시 아래 JSON 형식으로만 응답하세요. 설명 텍스트는 절대 포함하지 마세요.
        2. 시간이 명확하지 않으면 오늘 기준으로 가장 합리적인 시간을 추론하세요.
        3. 예상 시간이 명시되지 않으면 태그(과목/활동)에 따라 30~60분으로 추정하세요.
        4. confidence: 입력이 명확할수록 1.0에 가깝게, 추론이 많을수록 0.0에 가깝게.
        
        응답 JSON 형식:
        {
          "title": "string",
          "scheduledAt": "YYYY-MM-DDTHH:mm:00",
          "expectedMinutes": number,
          "tags": ["string"],
          "confidence": number
        }
    """.trimIndent()

    fun buildUserPrompt(text: String, timezone: String): String {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return """
            오늘 날짜: $today
            타임존: $timezone
            사용자 입력: "$text"
            
            위 입력을 파싱해 JSON으로 반환하세요.
        """.trimIndent()
    }
}