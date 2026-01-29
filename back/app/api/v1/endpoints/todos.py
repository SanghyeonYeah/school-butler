"""
Todo management API endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, and_
from typing import Optional, List
from datetime import datetime, date

from app.core.database import get_db
from app.models.user import User
from app.models.todo import Todo, TodoStatus
from app.schemas import TodoCreate, TodoUpdate, TodoResponse
from app.middleware.auth import get_current_user

router = APIRouter()


@router.post("", response_model=TodoResponse, status_code=status.HTTP_201_CREATED)
async def create_todo(
    todo_data: TodoCreate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Create a new todo item
    """
    new_todo = Todo(
        user_id=current_user.id,
        **todo_data.model_dump()
    )
    
    db.add(new_todo)
    await db.commit()
    await db.refresh(new_todo)
    
    return new_todo


@router.get("", response_model=List[TodoResponse])
async def get_todos(
    status: Optional[str] = Query(None),
    priority: Optional[int] = Query(None, ge=1, le=5),
    due_date: Optional[date] = Query(None),
    limit: int = Query(50, le=100),
    offset: int = Query(0),
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get list of todos with optional filters
    """
    query = select(Todo).where(Todo.user_id == current_user.id)
    
    # Apply filters
    if status:
        try:
            status_enum = TodoStatus(status)
            query = query.where(Todo.status == status_enum)
        except ValueError:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Invalid status: {status}"
            )
    if priority:
        query = query.where(Todo.priority == priority)
    if due_date:
        query = query.where(Todo.due_date == due_date)
    
    # Order by priority and due date
    query = query.order_by(Todo.priority.asc(), Todo.due_date.asc())
    
    # Apply pagination
    query = query.limit(limit).offset(offset)
    
    result = await db.execute(query)
    todos = result.scalars().all()
    
    return todos


@router.get("/stats/overview")
async def get_todo_stats(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get todo statistics overview
    """
    # Get all todos
    result = await db.execute(
        select(Todo).where(Todo.user_id == current_user.id)
    )
    todos = result.scalars().all()
    
    total_count = len(todos)
    completed_count = sum(1 for t in todos if t.status == TodoStatus.COMPLETED)
    pending_count = sum(1 for t in todos if t.status == TodoStatus.PENDING)
    in_progress_count = sum(1 for t in todos if t.status == TodoStatus.IN_PROGRESS)
    overdue_count = sum(
        1 for t in todos 
        if t.status == TodoStatus.PENDING and t.due_date and t.due_date < date.today()
    )
    
    return {
        "total_todos": total_count,
        "completed": completed_count,
        "pending": pending_count,
        "in_progress": in_progress_count,
        "overdue": overdue_count,
        "completion_rate": completed_count / total_count if total_count > 0 else 0
    }


@router.get("/{todo_id}", response_model=TodoResponse)
async def get_todo(
    todo_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get a specific todo by ID
    """
    result = await db.execute(
        select(Todo).where(
            and_(
                Todo.id == todo_id,
                Todo.user_id == current_user.id
            )
        )
    )
    todo = result.scalar_one_or_none()
    
    if not todo:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Todo not found"
        )
    
    return todo


@router.put("/{todo_id}", response_model=TodoResponse)
async def update_todo(
    todo_id: str,
    todo_data: TodoUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Update a todo
    """
    result = await db.execute(
        select(Todo).where(
            and_(
                Todo.id == todo_id,
                Todo.user_id == current_user.id
            )
        )
    )
    todo = result.scalar_one_or_none()
    
    if not todo:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Todo not found"
        )
    
    # Update fields
    update_data = todo_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(todo, field, value)
    
    await db.commit()
    await db.refresh(todo)
    
    return todo


@router.delete("/{todo_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_todo(
    todo_id: str,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Delete a todo
    """
    result = await db.execute(
        select(Todo).where(
            and_(
                Todo.id == todo_id,
                Todo.user_id == current_user.id
            )
        )
    )
    todo = result.scalar_one_or_none()
    
    if not todo:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Todo not found"
        )
    
    await db.delete(todo)
    await db.commit()
    
    return None


@router.post("/{todo_id}/complete", response_model=TodoResponse)
async def complete_todo(
    todo_id: str,
    actual_duration: Optional[int] = None,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Mark a todo as completed
    """
    result = await db.execute(
        select(Todo).where(
            and_(
                Todo.id == todo_id,
                Todo.user_id == current_user.id
            )
        )
    )
    todo = result.scalar_one_or_none()
    
    if not todo:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Todo not found"
        )
    
    todo.status = TodoStatus.COMPLETED
    todo.completed_at = datetime.utcnow()
    if actual_duration:
        todo.actual_duration = actual_duration
    
    await db.commit()
    await db.refresh(todo)
    
    return todo