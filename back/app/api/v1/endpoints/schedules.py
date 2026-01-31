"""
Schedule Management API Endpoints
Handles schedule CRUD operations, filtering, and statistics
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import Optional, List
from datetime import datetime, date

from app.core.database import get_db
from app.models.user import User
from app.models.schedule import Schedule, ScheduleStatus, ScheduleType
from app.schemas import ScheduleCreate, ScheduleUpdate, ScheduleResponse
from app.middleware.auth import get_current_user

router = APIRouter()


@router.post("", response_model=ScheduleResponse, status_code=status.HTTP_201_CREATED)
async def create_schedule(
    schedule_data: ScheduleCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Create a new schedule
    
    Creates a new schedule (event, task, or routine) for the current user.
    
    **Request Body:**
    - `title`: Schedule title (required, max 255 chars)
    - `description`: Optional description
    - `schedule_type`: "event", "task", or "routine" (default: "event")
    - `start_time`: Start time in ISO 8601 format (required)
    - `end_time`: Optional end time
    - `all_day`: Boolean for all-day events (default: false)
    - `location`: Optional location
    - `priority`: 1-5 (1=highest, default: 3)
    - `tags`: Array of tags
    - `color`: Hex color code (e.g., "#FF5733")
    - `reminder_minutes`: Array of minutes before start (e.g., [10, 30])
    
    **Returns:**
    - Created schedule with status "pending"
    
    **Example:**
    ```json
    {
        "title": "영어 단어 외우기",
        "schedule_type": "task",
        "start_time": "2025-01-30T19:00:00+09:00",
        "priority": 2,
        "tags": ["study", "english"]
    }
    ```
    """
    new_schedule = Schedule(
        user_id=current_user.id,
        **schedule_data.model_dump()
    )
    
    db.add(new_schedule)
    await db.commit()
    await db.refresh(new_schedule)
    
    return new_schedule


@router.get("", response_model=List[ScheduleResponse])
async def get_schedules(
    start_date: Optional[datetime] = Query(None, description="Filter by start date (ISO 8601)"),
    end_date: Optional[datetime] = Query(None, description="Filter by end date (ISO 8601)"),
    status: Optional[str] = Query(None, description="Filter by status (pending/in_progress/completed/cancelled)"),
    schedule_type: Optional[str] = Query(None, description="Filter by type (event/task/routine)"),
    limit: int = Query(50, le=100, description="Maximum schedules to return"),
    offset: int = Query(0, description="Number of schedules to skip"),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get list of schedules with optional filters
    
    Returns paginated list of schedules with support for filtering by:
    - Date range (start_date, end_date)
    - Status (pending, in_progress, completed, cancelled)
    - Type (event, task, routine)
    
    **Query Parameters:**
    - `start_date`: ISO 8601 datetime (e.g., "2025-01-30T00:00:00Z")
    - `end_date`: ISO 8601 datetime
    - `status`: pending | in_progress | completed | cancelled | postponed
    - `schedule_type`: event | task | routine
    - `limit`: 1-100 (default: 50)
    - `offset`: Starting position (default: 0)
    
    **Returns:**
    - Array of schedules ordered by start_time (ascending)
    
    **Example:**
    ```
    GET /schedules?start_date=2025-01-30T00:00:00Z&status=pending&limit=20
    ```
    """
    query = select(Schedule).where(Schedule.user_id == current_user.id)
    
    # Apply date filters
    if start_date:
        query = query.where(Schedule.start_time >= start_date)
    if end_date:
        query = query.where(Schedule.start_time <= end_date)
    
    # Apply status filter with validation
    if status:
        try:
            status_enum = ScheduleStatus(status)
            query = query.where(Schedule.status == status_enum)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid status: {status}. Must be one of: pending, in_progress, completed, cancelled, postponed"
            )
    
    # Apply type filter with validation
    if schedule_type:
        try:
            type_enum = ScheduleType(schedule_type)
            query = query.where(Schedule.schedule_type == type_enum)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid schedule_type: {schedule_type}. Must be one of: event, task, routine"
            )
    
    # Order by start time (earliest first)
    query = query.order_by(Schedule.start_time.asc())
    
    # Apply pagination
    query = query.limit(limit).offset(offset)
    
    result = await db.execute(query)
    schedules = result.scalars().all()
    
    return schedules


@router.get("/today/summary")
async def get_today_summary(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get summary of today's schedules
    
    Provides an overview of all schedules for the current day including:
    - Total count
    - Completed count
    - Pending count
    - In-progress count
    - Completion rate
    - List of all today's schedules
    
    **Returns:**
    ```json
    {
        "date": "2025-01-30",
        "total_schedules": 7,
        "completed": 4,
        "pending": 3,
        "in_progress": 0,
        "completion_rate": 0.57,
        "schedules": [
            {
                "id": "uuid",
                "title": "영어 공부",
                "start_time": "2025-01-30T19:00:00+09:00",
                "status": "pending",
                "priority": 2
            }
        ]
    }
    ```
    """
    today = date.today()
    day_start = datetime.combine(today, datetime.min.time())
    day_end = datetime.combine(today, datetime.max.time())
    
    # Get all schedules for today
    result = await db.execute(
        select(Schedule).where(
            and_(
                Schedule.user_id == current_user.id,
                Schedule.start_time >= day_start,
                Schedule.start_time < day_end
            )
        ).order_by(Schedule.start_time.asc())
    )
    schedules = result.scalars().all()
    
    # Calculate statistics
    total_count = len(schedules)
    completed_count = sum(1 for s in schedules if s.status == ScheduleStatus.COMPLETED)
    pending_count = sum(1 for s in schedules if s.status == ScheduleStatus.PENDING)
    in_progress_count = sum(1 for s in schedules if s.status == ScheduleStatus.IN_PROGRESS)
    
    return {
        "date": today.isoformat(),
        "total_schedules": total_count,
        "completed": completed_count,
        "pending": pending_count,
        "in_progress": in_progress_count,
        "completion_rate": round(completed_count / total_count, 2) if total_count > 0 else 0,
        "schedules": [
            {
                "id": str(s.id),
                "title": s.title,
                "start_time": s.start_time.isoformat(),
                "status": s.status.value,
                "priority": s.priority
            }
            for s in schedules
        ]
    }


@router.get("/{schedule_id}", response_model=ScheduleResponse)
async def get_schedule(
    schedule_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get a specific schedule by ID
    
    **Path Parameters:**
    - `schedule_id`: UUID of the schedule
    
    **Returns:**
    - Complete schedule details
    
    **Errors:**
    - `404`: Schedule not found
    """
    result = await db.execute(
        select(Schedule).where(
            and_(
                Schedule.id == schedule_id,
                Schedule.user_id == current_user.id
            )
        )
    )
    schedule = result.scalar_one_or_none()
    
    if not schedule:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Schedule not found"
        )
    
    return schedule


@router.put("/{schedule_id}", response_model=ScheduleResponse)
async def update_schedule(
    schedule_id: str,
    schedule_data: ScheduleUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Update a schedule
    
    Updates one or more fields of an existing schedule.
    All fields in the request body are optional.
    
    **Path Parameters:**
    - `schedule_id`: UUID of the schedule
    
    **Request Body:** (all fields optional)
    - `title`: New title
    - `description`: New description
    - `schedule_type`: New type
    - `status`: New status
    - `start_time`: New start time
    - `end_time`: New end time
    - `all_day`: New all-day flag
    - `location`: New location
    - `priority`: New priority (1-5)
    - `tags`: New tags array
    - `color`: New color
    - `reminder_minutes`: New reminder settings
    
    **Returns:**
    - Updated schedule
    
    **Errors:**
    - `404`: Schedule not found
    
    **Example:**
    ```json
    {
        "title": "영어 단어 복습",
        "priority": 1
    }
    ```
    """
    result = await db.execute(
        select(Schedule).where(
            and_(
                Schedule.id == schedule_id,
                Schedule.user_id == current_user.id
            )
        )
    )
    schedule = result.scalar_one_or_none()
    
    if not schedule:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Schedule not found"
        )
    
    # Update only provided fields
    update_data = schedule_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(schedule, field, value)
    
    await db.commit()
    await db.refresh(schedule)
    
    return schedule


@router.delete("/{schedule_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_schedule(
    schedule_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Delete a schedule
    
    Permanently deletes a schedule from the database.
    
    **Path Parameters:**
    - `schedule_id`: UUID of the schedule
    
    **Returns:**
    - No content (204)
    
    **Errors:**
    - `404`: Schedule not found
    """
    result = await db.execute(
        select(Schedule).where(
            and_(
                Schedule.id == schedule_id,
                Schedule.user_id == current_user.id
            )
        )
    )
    schedule = result.scalar_one_or_none()
    
    if not schedule:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Schedule not found"
        )
    
    await db.delete(schedule)
    await db.commit()
    
    return None


@router.post("/{schedule_id}/complete", response_model=ScheduleResponse)
async def complete_schedule(
    schedule_id: str,
    completion_note: Optional[str] = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Mark a schedule as completed
    
    Changes the schedule status to "completed" and records the completion time.
    Optionally adds a completion note.
    
    **Path Parameters:**
    - `schedule_id`: UUID of the schedule
    
    **Request Body:**
    ```json
    {
        "completion_note": "잘 마쳤어요!"
    }
    ```
    
    **Returns:**
    - Updated schedule with:
        - `status`: "completed"
        - `completed_at`: Current timestamp
        - `completion_note`: Optional note
    
    **Errors:**
    - `404`: Schedule not found
    
    **Example Response:**
    ```json
    {
        "id": "uuid",
        "status": "completed",
        "completed_at": "2025-01-30T20:00:00Z",
        "completion_note": "잘 마쳤어요!"
    }
    ```
    """
    result = await db.execute(
        select(Schedule).where(
            and_(
                Schedule.id == schedule_id,
                Schedule.user_id == current_user.id
            )
        )
    )
    schedule = result.scalar_one_or_none()
    
    if not schedule:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Schedule not found"
        )
    
    # Update schedule
    schedule.status = ScheduleStatus.COMPLETED
    schedule.completed_at = datetime.utcnow()
    if completion_note:
        schedule.completion_note = completion_note
    
    await db.commit()
    await db.refresh(schedule)
    
    return schedule