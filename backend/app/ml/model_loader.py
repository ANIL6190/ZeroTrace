import json
import logging
import numpy as np
import joblib
from typing import Optional, Tuple
from app.core.config import settings
from app.models.schemas import WindowMetricsRequest, EvaluationMetrics

logger = logging.getLogger(__name__)

# Feature column order used during training
FEATURE_COLUMNS = [
    "write_count",
    "rename_count",
    "delete_count",
    "unique_file_count",
    "suspicious_rename_count",
    "write_rate_ops_per_sec",
    "consecutive_alert_count",
    "total_bytes_written"
]

class MLModelManager:
    def __init__(self):
        self.model = None
        self.scaler = None
        self.metrics: Optional[EvaluationMetrics] = None
        self.load_model()

    def load_model(self):
        """Loads trained scikit-learn model and scaler if available."""
        if settings.MODEL_PATH.exists():
            try:
                artifact = joblib.load(settings.MODEL_PATH)
                if isinstance(artifact, dict) and "model" in artifact:
                    self.model = artifact["model"]
                    self.scaler = artifact.get("scaler")
                else:
                    self.model = artifact
                logger.info(f"Loaded trained model from {settings.MODEL_PATH}")
            except Exception as e:
                logger.warning(f"Could not load model from {settings.MODEL_PATH}: {e}. Using heuristic fallback.")
                self.model = None
        else:
            logger.info("No serialized model found. Using heuristic fallback.")

        if settings.METRICS_PATH.exists():
            try:
                with open(settings.METRICS_PATH, "r") as f:
                    data = json.load(f)
                    self.metrics = EvaluationMetrics(**data)
            except Exception as e:
                logger.warning(f"Could not load evaluation metrics: {e}")

    def extract_features(self, metrics: WindowMetricsRequest) -> np.ndarray:
        return np.array([[
            metrics.write_count,
            metrics.rename_count,
            metrics.delete_count,
            metrics.unique_file_count,
            metrics.suspicious_rename_count,
            metrics.write_rate_ops_per_sec,
            metrics.consecutive_alert_count,
            metrics.total_bytes_written
        ]])

    def predict_risk(self, metrics: WindowMetricsRequest) -> Tuple[float, float, str]:
        """
        Returns (risk_score: 0.0-1.0, confidence: 0.0-1.0, model_version: str)
        """
        features = self.extract_features(metrics)

        if self.model is not None:
            try:
                if self.scaler is not None:
                    scaled = self.scaler.transform(features)
                else:
                    scaled = features

                if hasattr(self.model, "predict_proba"):
                    probs = self.model.predict_proba(scaled)[0]
                    # Probability of class 1 (Ransomware/Suspicious)
                    risk_score = float(probs[1]) if len(probs) > 1 else float(probs[0])
                    confidence = float(np.max(probs))
                else:
                    # Decision function fallback (e.g., SVM or Isolation Forest)
                    pred = self.model.predict(scaled)[0]
                    risk_score = 1.0 if pred == 1 else 0.0
                    confidence = 0.85

                return round(risk_score, 4), round(confidence, 4), "RandomForest-v1.0"
            except Exception as e:
                logger.error(f"Inference error: {e}. Falling back to heuristic scoring.")

        # Heuristic fallback matching Android client baseline
        write_score = min(1.0, metrics.write_rate_ops_per_sec / (settings.BASELINE_WRITE_RATE * 3.0))
        rename_score = min(1.0, metrics.rename_count / (settings.BASELINE_RENAMES * 2.0))
        suspicious_score = min(1.0, metrics.suspicious_rename_count / 1.0)
        persistence_score = min(1.0, metrics.consecutive_alert_count / 2.0)

        risk_score = max(
            write_score * 0.35 + rename_score * 0.25 + suspicious_score * 0.40,
            suspicious_score,
            min(1.0, persistence_score * 0.8) if (write_score > 0.5 or rename_score > 0.5) else 0.0
        )
        return round(float(risk_score), 4), 0.88, "Heuristic-Rule-Engine"

ml_manager = MLModelManager()
