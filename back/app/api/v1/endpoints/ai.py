"""
AI Assistant API Endpoints (Improved)
- Service layer separation
- Proper error handling
- Request validation with Pydantic models
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.exc import SQLAlchemyError
from pydantic import BaseModel, Field, UUID4
from typing import List, Dict, Any, Optional
from datetime import datetime, date
import uuid

from app.core.database import get_db
from app.core.exceptions import (
    GeminiTimeoutError,
    GeminiRateLimitError,
    GeminiParsingError,
    DatabaseTransactionError,
    LowConfidenceError
)
from app.services.ai_service import ai_service
from app.models.user import User
from app.models.other import AIConversation
from app.middleware.auth import get_current_user

router = APIRouter()


# ==================== Request/Response Models ====================

class ParseScheduleRequest(BaseModel):
    """Request model for schedule parsing"""
    text: str = Field(..., min_length=1, max_length=500, description="Natural language schedule text")


class ParseScheduleResponse(BaseModel):
    """Response model for parsed schedule"""
    parsed: Dict[str, Any] = Field(..., description="Parsed schedule data")
    confidence: float = Field(..., ge=0, le=1, description="Parsing confidence")


class ChatRequest(BaseModel):
    """Request model for character chat"""
    message: str = Field(..., min_length=1, max_length=1000, description="User's message")
    session_id: UUID4 = Field(..., description="Conversation session ID")
    context: Optional[Dict[str, Any]] = Field(default=None, description="Additional context")


class ChatResponse(BaseModel):
    """Response model for chat"""
    response: str = Field(..., description="Character's response")
    suggestions: Optional[List[Dict[str, Any]]] = Field(default=None, description="Action suggestions")
    character_state: Dict[str, str] = Field(..., description="Character state")


class TaskInput(BaseModel):
    """Individual task for plan generation"""
    title: str = Field(..., min_length=1, max_length=255)
    priority: int = Field(..., ge=1, le=5)
    estimated_duration: int = Field(..., ge=1, le=480)  # max 8 hours


class PlanPreferences(BaseModel):
    """Preferences for plan generation"""
    start_time: str = Field(..., pattern=r"^\d{2}:\d{2}$")
    end_time: str = Field(..., pattern=r"^\d{2}:\d{2}$")
    break_duration: int = Field(default=10, ge=5, le=30)


class GeneratePlanRequest(BaseModel):
    """Request model for daily plan generation"""
    date: date = Field(..., description="Target date for planning")
    tasks: List[TaskInput] = Field(..., min_items=1, max_items=20, description="Tasks to schedule")
    preferences: PlanPreferences = Field(..., description="User preferences")


class GeneratePlanResponse(BaseModel):
    """Response model for generated plan"""
    plan: List[Dict[str, Any]] = Field(..., description="Generated schedule plan")
    reasoning: str = Field(..., description="Planning reasoning")


class RearrangeScheduleRequest(BaseModel):
    """Request model for schedule rearrangement"""
    user_message: str = Field(..., min_length=1, max_length=500, description="User's rearrangement request")
    current_date: Optional[date] = Field(default=None, description="Date to rearrange (default: today)")


# ==================== Endpoints ====================

@router.post("/parse", response_model=ParseScheduleResponse)
async def parse_schedule(
    request: ParseScheduleRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Parse natural language text into structured schedule data
    
    **Error Handling:**
    - `503`: AI service unavailable or timeout
    - `422`: Low confidence or invalid data
    - `500`: Database error
    
    **Example:**
    ```json
    {
        "text": "내일 오후 7시에 영어 단어 외우기"
    }
    ```
    """
    current_time = datetime.now()
    
    try:
        # Call AI service (may raise specific exceptions)
        parsed_data = await ai_service.parse_schedule_text(
            text=request.text,
            current_time=current_time
        )
        
        confidence = parsed_data.pop('confidence', 0.8)
        
        # Save to database with transaction
        try:
            async with db.begin():
                conversation = AIConversation(
                    user_id=current_user.id,
                    session_id=uuid.uuid4(),
                    user_message=request.text,
                    ai_response=str(parsed_data),
                    intent="parse_schedule",
                    response_time_ms=0  # Can be measured if needed
                )
                db.add(conversation)
        except SQLAlchemyError as e:
            raise DatabaseTransactionError(f"Failed to save conversation: {str(e)}")
        
        return ParseScheduleResponse(
            parsed=parsed_data,
            confidence=confidence
        )
        
    except (GeminiTimeoutError, GeminiRateLimitError, GeminiParsingError, LowConfidenceError):
        # Re-raise AI-specific errors
        raise
    except DatabaseTransactionError:
        raise
    except Exception as e:
        # Catch-all for unexpected errors
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Unexpected error: {str(e)}"
        )


@router.post("/chat", response_model=ChatResponse)
async def chat_with_character(
    request: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Chat with the AI character companion
    
    **Error Handling:**
    - `503`: AI service unavailable or timeout
    - `500`: Database error
    """
    try:
        # Prepare context
        context = request.context or {}
        context['date'] = context.get('date', datetime.now().isoformat())
        context['user_id'] = str(current_user.id)
        
        # Get personality from user preferences (fallback to friendly)
        personality = "friendly"  # TODO: Fetch from user preferences
        
        # Determine time of day
        current_hour = datetime.now().hour
        if 5 <= current_hour < 12:
            context['time_of_day'] = '아침'
        elif 12 <= current_hour < 17:
            context['time_of_day'] = '오후'
        elif 17 <= current_hour < 21:
            context['time_of_day'] = '저녁'
        else:
            context['time_of_day'] = '밤'
        
        # Generate response
        response_text = await ai_service.generate_character_response(
            user_message=request.message,
            context=context,
            personality=personality
        )
        
        # Determine character state
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
        
        # Save conversation with transaction
        try:
            async with db.begin():
                conversation = AIConversation(
                    user_id=current_user.id,
                    session_id=request.session_id,
                    user_message=request.message,
                    ai_response=response_text,
                    intent="chat",
                    context_data=context
                )
                db.add(conversation)
        except SQLAlchemyError as e:
            # Log error but don't fail the request
            # (conversation history is not critical)
            pass
        
        return ChatResponse(
            response=response_text,
            suggestions=None,
            character_state=character_state
        )
        
    except (GeminiTimeoutError, GeminiRateLimitError):
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Chat failed: {str(e)}"
        )


@router.post("/generate-plan", response_model=GeneratePlanResponse)
async def generate_daily_plan(
    request: GeneratePlanRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Generate an optimized daily schedule from tasks
    
    **Error Handling:**
    - `503`: AI service unavailable or timeout
    - `422`: Invalid task data
    """
    try:
        # Convert tasks to dict format
        tasks = [task.model_dump() for task in request.tasks]
        preferences = request.preferences.model_dump()
        
        # Generate plan
        plan = await ai_service.generate_daily_plan(
            target_date=request.date,
            tasks=tasks,
            preferences=preferences
        )
        
        # Extract reasoning
        reasoning = "우선순위와 에너지 패턴을 고려해서 계획을 짰어요."
        if plan and len(plan) > 0 and 'reason' in plan[0]:
            reasoning = plan[0]['reason']
        
        # Save to database
        try:
            async with db.begin():
                conversation = AIConversation(
                    user_id=current_user.id,
                    session_id=uuid.uuid4(),
                    user_message=f"Generate plan for {request.date}",
                    ai_response=str(plan),
                    intent="generate_plan",
                    context_data={"tasks": tasks, "preferences": preferences}
                )
                db.add(conversation)
        except SQLAlchemyError:
            pass  # Non-critical
        
        return GeneratePlanResponse(
            plan=plan,
            reasoning=reasoning
        )
        
    except (GeminiTimeoutError, GeminiParsingError):
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Plan generation failed: {str(e)}"
        )


@router.post("/rearrange-schedule")
async def rearrange_schedule(
    request: RearrangeScheduleRequest,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Analyze incomplete schedules and suggest rearrangements
    
    **Error Handling:**
    - `503`: AI service unavailable
    - `500`: Database error
    """
    from app.models.schedule import Schedule, ScheduleStatus
    from datetime import datetime
    
    # Use provided date or today
    target_date = request.current_date or date.today()
    day_start = datetime.combine(target_date, datetime.min.time())
    day_end = datetime.combine(target_date, datetime.max.time())
    
    try:
        # Fetch pending schedules with transaction
        async with db.begin():
            result = await db.execute(
                select(Schedule).where(
                    Schedule.user_id == current_user.id,
                    Schedule.status == ScheduleStatus.PENDING,
                    Schedule.start_time >= day_start,
                    Schedule.start_time < day_end
                )
            )
            schedules = result.scalars().all()
    except SQLAlchemyError as e:
        raise DatabaseTransactionError(f"Failed to fetch schedules: {str(e)}")
    
    current_schedules = [
        {
            "id": str(s.id),
            "title": s.title,
            "start_time": s.start_time.isoformat(),
            "priority": s.priority
        }
        for s in schedules
    ]
    
    try:
        # For now, return a simple response
        # (Full implementation would call AI service)
        return {
            "action": "rearrange",
            "suggestions": current_schedules,
            "message": f"Found {len(current_schedules)} pending schedules"
        }
        
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to rearrange: {str(e)}"
        )