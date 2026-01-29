"""
Character state API endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import Optional
from datetime import datetime, timedelta

from app.core.database import get_db
from app.models.user import User
from app.models.other import CharacterState, CharacterActivity, CharacterEmotion
from app.schemas import CharacterStateResponse
from app.middleware.auth import get_current_user

router = APIRouter()


@router.get("/state", response_model=CharacterStateResponse)
async def get_character_state(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get current character state
    
    Returns the most recent non-expired character state,
    or a default idle state if none exists.
    """
    # Get the most recent non-expired state
    result = await db.execute(
        select(CharacterState).where(
            and_(
                CharacterState.user_id == current_user.id,
                CharacterState.expires_at > datetime.utcnow()
            )
        ).order_by(CharacterState.created_at.desc()).limit(1)
    )
    state = result.scalar_one_or_none()
    
    if not state:
        # Return default idle state
        return CharacterStateResponse(
            activity="idle",
            emotion="normal",
            message="안녕! 오늘 하루도 같이 보내자~",
            led_color="#FFFFFF",
            led_pattern="solid",
            animation_key="idle_normal"
        )
    
    return CharacterStateResponse(
        activity=state.activity.value,
        emotion=state.emotion.value,
        message=state.message,
        led_color=state.led_color,
        led_pattern=state.led_pattern,
        animation_key=state.animation_key
    )


@router.post("/state")
async def update_character_state(
    activity: str,
    emotion: str,
    message: Optional[str] = None,
    duration_minutes: int = 60,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Update character state
    
    This endpoint is typically called by backend services or
    when the app wants to manually set the character's state.
    
    Args:
        activity: idle, focus, break, notify, celebrate
        emotion: normal, happy, proud, tired, worried, excited
        message: Optional message from character
        duration_minutes: How long this state should last
    """
    # Validate activity and emotion
    try:
        activity_enum = CharacterActivity(activity)
        emotion_enum = CharacterEmotion(emotion)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid activity or emotion value"
        )
    
    # Map activity to LED color and pattern
    led_mapping = {
        "idle": {"color": "#FFFFFF", "pattern": "solid"},
        "focus": {"color": "#0066FF", "pattern": "solid"},
        "break": {"color": "#00FF66", "pattern": "pulse"},
        "notify": {"color": "#FFAA00", "pattern": "blink"},
        "celebrate": {"color": "#FF00FF", "pattern": "blink"}
    }
    
    led_config = led_mapping.get(activity, {"color": "#FFFFFF", "pattern": "solid"})
    
    # Create new state
    new_state = CharacterState(
        user_id=current_user.id,
        activity=activity_enum,
        emotion=emotion_enum,
        message=message,
        led_color=led_config["color"],
        led_pattern=led_config["pattern"],
        animation_key=f"{activity}_{emotion}",
        expires_at=datetime.utcnow() + timedelta(minutes=duration_minutes)
    )
    
    db.add(new_state)
    await db.commit()
    
    return {
        "message": "Character state updated successfully",
        "state": {
            "activity": activity,
            "emotion": emotion,
            "led_color": led_config["color"],
            "led_pattern": led_config["pattern"]
        }
    }


@router.get("/history")
async def get_character_history(
    limit: int = 20,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get character state history
    
    Returns recent character state changes for analytics or debugging.
    """
    result = await db.execute(
        select(CharacterState)
        .where(CharacterState.user_id == current_user.id)
        .order_by(CharacterState.created_at.desc())
        .limit(limit)
    )
    states = result.scalars().all()
    
    return [
        {
            "id": str(s.id),
            "activity": s.activity.value,
            "emotion": s.emotion.value,
            "message": s.message,
            "created_at": s.created_at.isoformat(),
            "expires_at": s.expires_at.isoformat() if s.expires_at else None
        }
        for s in states
    ]


@router.delete("/state")
async def clear_character_state(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Clear all active character states
    
    This resets the character to idle state by expiring all current states.
    """
    result = await db.execute(
        select(CharacterState).where(
            and_(
                CharacterState.user_id == current_user.id,
                CharacterState.expires_at > datetime.utcnow()
            )
        )
    )
    states = result.scalars().all()
    
    # Expire all active states
    for state in states:
        state.expires_at = datetime.utcnow()
    
    await db.commit()
    
    return {
        "message": "Character state cleared",
        "expired_states": len(states)
    }