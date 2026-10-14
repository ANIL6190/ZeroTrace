from typing import List, Optional
from pydantic import BaseModel, Field, ConfigDict

class WindowMetricsRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    window_duration_ms: int = Field(default=5000, alias="windowDurationMs")
    total_operations: int = Field(default=0, alias="totalOperations")
    file_creates: int = Field(default=0, alias="fileCreates")
    write_count: int = Field(default=0, alias="fileWrites")
    rename_count: int = Field(default=0, alias="fileRenames")
    delete_count: int = Field(default=0, alias="fileDeletes")
    unique_file_count: int = Field(default=0, alias="uniqueFiles")
    suspicious_rename_count: int = Field(default=0, alias="suspiciousRenamesCount")
    write_rate_ops_per_sec: float = Field(default=0.0, alias="writeRate")
    consecutive_alert_count: int = Field(default=0, alias="consecutiveBurstCount")
    total_bytes_written: int = Field(default=0, alias="totalBytesWritten")

class AnalysisResponse(BaseModel):
    risk_score: float = Field(..., description="Calculated threat probability from 0.0 to 1.0")
    alert_level: str = Field(..., description="NORMAL, STAGE1_BURST, or STAGE2_WARNING")
    confidence: float = Field(..., description="Model confidence score")
    explanation_reasons: List[str] = Field(default_factory=list, description="Human interpretable detection reasons")
    persistence_verified: bool = Field(..., description="True if burst pattern persists across multiple windows")
    model_version: str = Field(default="1.0.0", description="Active ML model version or fallback")

class EvaluationMetrics(BaseModel):
    model_name: str
    accuracy: float
    precision: float
    recall: float
    f1_score: float
    false_positive_rate: float
    total_samples: int
    test_samples: int
    trained_at: Optional[str] = None
