"""
Focus Session Management API Endpoints
Handles focus session tracking, statistics, and history
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import Optional, List
from datetime import datetime, timedelta

from app.core.database import get_db
from app.models.user import User
from app.models.other import FocusSession, SessionStatus
from app.schemas import FocusSessionCreate, FocusSessionEnd, FocusSessionResponse
from app.middleware.auth import get_current_user

router = APIRouter()


@router.post("", response_model=FocusSessionResponse, status_code=status.HTTP_201_CREATED)
async def start_focus_session(
    session_data: FocusSessionCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Start a new focus session
    
    Creates a new active focus session. Only one active session is allowed at a time.
    
    **Request Body:**
    - `planned_duration`: Duration in minutes (1-240)
    - `schedule_id`: Optional linked schedule
    - `todo_id`: Optional linked todo
    
    **Returns:**
    - Focus session with status "active"
    
    **Errors:**
    - `400`: Already have an active focus session
    """
    # Check if there's already an active session
    result = await db.execute(
        select(FocusSession).where(
            and_(
                FocusSession.user_id == current_user.id,
                FocusSession.status == SessionStatus.ACTIVE
            )
        )
    )
    active_session = result.scalar_one_or_none()
    
    if active_session:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="You already have an active focus session. Please end it before starting a new one.",
            headers={"X-Error-Code": "FOCUS_001"}
        )
    
    # Create new session
    new_session = FocusSession(
        user_id=current_user.id,
        **session_data.model_dump()
    )
    
    db.add(new_session)
    await db.commit()
    await db.refresh(new_session)
    
    return new_session


@router.put("/{session_id}/end", response_model=FocusSessionResponse)
async def end_focus_session(
    session_id: str,
    end_data: FocusSessionEnd,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    End a focus session
    
    Marks the session as completed and calculates actual duration.
    
    **Path Parameters:**
    - `session_id`: UUID of the session
    
    **Request Body:**
    - `notes`: Optional notes about the session
    
    **Returns:**
    - Completed focus session with actual duration
    
    **Errors:**
    - `404`: Session not found
    - `400`: Session is not active
    """
    # Get session
    result = await db.execute(
        select(FocusSession).where(
            and_(
                FocusSession.id == session_id,
                FocusSession.user_id == current_user.id
            )
        )
    )
    session = result.scalar_one_or_none()
    
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Focus session not found"
        )
    
    if session.status != SessionStatus.ACTIVE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Session is not active",
            headers={"X-Error-Code": "FOCUS_002"}
        )
    
    # Calculate actual duration in minutes
    ended_at = datetime.utcnow()
    actual_duration = int((ended_at - session.started_at).total_seconds() / 60)
    
    # Update session
    session.status = SessionStatus.COMPLETED
    session.ended_at = ended_at
    session.actual_duration = actual_duration
    if end_data.notes:
        session.notes = end_data.notes
    
    await db.commit()
    await db.refresh(session)
    
    return session


@router.post("/{session_id}/cancel", response_model=FocusSessionResponse)
async def cancel_focus_session(
    session_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Cancel a focus session
    
    Cancels an active session. The partial time spent will be recorded.
    
    **Path Parameters:**
    - `session_id`: UUID of the session
    
    **Returns:**
    - Cancelled focus session
    
    **Errors:**
    - `404`: Session not found
    - `400`: Session is not active
    """
    # Get session
    result = await db.execute(
        select(FocusSession).where(
            and_(
                FocusSession.id == session_id,
                FocusSession.user_id == current_user.id
            )
        )
    )
    session = result.scalar_one_or_none()
    
    if not session:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Focus session not found"
        )
    
    if session.status != SessionStatus.ACTIVE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Session is not active",
            headers={"X-Error-Code": "FOCUS_002"}
        )
    
    # Update session
    ended_at = datetime.utcnow()
    session.status = SessionStatus.CANCELLED
    session.ended_at = ended_at
    
    # Calculate actual duration even for cancelled sessions
    actual_duration = int((ended_at - session.started_at).total_seconds() / 60)
    session.actual_duration = actual_duration
    
    await db.commit()
    await db.refresh(session)
    
    return session


@router.get("/active", response_model=Optional[FocusSessionResponse])
async def get_active_session(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get current active focus session
    
    Returns the currently active focus session, or null if none exists.
    
    **Returns:**
    - Active focus session or null
    """
    result = await db.execute(
        select(FocusSession).where(
            and_(
                FocusSession.user_id == current_user.id,
                FocusSession.status == SessionStatus.ACTIVE
            )
        )
    )
    session = result.scalar_one_or_none()
    
    return session


@router.get("/stats")
async def get_focus_stats(
    start_date: Optional[datetime] = Query(None, description="Start date for statistics"),
    end_date: Optional[datetime] = Query(None, description="End date for statistics"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get focus session statistics
    
    Returns aggregated statistics for completed focus sessions within a date range.
    
    **Query Parameters:**
    - `start_date`: Optional start date (default: 7 days ago)
    - `end_date`: Optional end date (default: now)
    
    **Returns:**
    ```json
    {
        "period": {"start_date": "...", "end_date": "..."},
        "total_sessions": 15,
        "total_minutes": 375,
        "total_hours": 6.25,
        "average_duration": 25.0,
        "completion_rate": 0.93,
        "sessions_per_day": 2.1
    }
    ```
    """
    # Default to last 7 days if no dates provided
    if not end_date:
        end_date = datetime.utcnow()
    if not start_date:
        start_date = end_date - timedelta(days=7)
    
    # Get completed sessions in date range
    result = await db.execute(
        select(FocusSession).where(
            and_(
                FocusSession.user_id == current_user.id,
                FocusSession.started_at >= start_date,
                FocusSession.started_at <= end_date,
                FocusSession.status == SessionStatus.COMPLETED
            )
        )
    )
    sessions = result.scalars().all()
    
    # Calculate statistics
    total_sessions = len(sessions)
    total_minutes = sum(s.actual_duration or 0 for s in sessions)
    average_duration = total_minutes / total_sessions if total_sessions > 0 else 0
    
    # Calculate completion rate (sessions that reached at least 90% of planned duration)
    completed_fully = sum(
        1 for s in sessions 
        if s.actual_duration and s.actual_duration >= s.planned_duration * 0.9
    )
    completion_rate = completed_fully / total_sessions if total_sessions > 0 else 0
    
    # Calculate sessions per day
    days_diff = (end_date - start_date).days + 1
    sessions_per_day = total_sessions / days_diff if days_diff > 0 else 0
    
    return {
        "period": {
            "start_date": start_date.isoformat() + "Z",
            "end_date": end_date.isoformat() + "Z"
        },
        "total_sessions": total_sessions,
        "total_minutes": total_minutes,
        "total_hours": round(total_minutes / 60, 2),
        "average_duration": round(average_duration, 1),
        "completion_rate": round(completion_rate, 2),
        "sessions_per_day": round(sessions_per_day, 1)
    }


@router.get("/history", response_model=List[FocusSessionResponse])
async def get_focus_history(
    limit: int = Query(20, le=100, description="Maximum number of sessions to return"),
    offset: int = Query(0, description="Number of sessions to skip"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get focus session history
    
    Returns a paginated list of past focus sessions, ordered by most recent first.
    
    **Query Parameters:**
    - `limit`: Maximum sessions to return (default: 20, max: 100)
    - `offset`: Number of sessions to skip (default: 0)
    
    **Returns:**
    - List of focus sessions
    """
    result = await db.execute(
        select(FocusSession)
        .where(FocusSession.user_id == current_user.id)
        .order_by(FocusSession.started_at.desc())
        .limit(limit)
        .offset(offset)
    )
    sessions = result.scalars().all()
    
    return sessions