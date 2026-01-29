# middleware/auth.py
from firebase_admin import auth, credentials
import firebase_admin

cred = credentials.Certificate("path/to/serviceAccountKey.json")
firebase_admin.initialize_app(cred)

async def verify_firebase_token(token: str):
    try:
        decoded_token = auth.verify_id_token(token)
        return decoded_token['uid']
    except Exception as e:
        raise HTTPException(status_code=401, detail="Invalid token")

# FastAPI Dependency
async def get_current_user(
    authorization: str = Header(...)
):
    if not authorization.startswith('Bearer '):
        raise HTTPException(status_code=401)
    
    token = authorization.split('Bearer ')[1]
    firebase_uid = await verify_firebase_token(token)
    
    # DB에서 사용자 조회
    user = await db.fetch_one(
        "SELECT * FROM users WHERE firebase_uid = $1",
        firebase_uid
    )
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    return user