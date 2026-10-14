from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    PROJECT_NAME: str = "SentinelPulse ML Early Warning API"
    VERSION: str = "1.0.0"
    API_V1_PREFIX: str = "/api/v1"
    
    BASE_DIR: Path = Path(__file__).resolve().parent.parent.parent
    MODEL_DIR: Path = BASE_DIR / "models_artifacts"
    MODEL_PATH: Path = MODEL_DIR / "ransomware_detector.joblib"
    METRICS_PATH: Path = MODEL_DIR / "model_metrics.json"
    DATA_DIR: Path = BASE_DIR / "data"

    # Default heuristic thresholds aligned with Android ZeroTrace lab
    BASELINE_WRITE_RATE: float = 1.6
    BASELINE_RENAMES: int = 3
    BASELINE_SUSPICIOUS_EXT: int = 1
    STAGE2_CONSECUTIVE_WINDOWS: int = 2
    STAGE2_UNIQUE_FILES: int = 5

settings = Settings()
