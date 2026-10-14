import pytest
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_root():
    response = client.get("/")
    assert response.status_code == 200
    data = response.json()
    assert "SentinelPulse" in data["service"]

def test_health_check():
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"

def test_analyze_normal_workload():
    payload = {
        "window_duration_ms": 5000,
        "write_count": 1,
        "rename_count": 0,
        "delete_count": 0,
        "unique_file_count": 1,
        "suspicious_rename_count": 0,
        "write_rate_ops_per_sec": 0.2,
        "consecutive_alert_count": 0,
        "total_bytes_written": 2048
    }
    response = client.post("/api/v1/analyze-window", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["alert_level"] == "NORMAL"
    assert data["risk_score"] < 0.40
    assert not data["persistence_verified"]
    assert len(data["explanation_reasons"]) > 0

def test_analyze_stage1_burst():
    payload = {
        "window_duration_ms": 5000,
        "write_count": 12,
        "rename_count": 4,
        "delete_count": 0,
        "unique_file_count": 4,
        "suspicious_rename_count": 0,
        "write_rate_ops_per_sec": 3.2,
        "consecutive_alert_count": 1,
        "total_bytes_written": 150000
    }
    response = client.post("/api/v1/analyze-window", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["alert_level"] in ["STAGE1_BURST", "STAGE2_WARNING"]
    assert data["risk_score"] >= 0.40
    assert any("Burst" in r or "Renaming" in r for r in data["explanation_reasons"])

def test_analyze_stage2_ransomware_persistence():
    payload = {
        "window_duration_ms": 5000,
        "write_count": 25,
        "rename_count": 12,
        "delete_count": 1,
        "unique_file_count": 12,
        "suspicious_rename_count": 8,
        "write_rate_ops_per_sec": 7.6,
        "consecutive_alert_count": 3,
        "total_bytes_written": 850000
    }
    response = client.post("/api/v1/analyze-window", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["alert_level"] == "STAGE2_WARNING"
    assert data["risk_score"] >= 0.70
    assert data["persistence_verified"] is True
    assert any("Persistence" in r or "Suspicious" in r for r in data["explanation_reasons"])

def test_android_camelcase_schema_compatibility():
    # Test sending exact JSON keys serialized by Android Retrofit/Gson
    android_payload = {
        "windowDurationMs": 5000,
        "totalOperations": 18,
        "fileCreates": 0,
        "fileWrites": 14,
        "fileRenames": 4,
        "fileDeletes": 0,
        "uniqueFiles": 6,
        "suspiciousRenamesCount": 3,
        "writeRate": 3.6,
        "consecutiveBurstCount": 2,
        "totalBytesWritten": 320000
    }
    response = client.post("/api/v1/analyze-window", json=android_payload)
    assert response.status_code == 200
    data = response.json()
    assert data["alert_level"] == "STAGE2_WARNING"
    assert data["persistence_verified"] is True

def test_evaluation_report():
    response = client.get("/api/v1/evaluation-report")
    assert response.status_code == 200
    data = response.json()
    assert "accuracy" in data
    assert "precision" in data
    assert "recall" in data
    assert "false_positive_rate" in data
