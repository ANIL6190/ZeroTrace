# SentinelPulse — Python ML Early Warning Backend

This service provides real-time, explainable Machine Learning threat scoring for **SentinelPulse / ZeroTrace** mobile file-system telemetry.

---

## 🚀 Quick Start (Local Python)

### 1. Set Up Virtual Environment
```bash
cd backend
python -m venv venv
# Windows:
.\venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
```

### 2. Generate Dataset & Train Model
```bash
# Generate synthetic sliding-window telemetry dataset (train/test split)
python scripts/generate_dataset.py

# Train Random Forest classifier & serialize to models_artifacts/
python scripts/train_model.py

# Benchmark evaluation metrics (Precision, Recall, ROC-AUC, FPR)
python scripts/evaluate.py
```

### 3. Run FastAPI Server
```bash
python main.py
# Or with uvicorn directly:
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
Interactive Swagger documentation is available at: **`http://localhost:8000/docs`**

---

## 🐳 Docker Deployment

Build and run using Docker Compose:
```bash
docker-compose up --build
```
Or directly with Docker:
```bash
docker build -t sentinelpulse-backend .
docker run -p 8000:8000 sentinelpulse-backend
```

---

## 📱 Connecting from Android (SentinelPulse)

| Environment | Base URL | Note |
|---|---|---|
| **Android Studio Emulator** | `http://10.0.2.2:8000/` | `10.0.2.2` automatically routes to host machine `127.0.0.1` |
| **Physical Device (Same Wi-Fi)** | `http://<HOST_LAN_IP>:8000/` | Find your IP with `ipconfig` (Windows) or `ifconfig` |

### API Endpoint: `POST /api/v1/analyze-window`

**Sample Request Payload (supports camelCase or snake_case):**
```json
{
  "windowDurationMs": 5000,
  "totalOperations": 18,
  "fileWrites": 12,
  "fileRenames": 5,
  "fileDeletes": 1,
  "uniqueFiles": 6,
  "suspiciousRenamesCount": 3,
  "writeRate": 3.6,
  "consecutiveBurstCount": 2,
  "totalBytesWritten": 450000
}
```

**Sample Response Payload:**
```json
{
  "risk_score": 0.9412,
  "alert_level": "STAGE2_WARNING",
  "confidence": 0.9412,
  "explanation_reasons": [
    "High Velocity Burst: Write rate of 3.60 ops/sec exceeded baseline threshold (1.60 ops/sec).",
    "Mass File Renaming: 5 file renames detected in window (threshold: 3).",
    "Suspicious Extension Transformation: 3 files renamed with known ransomware extensions (.locked, .crypto).",
    "Sustained Threat Persistence: Suspicious pattern persisted across 2 consecutive sliding windows.",
    "Wide Multi-File Target Spread: Operations spanned 6 distinct files (threshold: 5)."
  ],
  "persistence_verified": true,
  "model_version": "RandomForest-v1.0"
}
```

---

## 🧪 Running Unit Tests
```bash
pytest tests/ -v
```
