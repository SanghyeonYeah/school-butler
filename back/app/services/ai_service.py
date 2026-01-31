"""
AI Service Layer
Handles all AI-related business logic with proper error handling and validation
"""
import google.generativeai as genai
from datetime import datetime, timedelta, date
from typing import Dict, Any, List, Optional
import json
import re
import asyncio
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import settings
from app.core.exceptions import (
    GeminiTimeoutError,
    GeminiRateLimitError,
    GeminiParsingError,
    GeminiResponseError,
    InvalidDateTimeError,
    InvalidPriorityError,
    LowConfidenceError
)

# Configure Gemini
genai.configure(api_key=settings.GEMINI_API_KEY)


class AIService:
    """AI Service with improved error handling and validation"""
    
    def __init__(self):
        self.model = genai.GenerativeModel(settings.GEMINI_MODEL)
        self.timeout = 30  # seconds
        self.min_confidence = 0.7  # minimum confidence threshold
    
    async def parse_schedule_text(
        self,
        text: str,
        current_time: datetime
    ) -> Dict[str, Any]:
        """
        Parse natural language text into structured schedule data
        
        Raises:
            GeminiTimeoutError: If request times out
            GeminiRateLimitError: If rate limit exceeded
            GeminiParsingError: If parsing fails
            LowConfidenceError: If confidence too low
            InvalidDateTimeError: If parsed datetime is invalid
            InvalidPriorityError: If parsed priority is out of range
        """
        prompt = self._build_parse_prompt(text, current_time)
        
        try:
            # Call Gemini with timeout
            response = await asyncio.wait_for(
                self._generate_content_async(prompt),
                timeout=self.timeout
            )
            
            # Extract and validate JSON
            parsed_data = self._extract_json(response.text)
            
            # Validate confidence
            confidence = parsed_data.get('confidence', 0)
            if confidence < self.min_confidence:
                raise LowConfidenceError(confidence)
            
            # Validate and sanitize data
            validated_data = self._validate_parsed_schedule(parsed_data, current_time)
            
            return validated_data
            
        except asyncio.TimeoutError:
            raise GeminiTimeoutError()
        except Exception as e:
            if "429" in str(e) or "quota" in str(e).lower():
                raise GeminiRateLimitError()
            elif isinstance(e, (LowConfidenceError, InvalidDateTimeError, InvalidPriorityError)):
                raise
            else:
                raise GeminiParsingError(f"Failed to parse schedule: {str(e)}")
    
    async def generate_character_response(
        self,
        user_message: str,
        context: Dict[str, Any],
        personality: str = "friendly"
    ) -> str:
        """
        Generate character response with error handling
        
        Raises:
            GeminiTimeoutError: If request times out
            GeminiResponseError: If response is invalid
        """
        prompt = self._build_chat_prompt(user_message, context, personality)
        
        try:
            response = await asyncio.wait_for(
                self._generate_content_async(prompt),
                timeout=self.timeout
            )
            
            # Validate response
            if not response.text or len(response.text.strip()) == 0:
                raise GeminiResponseError()
            
            # Sanitize response (max 500 chars)
            sanitized = response.text.strip()[:500]
            
            return sanitized
            
        except asyncio.TimeoutError:
            raise GeminiTimeoutError()
        except GeminiResponseError:
            raise
        except Exception as e:
            if "429" in str(e):
                raise GeminiRateLimitError()
            raise GeminiResponseError()
    
    async def generate_daily_plan(
        self,
        target_date: date,
        tasks: List[Dict[str, Any]],
        preferences: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """
        Generate optimized daily plan
        
        Raises:
            GeminiTimeoutError: If request times out
            GeminiParsingError: If plan generation fails
        """
        prompt = self._build_plan_prompt(target_date, tasks, preferences)
        
        try:
            response = await asyncio.wait_for(
                self._generate_content_async(prompt),
                timeout=self.timeout
            )
            
            plan = self._extract_json(response.text)
            
            # Validate plan structure
            if not isinstance(plan, list):
                raise GeminiParsingError("Invalid plan format")
            
            # Validate each task in plan
            validated_plan = [
                self._validate_plan_task(task, target_date)
                for task in plan
            ]
            
            return validated_plan
            
        except asyncio.TimeoutError:
            raise GeminiTimeoutError()
        except Exception as e:
            if isinstance(e, GeminiTimeoutError):
                raise
            raise GeminiParsingError(f"Failed to generate plan: {str(e)}")
    
    async def generate_review_feedback(
        self,
        review_data: Dict[str, Any],
        historical_average: Dict[str, Any]
    ) -> str:
        """
        Generate personalized review feedback
        
        Raises:
            GeminiTimeoutError: If request times out
        """
        prompt = self._build_review_prompt(review_data, historical_average)
        
        try:
            response = await asyncio.wait_for(
                self._generate_content_async(prompt),
                timeout=self.timeout
            )
            
            return response.text.strip()[:300]
            
        except asyncio.TimeoutError:
            raise GeminiTimeoutError()
        except Exception:
            # Return fallback feedback on any error
            return "오늘 하루도 수고했어! 완벽하지 않아도 괜찮아, 꾸준히 하는 게 중요해."
    
    # ==================== Private Helper Methods ====================
    
    async def _generate_content_async(self, prompt: str):
        """Async wrapper for Gemini API call"""
        return await asyncio.to_thread(
            self.model.generate_content,
            prompt
        )
    
    def _extract_json(self, text: str) -> Dict[str, Any]:
        """Extract and parse JSON from response"""
        try:
            # Remove markdown code blocks
            text = re.sub(r'```json\s*|\s*```', '', text)
            text = text.strip()
            
            return json.loads(text)
        except json.JSONDecodeError:
            raise GeminiParsingError("Invalid JSON in response")
    
    def _validate_parsed_schedule(
        self,
        data: Dict[str, Any],
        current_time: datetime
    ) -> Dict[str, Any]:
        """Validate and sanitize parsed schedule data"""
        # Validate priority
        priority = data.get('priority', 3)
        if not (1 <= priority <= 5):
            raise InvalidPriorityError()
        
        # Validate datetime
        start_time_str = data.get('start_time')
        if start_time_str:
            try:
                start_time = datetime.fromisoformat(start_time_str.replace('Z', '+00:00'))
                
                # Check if datetime is not in the past (with 1 hour tolerance)
                if start_time < current_time - timedelta(hours=1):
                    raise InvalidDateTimeError("Schedule time is in the past")
                
                # Check if datetime is not too far in future (> 1 year)
                if start_time > current_time + timedelta(days=365):
                    raise InvalidDateTimeError("Schedule time is too far in the future")
                
                data['start_time'] = start_time.isoformat()
                
            except (ValueError, AttributeError):
                raise InvalidDateTimeError("Invalid start_time format")
        
        # Validate end_time if present
        end_time_str = data.get('end_time')
        if end_time_str:
            try:
                end_time = datetime.fromisoformat(end_time_str.replace('Z', '+00:00'))
                if start_time_str:
                    start_time = datetime.fromisoformat(data['start_time'].replace('Z', '+00:00'))
                    if end_time <= start_time:
                        raise InvalidDateTimeError("End time must be after start time")
                
                data['end_time'] = end_time.isoformat()
            except (ValueError, AttributeError):
                raise InvalidDateTimeError("Invalid end_time format")
        
        # Sanitize text fields
        data['title'] = str(data.get('title', ''))[:255]
        if 'description' in data:
            data['description'] = str(data['description'])[:1000]
        
        return data
    
    def _validate_plan_task(
        self,
        task: Dict[str, Any],
        target_date: date
    ) -> Dict[str, Any]:
        """Validate individual plan task"""
        # Ensure required fields
        if 'title' not in task or 'start_time' not in task:
            raise GeminiParsingError("Invalid task structure")
        
        # Sanitize
        task['title'] = str(task['title'])[:255]
        
        return task
    
    def _build_parse_prompt(self, text: str, current_time: datetime) -> str:
        """Build prompt for schedule parsing"""
        return f"""
현재 시간: {current_time.isoformat()}
현재 날짜: {current_time.strftime('%Y년 %m월 %d일 %A')}

다음 텍스트를 일정 데이터로 파싱하세요: "{text}"

응답은 반드시 아래 JSON 형식으로만 작성하세요:
{{
    "title": "일정 제목",
    "start_time": "ISO 8601 형식 (예: 2025-01-30T19:00:00+09:00)",
    "end_time": "ISO 8601 형식 또는 null",
    "all_day": false,
    "priority": 3,
    "tags": ["태그1"],
    "confidence": 0.95
}}

규칙:
- priority는 1-5 사이 (1=최고)
- confidence는 0-1 사이
- 날짜/시간이 명확하지 않으면 confidence를 낮게
"""
    
    def _build_chat_prompt(
        self,
        message: str,
        context: Dict[str, Any],
        personality: str
    ) -> str:
        """Build prompt for character chat"""
        personality_map = {
            "friendly": "친근하고 편안한 말투 (반말)",
            "motivating": "동기부여하는 코치 스타일 (존댓말)",
            "calm": "차분하고 위로하는 상담사 스타일 (존댓말)"
        }
        
        style = personality_map.get(personality, personality_map["friendly"])
        
        return f"""
당신은 일정 관리를 돕는 AI 캐릭터입니다.

성격: {style}
상황: {context.get('time_of_day', '일반')} 시간대
오늘 진행도: {context.get('completed_count', 0)}/{context.get('total_count', 0)} 완료

사용자 메시지: "{message}"

응답 규칙:
- 2-3문장 이내로 간결하게
- 격려와 실질적인 조언 제공
- 이모지는 최소한으로 (0-1개)
- {style}로 작성
"""
    
    def _build_plan_prompt(
        self,
        target_date: date,
        tasks: List[Dict[str, Any]],
        preferences: Dict[str, Any]
    ) -> str:
        """Build prompt for daily plan generation"""
        return f"""
날짜: {target_date.isoformat()}
작업 시작: {preferences.get('start_time', '09:00')}
작업 종료: {preferences.get('end_time', '18:00')}
휴식 시간: {preferences.get('break_duration', 10)}분

할 일 목록:
{json.dumps(tasks, ensure_ascii=False, indent=2)}

최적의 일정표를 JSON 배열로 생성하세요:
[
  {{
    "title": "작업명",
    "start_time": "HH:MM",
    "end_time": "HH:MM",
    "reason": "이 시간에 배치한 이유"
  }}
]

원칙:
- 우선순위 높은 작업을 오전에
- 적절한 휴식 포함
- 실현 가능한 계획
"""
    
    def _build_review_prompt(
        self,
        review_data: Dict[str, Any],
        historical_average: Dict[str, Any]
    ) -> str:
        """Build prompt for review feedback"""
        return f"""
오늘의 회고:
- 평점: {review_data.get('rating', 3)}/5
- 완료율: {review_data.get('completion_rate', 0):.0%}
- 집중 시간: {review_data.get('focus_minutes', 0)}분
- 기분: {review_data.get('mood_keyword', '보통')}

30일 평균:
- 완료율: {historical_average.get('completion_rate', 0):.0%}
- 집중 시간: {historical_average.get('focus_minutes', 0)}분

따뜻하고 격려하는 피드백을 2-3문장으로 작성하세요.
평가보다는 성취에 초점을 맞추세요.
"""


# Create singleton instance
ai_service = AIService()