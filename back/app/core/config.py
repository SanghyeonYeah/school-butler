"""
Application Configuration
Manages all environment variables and settings
"""
from pydantic_settings import BaseSettings
from pydantic import Field
from typing import List
import os


class Settings(BaseSettings):
    """
    Application settings loaded from environment variables
    """
    
    # Application
    APP_NAME: str = Field(default="Schedule Partner API", description="Application name")
    ENVIRONMENT: str = Field(default="development", description="Environment (development/production)")
    DEBUG: bool = Field(default=True, description="Debug mode")
    
    # Database
    DATABASE_URL: str = Field(
        default="postgresql+asyncpg://admin:password@localhost:5432/schedule_partner",
        description="PostgreSQL connection string with asyncpg driver"
    )
    
    # Firebase Authentication
    FIREBASE_CREDENTIALS_PATH: str = Field(
        default="./serviceAccountKey.json",
        description="Path to Firebase service account key JSON file"
    )
    
    # Gemini AI
    GEMINI_API_KEY: str = Field(
        default="AIzaSyAFyKJ60Wb51sqGPOFd4NMnPefJbFuRFEg",
        description="Google Gemini API key"
    )
    GEMINI_MODEL: str = Field(
        default="gemini-1.5-flash",
        description="Gemini model to use"
    )
    
    # Security
    SECRET_KEY: str = Field(
        default="your-secret-key-change-in-production-use-openssl-rand-hex-32",
        description="Secret key for signing tokens"
    )
    ALGORITHM: str = Field(default="HS256", description="JWT algorithm")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = Field(
        default=10080,  # 7 days
        description="Access token expiration time in minutes"
    )
    
    # CORS
    ALLOWED_ORIGINS: List[str] = Field(
        default=[
            "http://localhost:3000",
            "http://localhost:8080",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:8080"
        ],
        description="Allowed CORS origins"
    )
    
    # Rate Limiting
    RATE_LIMIT_PER_MINUTE: int = Field(
        default=60,
        description="Maximum requests per minute per IP"
    )
    
    # Pagination
    DEFAULT_PAGE_SIZE: int = Field(default=20, description="Default page size for list endpoints")
    MAX_PAGE_SIZE: int = Field(default=100, description="Maximum page size")
    
    # Database Pool
    DB_POOL_SIZE: int = Field(default=5, description="Database connection pool size")
    DB_MAX_OVERFLOW: int = Field(default=10, description="Maximum overflow connections")
    
    # Logging
    LOG_LEVEL: str = Field(default="INFO", description="Logging level")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = True


# Create settings instance
settings = Settings()


# Validate critical settings on startup
def validate_settings():
    """
    Validate critical settings and raise errors if misconfigured
    """
    errors = []
    
    # Check database URL
    if not settings.DATABASE_URL.startswith("postgresql"):
        errors.append("DATABASE_URL must be a PostgreSQL connection string")
    
    # Check Gemini API key
    if settings.GEMINI_API_KEY == "your-gemini-api-key-here":
        errors.append("GEMINI_API_KEY must be set to a valid API key")
    
    # Check Firebase credentials file exists
    if not os.path.exists(settings.FIREBASE_CREDENTIALS_PATH):
        errors.append(f"Firebase credentials file not found at {settings.FIREBASE_CREDENTIALS_PATH}")
    
    # Check secret key in production
    if settings.ENVIRONMENT == "production" and settings.SECRET_KEY == "your-secret-key-change-in-production-use-openssl-rand-hex-32":
        errors.append("SECRET_KEY must be changed in production")
    
    if errors:
        error_msg = "Configuration errors:\n" + "\n".join(f"  - {e}" for e in errors)
        raise ValueError(error_msg)


# Validate settings on import (can be disabled for testing)
if os.getenv("SKIP_VALIDATION") != "true":
    try:
        validate_settings()
    except ValueError as e:
        # Only raise in production, warn in development
        if settings.ENVIRONMENT == "production":
            raise
        else:
            import logging
            logging.warning(f"Configuration warnings: {e}")