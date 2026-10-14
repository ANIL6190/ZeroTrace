# Catching Ransomware-like Behaviour Early
### Android Ransomware-like Activity Early Warning in a Controlled Lab
**AIORI Hackathon 3 — Problem Statement: `A3-PS001-TC197`**

---

## 👥 Team Details

- **Mentor**: **Dr. Manonmani S**
- **Team Members**:
  - **Anil A**
  - **Avinash R**

---

## 📌 Abstract & Problem Statement

Mobile ransomware locks users out of their own files, and it works fast. Any early warning we can provide before mass encryption occurs is vital.

- **The Problem**: Ransomware-like apps write to, rename, and replace large numbers of files in a very short time window. Traditional signature-based detection frequently misses novel or obfuscated variants.
- **The Catch**: Heavy file activity is not always malicious—legitimate workloads like cloud backups, photo syncs, or batch document edits look remarkably similar. Minimizing false alarms while maintaining near-instant reaction time is essential.
- **Our Solution**: Learn what a burst of suspicious file activity looks like from simple counts and rates across sliding time windows, verify whether the behavior persists across consecutive intervals, and issue an explainable warning detailing *why* the alert was triggered.

> ⚠️ **Safety & Ethics Disclaimer**: This is a safe proof-of-concept developed for a controlled lab environment. No real ransomware is executed, and no real user files are ever encrypted or modified. All operations occur strictly within dummy files in the Android application's private sandbox.

---

## 🏗️ System Architecture

```
 ┌─────────────────────────────────────────────────────────────┐
 │                  Android App (SentinelPulse)               │
 │                                                             │
 │   SandboxSimulator ──► TwoStageBehaviorAnalyzer             │
 │   (Controlled Lab)     (Sliding Window: 5000ms)             │
 │                              │                              │
 │                       SlidingWindowMetrics DTO               │
 └──────────────────────────────┼──────────────────────────────┘
                                │ JSON HTTP POST (Retrofit / OkHttp)
                                ▼ (http://10.0.2.2:8000)
 ┌─────────────────────────────────────────────────────────────┐
 │                    Python FastAPI Backend                   │
 │                                                             │
 │   ┌─────────────────┐       ┌───────────────────────────┐   │
 │   │ FastAPI Server  │ ────► │ scikit-learn ML Model     │   │
 │   │ (uvicorn)       │       │ (RandomForest Classifier) │   │
 │   └─────────────────┘       └───────────────────────────┘   │
 │                                           │                 │
 │                             Prediction & Explainability     │
 └───────────────────────────────────────────┼─────────────────┘
                                             │ JSON Response
                                             ▼
                        Android UI (Explainable Warning)
```

---

## 📡 Observed Telemetry Signals

| Signal | Feature Name | Description & Heuristic Importance |
|---|---|---|
| `file_creates` | `fileCreates` | Dummy files created; sudden floods can precede encryption. |
| `file_writes` | `fileWrites` | Write operations; velocity bursts indicate rapid in-place modification. |
| `file_renames` | `fileRenames` | Mass renames (e.g. adding `.locked` or `.crypto` extensions) are a primary indicator. |
| `unique_files` | `uniqueFiles` | Distinct files touched; wide fanout across different directories vs. single-file edit. |
| `write_rate` | `writeRate` | Operations per second; how fast file operations are executed. |
| `persistence` | `consecutiveBurstCount` | Whether burst activity is sustained across consecutive sliding time windows. |

---

## 🧠 Machine Learning Engine & Benchmark Results

The ML backend uses a `RandomForestClassifier` trained on a balanced synthetic dataset of **2,500 telemetry sliding windows** (normal user edits, benign sync bursts, and ransomware encryption sequences).

### Benchmark Performance (500 Test Samples):
> *Measured on held-out synthetic test split representing simulation workloads (normal usage, benign bursts, and ransomware scenarios).*

| Metric | Score | Lab Target | Status |
|---|---|---|---|
| **Accuracy** | **95.6%** | > 90% | ✅ Passed |
| **Precision** | **96.0%** (0.96) | > 90% | ✅ Passed |
| **Recall (Sensitivity)** | **95.0%** (0.95) | > 90% | ✅ Passed |
| **F1 Score** | **0.96** | > 0.90 | ✅ Passed |
| **False Positives (FP)** | **9** (FPR: 2.5%) | < 5% | ✅ Passed |
| **False Negatives (FN)** | **13** (Miss rate: 9.1%) | < 10% | ✅ Passed |
| **ROC-AUC Score** | **0.984** | > 0.90 | ✅ Passed |

### Top Feature Importances:
1. `rename_count` (34.8%)
2. `suspicious_rename_count` (25.0%)
3. `write_rate_ops_per_sec` (11.9%)
4. `consecutive_alert_count` (10.6%)
5. `unique_file_count` (9.1%)
6. `write_count` (4.9%)

---

## 📂 Repository Structure

```
.
├── A3-PS001-TC197 Early Warning.pdf   # AIORI Hackathon presentation deck
├── Proposed-structure-hackathon.pdf  # AIORI competition guidelines
├── backend/                          # Python ML & REST API service
│   ├── app/
│   │   ├── api/endpoints.py          # /analyze-window, /health, /evaluation-report
│   │   ├── core/config.py            # Baselines & configuration
│   │   ├── ml/model_loader.py        # ML inference manager & feature extractor
│   │   ├── models/schemas.py         # Pydantic schemas (supports camelCase & snake_case)
│   │   └── services/explainability.py# Two-stage explainability & reasoning engine
│   ├── data/                         # CSV datasets (train_windows.csv, test_windows.csv)
│   ├── models_artifacts/             # Serialized .joblib model & model_metrics.json
│   ├── scripts/                      # Data generator, training & evaluation scripts
│   ├── tests/test_api.py             # 7 API unit & schema test cases (all passing)
│   ├── Dockerfile                    # Containerization
│   ├── docker-compose.yml            # Docker Compose orchestration (port 8000)
│   ├── main.py                       # FastAPI entrypoint
│   └── requirements.txt              # Backend dependencies
├── src/                              # Android client source code (Kotlin + Jetpack Compose)
│   ├── main/java/com/example/zerotrace/
│   │   ├── analyzer/                 # TwoStageBehaviorAnalyzer sliding-window engine
│   │   ├── data/model/               # FileOpEvent, SlidingWindowMetrics, AlertLevel
│   │   ├── simulator/                # SandboxSimulator (Normal, Burst Storm, Ransomware)
│   │   └── ui/                       # Jetpack Compose dashboards & alerts
│   └── test/java/com/example/zerotrace/
│       └── TwoStageBehaviorAnalyzerTest.kt # Android analyzer unit tests
├── build.gradle.kts                  # Android build configuration
└── README.md                         # Project documentation
```

---

## 🚀 Getting Started

### 1. Python ML Backend

#### Option A: Run Locally
```bash
cd backend
pip install -r requirements.txt

# Run server
python main.py
```
- Interactive Swagger documentation: **`http://localhost:8000/docs`**
- Health status: **`http://localhost:8000/api/v1/health`**
- Model benchmarks: **`http://localhost:8000/api/v1/evaluation-report`**

#### Option B: Run with Docker Compose
```bash
cd backend
docker-compose up --build
```

#### Run Backend Tests:
```bash
cd backend
pytest tests/ -v
```

---

### 2. Android Client (SentinelPulse / ZeroTrace)

1. Open this project directory in **Android Studio** (`File > Open...`).
2. Sync Gradle dependencies.
3. Start an Android Virtual Device (AVD) or connect a physical device via USB.
4. Run the app (`Shift + F10`).
5. Run unit tests by right-clicking `TwoStageBehaviorAnalyzerTest.kt` > **Run**.

---

## 📚 References & Bibliography

- **[1] MITRE ATT&CK**: [Data Encrypted for Impact, Technique T1471 — Mobile](https://attack.mitre.org/techniques/T1471/)
- **[2] Android Open Source Project**: [App Security Best Practices & Sandboxing](https://source.android.com/docs/security/best-practices/app)
- **[3] S. Sharma, R. Kumar and C. Rama Krishna (2021)**: "A survey on analysis and detection of Android ransomware." *Concurrency and Computation: Practice and Experience*, 33, e6272.
- **[4] Procedural Survey (2022)**: "On building machine learning pipelines for Android malware detection: a procedural survey of practices, challenges and opportunities." *Cybersecurity*.
