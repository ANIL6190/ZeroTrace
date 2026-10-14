from typing import List, Tuple
from app.models.schemas import WindowMetricsRequest
from app.core.config import settings

class ExplainabilityService:
    @staticmethod
    def generate_explanations(
        metrics: WindowMetricsRequest,
        risk_score: float
    ) -> Tuple[List[str], str, bool]:
        reasons: List[str] = []
        is_stage1 = False
        is_stage2 = False

        # 1. Check Burst (Stage 1) Indicators
        if metrics.write_rate_ops_per_sec >= settings.BASELINE_WRITE_RATE:
            is_stage1 = True
            reasons.append(
                f"High Velocity Burst: Write rate of {metrics.write_rate_ops_per_sec:.2f} ops/sec exceeded baseline threshold ({settings.BASELINE_WRITE_RATE:.2f} ops/sec)."
            )

        if metrics.rename_count >= settings.BASELINE_RENAMES:
            is_stage1 = True
            reasons.append(
                f"Mass File Renaming: {metrics.rename_count} file renames detected in window (threshold: {settings.BASELINE_RENAMES})."
            )

        if metrics.suspicious_rename_count >= settings.BASELINE_SUSPICIOUS_EXT:
            is_stage1 = True
            reasons.append(
                f"Suspicious Extension Transformation: {metrics.suspicious_rename_count} files renamed with known ransomware extensions (.locked, .crypto)."
            )

        # 2. Check Stage 2 Persistence & Spread Indicators
        if metrics.consecutive_alert_count >= settings.STAGE2_CONSECUTIVE_WINDOWS:
            is_stage2 = True
            reasons.append(
                f"Sustained Threat Persistence: Suspicious pattern persisted across {metrics.consecutive_alert_count} consecutive sliding windows."
            )

        if metrics.unique_file_count >= settings.STAGE2_UNIQUE_FILES and is_stage1:
            is_stage2 = True
            reasons.append(
                f"Wide Multi-File Target Spread: Operations spanned {metrics.unique_file_count} distinct files (threshold: {settings.STAGE2_UNIQUE_FILES})."
            )

        # 3. Model score correlation
        if risk_score >= 0.75 or (is_stage1 and is_stage2) or metrics.suspicious_rename_count > 0:
            alert_level = "STAGE2_WARNING"
            persistence_verified = True
        elif risk_score >= 0.40 or is_stage1:
            alert_level = "STAGE1_BURST"
            persistence_verified = False
        else:
            alert_level = "NORMAL"
            persistence_verified = False

        if not reasons:
            reasons.append("File operations within baseline normal activity limits.")

        return reasons, alert_level, persistence_verified
