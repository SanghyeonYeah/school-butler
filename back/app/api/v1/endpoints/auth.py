"""
Authentication API Endpoints
Handles user registration, login, and preferences management
"""
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from datetime import datetime

from app.core.database import get_db
from app.models.user import User, UserPreferences
from app.schemas import UserCreate, UserResponse, UserPreferencesUpdate
from app.middleware.auth import get_current_user

router = APIRouter()


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register_user(
    user_data: UserCreate,
    db: AsyncSession = Depends(get_db)
):
    """
    Register a new user after Firebase authentication
    
    This endpoint should be called after the user successfully authenticates
    with Firebase. It creates the user record in our database and initializes
    default preferences.
    
    **Request Body:**
    - `firebase_uid`: Firebase user ID (required)
    - `email`: User email (required)
    - `display_name`: Display name (optional)
    - `timezone`: User timezone (default: "Asia/Seoul")
    
    **Returns:**
    - Created user object
    
    **Errors:**
    - `400`: User already registered
    
    **Example:**
    ```json
    {
        "firebase_uid": "firebase_user_123",
        "email": "user@example.com",
        "display_name": "홍길동",
        "timezone": "Asia/Seoul"
    }
    ```
    
    **Note:**
    This endpoint does not require authentication since the user
    is being created for the first time.
    """
    # Check if user already exists
    result = await db.execute(
        select(User).where(User.firebase_uid == user_data.firebase_uid)
    )
    existing_user = result.scalar_one_or_none()
    
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="User already registered",
            headers={"X-Error-Code": "AUTH_004"}
        )
    
    # Create new user
    new_user = User(
        firebase_uid=user_data.firebase_uid,
        email=user_data.email,
        display_name=user_data.display_name,
        timezone=user_data.timezone,
        last_login_at=datetime.utcnow()
    )
    
    db.add(new_user)
    await db.flush()
    
    # Create default preferences
    preferences = UserPreferences(user_id=new_user.id)
    db.add(preferences)
    
    await db.commit()
    await db.refresh(new_user)
    
    return new_user


@router.get("/me")
async def get_current_user_info(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Get current authenticated user's information
    
    Returns complete user profile including preferences.
    
    **Headers:**
    - `Authorization: Bearer <firebase_token>` (required)
    
    **Returns:**
    ```json
    {
        "id": "uuid",
        "email": "user@example.com",
        "display_name": "홍길동",
        "timezone": "Asia/Seoul",
        "created_at": "2025-01-30T12:00:00Z",
        "preferences": {
            "morning_setup_time": "08:00:00",
            "night_review_time": "22:00:00",
            "focus_session_duration": 25,
            "break_duration": 5,
            "enable_gentle_reminders": true,
            "enable_push_notifications": true,
            "character_personality": "friendly"
        }
    }
    ```
    
    **Errors:**
    - `401`: Invalid or missing authentication token
    - `404`: User not found
    """
    # Get user preferences
    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == current_user.id)
    )
    preferences = result.scalar_one_or_none()
    
    # Build response
    return {
        "id": str(current_user.id),
        "email": current_user.email,
        "display_name": current_user.display_name,
        "timezone": current_user.timezone,
        "created_at": current_user.created_at.isoformat() + "Z",
        "preferences": {
            "morning_setup_time": str(preferences.morning_setup_time) if preferences and preferences.morning_setup_time else "08:00:00",
            "night_review_time": str(preferences.night_review_time) if preferences and preferences.night_review_time else "22:00:00",
            "focus_session_duration": preferences.focus_session_duration if preferences else 25,
            "break_duration": preferences.break_duration if preferences else 5,
            "enable_gentle_reminders": preferences.enable_gentle_reminders if preferences else True,
            "enable_push_notifications": preferences.enable_push_notifications if preferences else True,
            "character_personality": preferences.character_personality if preferences else "friendly",
        }
    }


@router.put("/preferences")
async def update_user_preferences(
    preferences_data: UserPreferencesUpdate,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Update user preferences
    
    Updates one or more user preference settings.
    All fields are optional - only provided fields will be updated.
    
    **Headers:**
    - `Authorization: Bearer <firebase_token>` (required)
    
    **Request Body:** (all fields optional)
    - `morning_setup_time`: Morning setup time (HH:MM:SS)
    - `night_review_time`: Night review time (HH:MM:SS)
    - `focus_session_duration`: Default focus duration in minutes (1-240)
    - `break_duration`: Default break duration in minutes (1-60)
    - `enable_gentle_reminders`: Enable gentle reminders (boolean)
    - `enable_push_notifications`: Enable push notifications (boolean)
    - `character_personality`: Character personality ("friendly", "motivating", "calm")
    
    **Returns:**
    ```json
    {
        "message": "Preferences updated successfully"
    }
    ```
    
    **Example:**
    ```json
    {
        "morning_setup_time": "09:00:00",
        "focus_session_duration": 30,
        "character_personality": "motivating"
    }
    ```
    
    **Errors:**
    - `401`: Invalid or missing authentication token
    - `422`: Invalid field values
    """
    # Get existing preferences
    result = await db.execute(
        select(UserPreferences).where(UserPreferences.user_id == current_user.id)
    )
    preferences = result.scalar_one_or_none()
    
    if not preferences:
        # Create preferences if they don't exist
        preferences = UserPreferences(user_id=current_user.id)
        db.add(preferences)
    
    # Update only provided fields
    update_data = preferences_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(preferences, field, value)
    
    await db.commit()
    
    return {
        "message": "Preferences updated successfully"
    }


@router.post("/logout")
async def logout(current_user: User = Depends(get_current_user)):
    """
    Logout endpoint
    
    This endpoint is mainly for logging purposes. Actual token invalidation
    happens on the client side with Firebase.
    
    **Headers:**
    - `Authorization: Bearer <firebase_token>` (required)
    
    **Returns:**
    ```json
    {
        "message": "Logged out successfully"
    }
    ```
    
    **Note:**
    The client should:
    1. Call this endpoint
    2. Call Firebase signOut()
    3. Clear local token storage
    """
    return {
        "message": "Logged out successfully"
    }