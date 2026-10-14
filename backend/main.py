import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

from app.core.config import settings
from app.api.endpoints import router as api_router
from app.ml.model_loader import ml_manager

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("sentinelpulse")

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing SentinelPulse Backend Service...")
    ml_manager.load_model()
    if ml_manager.model:
        logger.info("ML Model loaded successfully.")
    else:
        logger.info("Running with baseline Heuristic Rule Engine.")
    yield
    logger.info("Shutting down SentinelPulse Backend Service.")

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="Early Warning API Service for Mobile Ransomware Detection & Sliding-Window Analysis",
    lifespan=lifespan
)

# CORS configuration for local testing and dev clients
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router, prefix=settings.API_V1_PREFIX)

@app.get("/")
def root():
    return {
        "service": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "docs_url": "/docs",
        "health_url": f"{settings.API_V1_PREFIX}/health"
    }

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
