"""
Review Service Layer
Handles daily review business logic with proper transaction management
"""
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from sqlalchemy.exc import SQLAlchemyError
from datetime import datetime, date, timedelta
from typing import Dict, Any, List

from app.core.exceptions import DatabaseTransactionError
from app.models.other import DailyReview
from app.models.schedule import Schedule, ScheduleStatus
from app.models.todo import Todo, TodoStatus
from app.models.other import FocusSession, SessionStatus
from app.services.ai_service import ai_service


class ReviewService:
    """Service for daily review operations"""
    
    async def create_or_update_review(
        self,
        user_id: str,
        review_date: date,
        rating: int = None,
        mood_keyword: str = None,
        reflection: str = None,
        db: AsyncSession = None
    ) -> Dict[str, Any]:
        """
        Create or update daily review with statistics and AI feedback
        
        Raises:
            DatabaseTransactionError: If database operation fails
        """
        day_start = datetime.combine(review_date, datetime.min.time())
        day_end = datetime.combine(review_date, datetime.max.time())
        
        try:
            async with db.begin():
                # Calculate statistics
                stats = await self._calculate_daily_stats(
                    user_id, day_start, day_end, db
                )
                
                # Get or create review
                review = await self._get_or_create_review(
                    user_id, review_date, db
                )
                
                # Update review data
                review.rating = rating
                review.mood_keyword = mood_keyword
                review.reflection = reflection
                review.total_focus_minutes = stats['total_focus_minutes']
                review.completed_schedules_count = stats['completed_schedules']
                review.completed_todos_count = stats['completed_todos']
                review.total_schedules_count = stats['total_schedules']
                review.total_todos_count = stats['total_todos']
                
                # Transaction automatically commits here
                
        except SQLAlchemyError as e:
            raise DatabaseTransactionError(f"Failed to save review: {str(e)}")
        
        # Get historical data for AI feedback
        historical_avg = await self._get_historical_average(
            user_id, review_date, db
        )
        
        # Calculate completion rate
        completion_rate = (
            (stats['completed_schedules'] + stats['completed_todos']) /
            max((stats['total_schedules'] + stats['total_todos']), 1)
        )
        
        # Generate AI feedback (non-critical, use fallback on error)
        try:
            feedback = await ai_service.generate_review_feedback(
                review_data={
                    "rating": rating or 3,
                    "completion_rate": completion_rate,
                    "focus_minutes": stats['total_focus_minutes'],
                    "mood_keyword": mood_keyword or "보통"
                },
                historical_average=historical_avg
            )
        except Exception:
            feedback = "오늘 하루도 수고했어! 완벽하지 않아도 괜찮아, 꾸준히 하는 게 중요해."
        
        return {
            "id": str(review.id),
            "review_date": review.review_date.isoformat(),
            "rating": review.rating,
            "mood_keyword": review.mood_keyword,
            "reflection": review.reflection,
            "summary": {
                "total_focus_minutes": stats['total_focus_minutes'],
                "completed_schedules_count": stats['completed_schedules'],
                "total_schedules_count": stats['total_schedules'],
                "completed_todos_count": stats['completed_todos'],
                "total_todos_count": stats['total_todos'],
                "completion_rate": round(completion_rate, 2)
            },
            "character_feedback": feedback
        }
    
    async def _calculate_daily_stats(
        self,
        user_id: str,
        day_start: datetime,
        day_end: datetime,
        db: AsyncSession
    ) -> Dict[str, int]:
        """Calculate daily statistics"""
        # Get schedules
        schedule_result = await db.execute(
            select(Schedule).where(
                and_(
                    Schedule.user_id == user_id,
                    Schedule.start_time >= day_start,
                    Schedule.start_time <= day_end
                )
            )
        )
        schedules = schedule_result.scalars().all()
        
        # Get todos
        todo_result = await db.execute(
            select(Todo).where(
                and_(
                    Todo.user_id == user_id,
                    Todo.due_date == day_start.date()
                )
            )
        )
        todos = todo_result.scalars().all()
        
        # Get focus sessions
        focus_result = await db.execute(
            select(FocusSession).where(
                and_(
                    FocusSession.user_id == user_id,
                    FocusSession.started_at >= day_start,
                    FocusSession.started_at <= day_end,
                    FocusSession.status == SessionStatus.COMPLETED
                )
            )
        )
        focus_sessions = focus_result.scalars().all()
        
        return {
            'total_schedules': len(schedules),
            'completed_schedules': sum(1 for s in schedules if s.status == ScheduleStatus.COMPLETED),
            'total_todos': len(todos),
            'completed_todos': sum(1 for t in todos if t.status == TodoStatus.COMPLETED),
            'total_focus_minutes': sum(s.actual_duration or 0 for s in focus_sessions)
        }
    
    async def _get_or_create_review(
        self,
        user_id: str,
        review_date: date,
        db: AsyncSession
    ) -> DailyReview:
        """Get existing review or create new one"""
        result = await db.execute(
            select(DailyReview).where(
                and_(
                    DailyReview.user_id == user_id,
                    DailyReview.review_date == review_date
                )
            )
        )
        review = result.scalar_one_or_none()
        
        if not review:
            review = DailyReview(
                user_id=user_id,
                review_date=review_date
            )
            db.add(review)
        
        return review
    
    async def _get_historical_average(
        self,
        user_id: str,
        review_date: date,
        db: AsyncSession
    ) -> Dict[str, float]:
        """Calculate 30-day historical average"""
        thirty_days_ago = review_date - timedelta(days=30)
        
        result = await db.execute(
            select(DailyReview).where(
                and_(
                    DailyReview.user_id == user_id,
                    DailyReview.review_date >= thirty_days_ago,
                    DailyReview.review_date < review_date
                )
            )
        )
        history = result.scalars().all()
        
        if not history:
            return {"focus_minutes": 0, "completion_rate": 0}
        
        avg_focus = sum(h.total_focus_minutes for h in history) / len(history)
        avg_completion = sum(
            (h.completed_schedules_count + h.completed_todos_count) /
            max((h.total_schedules_count + h.total_todos_count), 1)
            for h in history
        ) / len(history)
        
        return {
            "focus_minutes": avg_focus,
            "completion_rate": avg_completion
        }


# Create singleton instance
review_service = ReviewService()