"""
API v1 Endpoints Package

This package contains all API endpoint modules for version 1 of the API.

Available endpoint modules:
- auth: User authentication and preferences
- schedules: Schedule management (CRUD, filtering, statistics)
- todos: Todo management (CRUD, completion tracking)
- ai: AI assistant (parsing, chat, planning)
- focus: Focus session tracking and statistics
- reviews: Daily reviews with AI feedback
- character: Character state management

Each module exports a FastAPI router that is included in the main API router.
"""

# Import all endpoint routers for easy access
from app.api.v1.endpoints import auth
from app.api.v1.endpoints import schedules
from app.api.v1.endpoints import todos
from app.api.v1.endpoints import ai
from app.api.v1.endpoints import focus
from app.api.v1.endpoints import reviews
from app.api.v1.endpoints import character

__all__ = [
    "auth",
    "schedules",
    "todos",
    "ai",
    "focus",
    "reviews",
    "character"
]