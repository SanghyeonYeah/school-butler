"""
Other Database Models
Contains models for focus sessions, reviews, character, AI conversations, and notifications
"""
from sqlalchemy import Column, String, DateTime, Boolean, Integer, Text, Enum, ForeignKey, Date
from sqlalchemy.dialects.postgresql import UUID, JSONB
from sqlalchemy.sql import func
import uuid
import enum

from app.core.database import Base


# ==================== Enums ====================

class SessionStatus(str, enum.Enum):
    """Focus session status"""
    ACTIVE = "active"
    COMPLETED = "completed"
    CANCELLED = "cancelled"


class CharacterActivity(str, enum.Enum):
    """Character activity states"""
    IDLE = "idle"
    FOCUS = "focus"
    BREAK = "break"
    NOTIFY = "notify"
    CELEBRATE = "celebrate"


class CharacterEmotion(str, enum.Enum):
    """Character emotional states"""
    NORMAL = "normal"
    HAPPY = "happy"
    PROUD = "proud"
    TIRED = "tired"
    WORRIED = "worried"
    EXCITED = "excited"


class NotificationType(str, enum.Enum):
    """Types of notifications"""
    REMINDER = "reminder"
    MORNING_SETUP = "morning_setup"
    NIGHT_REVIEW = "night_review"
    FOCUS_START = "focus_start"
    FOCUS_END = "focus_end"
    ACHIEVEMENT = "achievement"


class NotificationStatus(str, enum.Enum):
    """Notification delivery status"""
    PENDING = "pending"
    SENT = "sent"
    FAILED = "failed"
    CANCELLED = "cancelled"


# ==================== Focus Sessions ====================

class FocusSession(Base):
    """
    Focus session tracking for pomodoro-style work sessions
    
    Tracks planned and actual duration, breaks, and completion status.
    Can be linked to schedules or todos.
    """
    __tablename__ = "focus_sessions"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True)
    schedule_id = Column(UUID(as_uuid=True), ForeignKey('schedules.id', ondelete='SET NULL'))
    todo_id = Column(UUID(as_uuid=True), ForeignKey('todos.id', ondelete='SET NULL'))
    status = Column(Enum(SessionStatus), nullable=False, default=SessionStatus.ACTIVE, index=True)
    planned_duration = Column(Integer, nullable=False)  # minutes
    actual_duration = Column(Integer)  # minutes
    break_count = Column(Integer, default=0)
    started_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now(), index=True)
    ended_at = Column(DateTime(timezone=True))
    notes = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


# ==================== Daily Reviews ====================

class DailyReview(Base):
    """
    Daily review tracking with AI-generated feedback
    
    Stores user reflections, ratings, and automatically calculated statistics
    for each day including completion rates and focus time.
    """
    __tablename__ = "daily_reviews"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True)
    review_date = Column(Date, nullable=False, index=True)
    rating = Column(Integer)  # 1-5
    mood_keyword = Column(String(50))
    reflection = Column(Text)
    total_focus_minutes = Column(Integer, default=0)
    completed_schedules_count = Column(Integer, default=0)
    completed_todos_count = Column(Integer, default=0)
    total_schedules_count = Column(Integer, default=0)
    total_todos_count = Column(Integer, default=0)
    achievements = Column(JSONB)  # Store achievement data as JSON
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())


# ==================== Character States ====================

class CharacterState(Base):
    """
    Character state tracking for UI/IoT synchronization
    
    Tracks the character's current activity, emotion, and display settings.
    States expire after a set duration.
    """
    __tablename__ = "character_states"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True)
    activity = Column(Enum(CharacterActivity), nullable=False, default=CharacterActivity.IDLE)
    emotion = Column(Enum(CharacterEmotion), nullable=False, default=CharacterEmotion.NORMAL)
    message = Column(Text)
    led_color = Column(String(7))  # hex color for IoT devices
    led_pattern = Column(String(50))  # solid, blink, pulse
    animation_key = Column(String(100))  # Key for animation lookup
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    expires_at = Column(DateTime(timezone=True))


# ==================== AI Conversations ====================

class AIConversation(Base):
    """
    AI conversation history and analytics
    
    Stores all interactions with the AI assistant including:
    - Chat messages
    - Schedule parsing requests
    - Plan generation
    - Performance metrics (tokens, response time)
    """
    __tablename__ = "ai_conversations"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True)
    session_id = Column(UUID(as_uuid=True), nullable=False, index=True)  # Group related conversations
    user_message = Column(Text, nullable=False)
    ai_response = Column(Text, nullable=False)
    intent = Column(String(100))  # chat, parse_schedule, generate_plan, etc.
    context_data = Column(JSONB)  # Additional context as JSON
    model_used = Column(String(50), default='gemini-1.5-flash')
    tokens_used = Column(Integer)
    response_time_ms = Column(Integer)
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)


# ==================== Notifications ====================

class Notification(Base):
    """
    Notification queue for Firebase Cloud Messaging
    
    Manages scheduled and sent notifications including:
    - Reminders
    - Morning/night prompts
    - Focus session alerts
    - Achievement celebrations
    """
    __tablename__ = "notifications"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True)
    schedule_id = Column(UUID(as_uuid=True), ForeignKey('schedules.id', ondelete='CASCADE'))
    todo_id = Column(UUID(as_uuid=True), ForeignKey('todos.id', ondelete='CASCADE'))
    notification_type = Column(Enum(NotificationType), nullable=False)
    status = Column(Enum(NotificationStatus), nullable=False, default=NotificationStatus.PENDING, index=True)
    title = Column(String(255), nullable=False)
    body = Column(Text, nullable=False)
    scheduled_at = Column(DateTime(timezone=True), nullable=False, index=True)
    sent_at = Column(DateTime(timezone=True))
    fcm_message_id = Column(String(255))  # Firebase Cloud Messaging message ID
    error_message = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


# ==================== Analytics Events ====================

class AnalyticsEvent(Base):
    """
    User behavior analytics tracking
    
    Tracks user interactions and behaviors for analytics:
    - Feature usage
    - User engagement
    - Performance metrics
    """
    __tablename__ = "analytics_events"
    
    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey('users.id', ondelete='CASCADE'), index=True)
    event_type = Column(String(100), nullable=False, index=True)  # page_view, button_click, etc.
    event_name = Column(String(255), nullable=False)
    properties = Column(JSONB)  # Event properties as JSON
    timestamp = Column(DateTime(timezone=True), server_default=func.now(), index=True)