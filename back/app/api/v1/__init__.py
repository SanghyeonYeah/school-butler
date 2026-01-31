"""
API v1 Package

This is the main API version 1 package containing all routes and endpoints.

Structure:
- router.py: Main API router that combines all endpoint routers
- endpoints/: Individual endpoint modules (auth, schedules, todos, etc.)

The api_router from router.py is included in the main FastAPI application
with the /v1 prefix.

Example usage in main.py:
    from app.api.v1 import api_router
    app.include_router(api_router, prefix="/v1")
"""

from app.api.v1.router import api_router

__all__ = ["api_router"]