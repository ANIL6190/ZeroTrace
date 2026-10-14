"""
Model Training Script for SentinelPulse Early Warning Engine.
Trains a RandomForestClassifier on telemetry window features and serializes the model.
"""

import os
import json
from datetime import datetime
import pandas as pd
import numpy as np
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix

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

def train():
    data_dir = os.path.join(os.path.dirname(__file__), "..", "data")
    train_path = os.path.join(data_dir, "train_windows.csv")
    test_path = os.path.join(data_dir, "test_windows.csv")
    artifacts_dir = os.path.join(os.path.dirname(__file__), "..", "models_artifacts")
    os.makedirs(artifacts_dir, exist_ok=True)

    if not os.path.exists(train_path):
        print(f"Dataset not found at {train_path}. Generating now...")
        from generate_dataset import create_synthetic_dataset
        create_synthetic_dataset(output_dir=data_dir)

    train_df = pd.read_csv(train_path)
    test_df = pd.read_csv(test_path)

    X_train = train_df[FEATURE_COLUMNS].values
    y_train = train_df["label"].values

    X_test = test_df[FEATURE_COLUMNS].values
    y_test = test_df["label"].values

    # Fit Scaler
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    # Train Random Forest
    print("Training RandomForestClassifier...")
    clf = RandomForestClassifier(
        n_estimators=100,
        max_depth=8,
        min_samples_split=4,
        random_state=42,
        class_weight="balanced"
    )
    clf.fit(X_train_scaled, y_train)

    # Evaluate on Test Set
    y_pred = clf.predict(X_test_scaled)
    acc = float(accuracy_score(y_test, y_pred))
    prec = float(precision_score(y_test, y_pred))
    rec = float(recall_score(y_test, y_pred))
    f1 = float(f1_score(y_test, y_pred))

    tn, fp, fn, tp = confusion_matrix(y_test, y_pred).ravel()
    fpr = float(fp / (fp + tn)) if (fp + tn) > 0 else 0.0

    print("\n================ Model Evaluation Report ================")
    print(f"Accuracy:            {acc:.4f}")
    print(f"Precision:           {prec:.4f}")
    print(f"Recall (Sensitivity):{rec:.4f}")
    print(f"F1 Score:            {f1:.4f}")
    print(f"False Positive Rate: {fpr:.4f}")
    print(f"Confusion Matrix:    TN={tn}, FP={fp}, FN={fn}, TP={tp}")
    print("=========================================================\n")

    # Feature Importances
    importances = clf.feature_importances_
    print("Feature Importances:")
    for col, imp in sorted(zip(FEATURE_COLUMNS, importances), key=lambda x: x[1], reverse=True):
        print(f"  - {col:25s}: {imp:.4f}")

    # Serialize Model & Scaler
    model_output_path = os.path.join(artifacts_dir, "ransomware_detector.joblib")
    joblib.dump({"model": clf, "scaler": scaler}, model_output_path)
    print(f"\nModel artifact saved to {model_output_path}")

    # Save metrics JSON
    metrics_data = {
        "model_name": "RandomForestClassifier",
        "accuracy": round(acc, 4),
        "precision": round(prec, 4),
        "recall": round(rec, 4),
        "f1_score": round(f1, 4),
        "false_positive_rate": round(fpr, 4),
        "total_samples": len(train_df) + len(test_df),
        "test_samples": len(test_df),
        "trained_at": datetime.now().isoformat()
    }
    metrics_output_path = os.path.join(artifacts_dir, "model_metrics.json")
    with open(metrics_output_path, "w") as f:
        json.dump(metrics_data, f, indent=2)
    print(f"Metrics saved to {metrics_output_path}")

if __name__ == "__main__":
    train()
