"""
Custom Exception Classes
Provides specific exception types for better error handling
"""
from fastapi import HTTPException, status


# ==================== AI Service Exceptions ====================

class AIServiceException(HTTPException):
    """Base exception for AI service errors"""
    def __init__(self, detail: str, error_code: str = "AI_ERROR"):
        super().__init__(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=detail,
            headers={"X-Error-Code": error_code}
        )


class GeminiTimeoutError(AIServiceException):
    """Raised when Gemini API times out"""
    def __init__(self):
        super().__init__(
            detail="AI service request timed out. Please try again.",
            error_code="AI_001"
        )


class GeminiRateLimitError(AIServiceException):
    """Raised when Gemini API rate limit is exceeded"""
    def __init__(self):
        super().__init__(
            detail="AI service rate limit exceeded. Please try again later.",
            error_code="AI_002"
        )


class GeminiParsingError(AIServiceException):
    """Raised when Gemini fails to parse input"""
    def __init__(self, detail: str = "Failed to parse input"):
        super().__init__(
            detail=detail,
            error_code="AI_003"
        )


class GeminiResponseError(AIServiceException):
    """Raised when Gemini returns invalid response"""
    def __init__(self):
        super().__init__(
            detail="AI service returned invalid response",
            error_code="AI_004"
        )


# ==================== Database Exceptions ====================

class DatabaseException(HTTPException):
    """Base exception for database errors"""
    def __init__(self, detail: str, error_code: str = "DB_ERROR"):
        super().__init__(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=detail,
            headers={"X-Error-Code": error_code}
        )


class DatabaseConnectionError(DatabaseException):
    """Raised when database connection fails"""
    def __init__(self):
        super().__init__(
            detail="Database connection failed",
            error_code="DB_001"
        )


class DatabaseTransactionError(DatabaseException):
    """Raised when database transaction fails"""
    def __init__(self, detail: str = "Database transaction failed"):
        super().__init__(
            detail=detail,
            error_code="DB_002"
        )


# ==================== Validation Exceptions ====================

class ValidationException(HTTPException):
    """Base exception for validation errors"""
    def __init__(self, detail: str, error_code: str = "VALIDATION_ERROR"):
        super().__init__(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=detail,
            headers={"X-Error-Code": error_code}
        )


class InvalidDateTimeError(ValidationException):
    """Raised when datetime validation fails"""
    def __init__(self, detail: str = "Invalid date or time format"):
        super().__init__(
            detail=detail,
            error_code="VALIDATION_001"
        )


class InvalidPriorityError(ValidationException):
    """Raised when priority is out of range"""
    def __init__(self):
        super().__init__(
            detail="Priority must be between 1 and 5",
            error_code="VALIDATION_002"
        )


class LowConfidenceError(ValidationException):
    """Raised when AI confidence is too low"""
    def __init__(self, confidence: float):
        super().__init__(
            detail=f"AI confidence too low ({confidence:.2f}). Please provide more specific input.",
            error_code="VALIDATION_003"
        )


# ==================== Resource Exceptions ====================

class ResourceNotFoundException(HTTPException):
    """Raised when a resource is not found"""
    def __init__(self, resource_type: str, resource_id: str = None):
        detail = f"{resource_type} not found"
        if resource_id:
            detail += f": {resource_id}"
        
        super().__init__(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=detail,
            headers={"X-Error-Code": f"{resource_type.upper()}_NOT_FOUND"}
        )


class ResourceAlreadyExistsException(HTTPException):
    """Raised when trying to create a duplicate resource"""
    def __init__(self, resource_type: str):
        super().__init__(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"{resource_type} already exists",
            headers={"X-Error-Code": f"{resource_type.upper()}_EXISTS"}
        )