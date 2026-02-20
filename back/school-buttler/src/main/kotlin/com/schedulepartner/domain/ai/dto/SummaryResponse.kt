package com.schedulepartner.domain.ai.dto

/**
 * POST /api/ai/summary 응답
 * Gemini 가 생성한 하루 회고 요약
 */
data class SummaryResponse(

    /**
     * 오늘 하루를 한 문장으로 정의
     * ex) "실패를 학습으로, 작은 성공을 축하해"
     * 홈 화면 상단 슬로건 영역에 표시
     */
    val headline: String,

    /**
     * 캐릭터 말풍선용 짧은 응원 메시지
     * ex) "오늘도 수고했어! 🔥"
     */
    val encouragement: String,

    /**
     * 내일을 위한 AI 추천 전략
     * ex) "수학을 오전에 먼저 끝내보자!"
     */
    val tomorrowTip: String,

    /**
     * 오늘 일정 완료율 (0.0 ~ 1.0)
     * ex) 0.75 → 75% 완료
     */
    val completionRate: Double,
)