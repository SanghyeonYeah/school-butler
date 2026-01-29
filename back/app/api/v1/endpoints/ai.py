"""
AI Assistant API endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from datetime import datetime, date, timedelta
import uuid

from app.core.database import get_db
from app.services.ai_service import gemini_service
from app.models.user import User
from app.models.schedule import Schedule, ScheduleStatus
from app.models.other import AIConversation
from app.middleware.auth import get_current_user

router = APIRouter()


# Request/Response Models
class ChatRequest(BaseModel):
    message: str = Field(..., description="User's message to the character")
    session_id: uuid.UUID = Field(..., description="Conversation session ID")
    context: Optional[Dict[str, Any]] = Field(
        default=None,
        description="Additional context (current_time, schedules, etc.)"
    )


class ChatResponse(BaseModel):
    response: str = Field(..., description="Character's response")
    suggestions: Optional[List[Dict[str, Any]]] = Field(
        default=None,
        description="Action suggestions (schedule creation, etc.)"
    )
    character_state: Dict[str, str] = Field(
        ...,
        description="Character's current emotional state"
    )


class ParseScheduleRequest(BaseModel):
    text: str = Field(..., description="Natural language schedule text")


class ParseScheduleResponse(BaseModel):
    parsed: Dict[str, Any] = Field(..., description="Parsed schedule data")
    confidence: float = Field(..., description="Parsing confidence (0-1)")


class GeneratePlanRequest(BaseModel):
    date: date = Field(..., description="Target date for planning")
    tasks: List[Dict[str, Any]] = Field(..., description="Tasks to schedule")
    preferences: Dict[str, Any] = Field(..., description="User preferences")


class GeneratePlanResponse(BaseModel):
    plan: List[Dict[str, Any]] = Field(..., description="Generated schedule plan")
    reasoning: str = Field(..., description="Explanation of planning decisions")


@router.post("/chat", response_model=ChatResponse)
async def chat_with_character(
    request: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Chat with the AI character companion
    
    The character provides conversational responses based on:
    - User's message
    - Current context (time, completed tasks, focus time)
    - Character personality settings
    """
    try:
        # Prepare context
        context = request.context or {}
        if 'date' not in context:
            context['date'] = datetime.now().isoformat()
        if 'user_id' not in context:
            context['user_id'] = str(current_user.id)
        
        # Get user's character personality preference
        personality = "friendly"  # TODO: Fetch from user preferences
        
        # Determine time of day for context
        current_hour = datetime.now().hour
        if 5 <= current_hour < 12:
            context['time_of_day'] = '아침'
        elif 12 <= current_hour < 17:
            context['time_of_day'] = '오후'
        elif 17 <= current_hour < 21:
            context['time_of_day'] = '저녁'
        else:
            context['time_of_day'] = '밤'
        
        # Generate character response
        start_time = datetime.now()
        response_text = await gemini_service.generate_character_response(
            user_message=request.message,
            context=context,
            personality=personality
        )
        response_time = int((datetime.now() - start_time).total_seconds() * 1000)
        
        # Determine character state based on context and message
        character_state = {
            "emotion": "normal",
            "animation": "talking"
        }
        
        if any(word in request.message for word in ["집중", "공부", "일", "작업"]):
            character_state["emotion"] = "motivated"
            character_state["animation"] = "encouraging"
        elif any(word in request.message for word in ["힘들", "피곤", "지쳐"]):
            character_state["emotion"] = "worried"
            character_state["animation"] = "comforting"
        elif any(word in request.message for word in ["완료", "끝", "했어", "성공"]):
            character_state["emotion"] = "proud"
            character_state["animation"] = "celebrating"
        
        # Save conversation to database
        conversation = AIConversation(
            user_id=current_user.id,
            session_id=request.session_id,
            user_message=request.message,
            ai_response=response_text,
            intent="chat",
            context_data=context,
            response_time_ms=response_time
        )
        db.add(conversation)
        await db.commit()
        
        return ChatResponse(
            response=response_text,
            suggestions=None,  # TODO: Extract actionable suggestions
            character_state=character_state
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating response: {str(e)}"
        )


@router.post("/parse", response_model=ParseScheduleResponse)
async def parse_schedule(
    request: ParseScheduleRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Parse natural language text into structured schedule data
    
    Examples:
    - "내일 오후 7시에 영어 단어 외우기"
    - "다음주 월요일 10시 회의"
    - "매일 아침 9시 운동"
    """
    try:
        current_time = datetime.now()
        
        # Parse using Gemini
        start_time = datetime.now()
        parsed_data = await gemini_service.parse_schedule_text(
            text=request.text,
            current_time=current_time
        )
        response_time = int((datetime.now() - start_time).total_seconds() * 1000)
        
        confidence = parsed_data.pop('confidence', 0.8)
        
        # Save AI conversation
        conversation = AIConversation(
            user_id=current_user.id,
            session_id=uuid.uuid4(),
            user_message=request.text,
            ai_response=str(parsed_data),
            intent="parse_schedule",
            response_time_ms=response_time
        )
        db.add(conversation)
        await db.commit()
        
        return ParseScheduleResponse(
            parsed=parsed_data,
            confidence=confidence
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error parsing schedule: {str(e)}"
        )


@router.post("/generate-plan", response_model=GeneratePlanResponse)
async def generate_daily_plan(
    request: GeneratePlanRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Generate an optimized daily schedule from tasks
    
    The AI will:
    1. Prioritize important tasks
    2. Place difficult tasks in high-energy periods
    3. Include appropriate breaks
    4. Create a realistic, achievable plan
    """
    try:
        # Generate plan using Gemini
        start_time = datetime.now()
        plan = await gemini_service.generate_daily_plan(
            target_date=request.date,
            tasks=request.tasks,
            preferences=request.preferences
        )
        response_time = int((datetime.now() - start_time).total_seconds() * 1000)
        
        # Extract overall reasoning
        reasoning = "우선순위와 에너지 패턴을 고려해서 계획을 짰어요."
        if plan and len(plan) > 0 and 'reason' in plan[0]:
            reasoning = plan[0]['reason']
        
        # Save AI conversation
        conversation = AIConversation(
            user_id=current_user.id,
            session_id=uuid.uuid4(),
            user_message=f"Generate plan for {request.date}",
            ai_response=str(plan),
            intent="generate_plan",
            context_data={"tasks": request.tasks, "preferences": request.preferences},
            response_time_ms=response_time
        )
        db.add(conversation)
        await db.commit()
        
        return GeneratePlanResponse(
            plan=plan,
            reasoning=reasoning
        )
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error generating plan: {str(e)}"
        )


@router.post("/rearrange-schedule")
async def rearrange_schedule(
    user_message: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Analyze incomplete schedules and suggest rearrangements
    
    Handles requests like:
    - "못한 일정 정리해줘"
    - "오늘 일정 다시 짜줘"
    """
    try:
        # Fetch current pending schedules
        today = date.today()
        result = await db.execute(
            select(Schedule).where(
                and_(
                    Schedule.user_id == current_user.id,
                    Schedule.status == ScheduleStatus.PENDING,
                    Schedule.start_time >= datetime.combine(today, datetime.min.time())
                )
            )
        )
        schedules = result.scalars().all()
        
        current_schedules = [
            {
                "id": str(s.id),
                "title": s.title,
                "start_time": s.start_time.isoformat(),
                "priority": s.priority
            }
            for s in schedules
        ]
        
        # Get modification suggestions
        modifications = await gemini_service.extract_schedule_modifications(
            user_message=user_message,
            current_schedules=current_schedules
        )
        
        # Save AI conversation
        conversation = AIConversation(
            user_id=current_user.id,
            session_id=uuid.uuid4(),
            user_message=user_message,
            ai_response=str(modifications),
            intent="rearrange_schedule",
            context_data={"schedules": current_schedules}
        )
        db.add(conversation)
        await db.commit()
        
        return {
            "action": modifications['action'],
            "suggestions": modifications['suggestions'],
            "message": modifications['message']
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error rearranging schedule: {str(e)}"
        )