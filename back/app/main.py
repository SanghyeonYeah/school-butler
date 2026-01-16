from fastapi import FastAPI

app = FastAPI(title="Student Butler Backend")

@app.get("/health")
def health():
    return {"status": "ok"}
