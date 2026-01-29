"""
Daily review API endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import Optional, List
from datetime import datetime, date, timedelta

from app.core.database import get_db
from app.models.user import User
from app.models.other import DailyReview
from app.models.schedule import Schedule, ScheduleStatus
from app.models.todo import Todo, TodoStatus
from app.models.other import FocusSession, SessionStatus
from app.schemas import DailyReviewCreate, DailyReviewResponse
from app.middleware.auth import get_current_user
from app.services.ai_service import gemini_service

router = APIRouter()


@router.post("", status_code=status.HTTP_201_CREATED)
async def create_or_update_review(
    review_data: DailyReviewCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Create or update daily review with AI-generated feedback
    """
    review_date = review_data.review_date
    
    # Calculate statistics for the day
    day_start = datetime.combine(review_date, datetime.min.time())
    day_end = datetime.combine(review_date, datetime.max.time())
    
    # Get schedules
    schedule_result = await db.execute(
        select(Schedule).where(
            and_(
                Schedule.user_id == current_user.id,
                Schedule.start_time >= day_start,
                Schedule.start_time <= day_end
            )
        )
    )
    schedules = schedule_result.scalars().all()
    total_schedules = len(schedules)
    completed_schedules = sum(1 for s in schedules if s.status == ScheduleStatus.COMPLETED)
    
    # Get todos due on this date
    todo_result = await db.execute(
        select(Todo).where(
            and_(
                Todo.user_id == current_user.id,
                Todo.due_date == review_date
            )
        )
    )
    todos = todo_result.scalars().all()
    total_todos = len(todos)
    completed_todos = sum(1 for t in todos if t.status == TodoStatus.COMPLETED)
    
    # Get focus time
    focus_result = await db.execute(
        select(FocusSession).where(
            and_(
                FocusSession.user_id == current_user.id,
                FocusSession.started_at >= day_start,
                FocusSession.started_at <= day_end,
                FocusSession.status == SessionStatus.COMPLETED
            )
        )
    )
    focus_sessions = focus_result.scalars().all()
    total_focus_minutes = sum(s.actual_duration or 0 for s in focus_sessions)
    
    # Check if review already exists
    result = await db.execute(
        select(DailyReview).where(
            and_(
                DailyReview.user_id == current_user.id,
                DailyReview.review_date == review_date
            )
        )
    )
    review = result.scalar_one_or_none()
    
    if review:
        # Update existing review
        review.rating = review_data.rating
        review.mood_keyword = review_data.mood_keyword
        review.reflection = review_data.reflection
    else:
        # Create new review
        review = DailyReview(
            user_id=current_user.id,
            review_date=review_date,
            rating=review_data.rating,
            mood_keyword=review_data.mood_keyword,
            reflection=review_data.reflection
        )
        db.add(review)
    
    # Update statistics
    review.total_focus_minutes = total_focus_minutes
    review.completed_schedules_count = completed_schedules
    review.completed_todos_count = completed_todos
    review.total_schedules_count = total_schedules
    review.total_todos_count = total_todos
    
    await db.commit()
    await db.refresh(review)
    
    # Get historical average for comparison
    thirty_days_ago = review_date - timedelta(days=30)
    history_result = await db.execute(
        select(DailyReview).where(
            and_(
                DailyReview.user_id == current_user.id,
                DailyReview.review_date >= thirty_days_ago,
                DailyReview.review_date < review_date
            )
        )
    )
    history = history_result.scalars().all()
    
    avg_focus = sum(h.total_focus_minutes for h in history) / len(history) if history else 0
    avg_completion = sum(
        (h.completed_schedules_count + h.completed_todos_count) / 
        max((h.total_schedules_count + h.total_todos_count), 1)
        for h in history
    ) / len(history) if history else 0
    
    # Generate AI feedback
    completion_rate = (completed_schedules + completed_todos) / max((total_schedules + total_todos), 1)
    
    try:
        feedback = await gemini_service.generate_review_feedback(
            review_data={
                "rating": review_data.rating or 3,
                "completion_rate": completion_rate,
                "focus_minutes": total_focus_minutes,
                "mood_keyword": review_data.mood_keyword or "보통"
            },
            historical_average={
                "focus_minutes": avg_focus,
                "completion_rate": avg_completion
            }
        )
    except Exception as e:
        # Fallback feedback if AI fails
        feedback = "오늘 하루도 수고했어! 완벽하지 않아도 괜찮아, 꾸준히 하는 게 중요해."
    
    return {
        "id": str(review.id),
        "review_date": review.review_date.isoformat(),
        "rating": review.rating,
        "mood_keyword": review.mood_keyword,
        "reflection": review.reflection,
        "summary": {
            "total_focus_minutes": total_focus_minutes,
            "completed_schedules_count": completed_schedules,
            "total_schedules_count": total_schedules,
            "completed_todos_count": completed_todos,
            "total_todos_count": total_todos,
            "completion_rate": round(completion_rate, 2)
        },
        "character_feedback": feedback
    }


@router.get("", response_model=List[DailyReviewResponse])
async def get_reviews(
    start_date: Optional[date] = Query(None),
    end_date: Optional[date] = Query(None),
    limit: int = Query(30, le=100),
    offset: int = Query(0),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get list of daily reviews
    """
    query = select(DailyReview).where(DailyReview.user_id == current_user.id)
    
    if start_date:
        query = query.where(DailyReview.review_date >= start_date)
    if end_date:
        query = query.where(DailyReview.review_date <= end_date)
    
    query = query.order_by(DailyReview.review_date.desc()).limit(limit).offset(offset)
    
    result = await db.execute(query)
    reviews = result.scalars().all()
    
    return reviews


@router.get("/analytics")
async def get_analytics(
    period: str = Query("weekly", regex="^(weekly|monthly)$"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get analytics for weekly or monthly period
    """
    end_date = date.today()
    if period == "weekly":
        start_date = end_date - timedelta(days=7)
    else:  # monthly
        start_date = end_date - timedelta(days=30)
    
    result = await db.execute(
        select(DailyReview).where(
            and_(
                DailyReview.user_id == current_user.id,
                DailyReview.review_date >= start_date,
                DailyReview.review_date <= end_date
            )
        ).order_by(DailyReview.review_date.asc())
    )
    reviews = result.scalars().all()
    
    if not reviews:
        return {
            "period": period,
            "start_date": start_date.isoformat(),
            "end_date": end_date.isoformat(),
            "average_rating": 0,
            "total_focus_hours": 0,
            "completion_trend": [],
            "mood_frequency": {},
            "days_reviewed": 0
        }
    
    # Calculate metrics
    avg_rating = sum(r.rating for r in reviews if r.rating) / len([r for r in reviews if r.rating]) if reviews else 0
    total_focus_hours = sum(r.total_focus_minutes for r in reviews) / 60
    
    completion_trend = [
        {
            "date": r.review_date.isoformat(),
            "rate": round((r.completed_schedules_count + r.completed_todos_count) / 
                   max((r.total_schedules_count + r.total_todos_count), 1), 2)
        }
        for r in reviews
    ]
    
    # Mood frequency
    mood_frequency = {}
    for r in reviews:
        if r.mood_keyword:
            mood_frequency[r.mood_keyword] = mood_frequency.get(r.mood_keyword, 0) + 1
    
    return {
        "period": period,
        "start_date": start_date.isoformat(),
        "end_date": end_date.isoformat(),
        "average_rating": round(avg_rating, 1),
        "total_focus_hours": round(total_focus_hours, 1),
        "completion_trend": completion_trend,
        "mood_frequency": mood_frequency,
        "days_reviewed": len(reviews)
    }


@router.get("/{review_date}", response_model=DailyReviewResponse)
async def get_review_by_date(
    review_date: date,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get a specific review by date
    """
    result = await db.execute(
        select(DailyReview).where(
            and_(
                DailyReview.user_id == current_user.id,
                DailyReview.review_date == review_date
            )
        )
    )
    review = result.scalar_one_or_none()
    
    if not review:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Review not found for this date"
        )
    
    return review