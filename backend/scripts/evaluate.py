"""
Evaluation and Benchmarking Script for SentinelPulse.
Compares Heuristic Analyzer vs Trained ML Model on Test Dataset.
"""

import os
import json
import pandas as pd
import numpy as np
import joblib
from sklearn.metrics import classification_report, roc_auc_score, confusion_matrix

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

def evaluate():
    base_dir = os.path.dirname(__file__)
    test_path = os.path.join(base_dir, "..", "data", "test_windows.csv")
    model_path = os.path.join(base_dir, "..", "models_artifacts", "ransomware_detector.joblib")

    if not os.path.exists(test_path):
        print(f"Test dataset not found at {test_path}. Please run generate_dataset.py first.")
        return

    test_df = pd.read_csv(test_path)
    X_test = test_df[FEATURE_COLUMNS].values
    y_test = test_df["label"].values

    if not os.path.exists(model_path):
        print(f"Trained model not found at {model_path}. Please run train_model.py first.")
        return

    artifact = joblib.load(model_path)
    clf = artifact["model"]
    scaler = artifact.get("scaler")

    X_test_scaled = scaler.transform(X_test) if scaler else X_test
    y_pred_ml = clf.predict(X_test_scaled)
    y_prob_ml = clf.predict_proba(X_test_scaled)[:, 1]

    roc_auc = roc_auc_score(y_test, y_prob_ml)

    print("================ ML Model (RandomForest) Performance ================")
    print(classification_report(y_test, y_pred_ml, target_names=["Normal/Burst", "Ransomware"]))
    print(f"ROC-AUC Score: {roc_auc:.4f}")

    tn, fp, fn, tp = confusion_matrix(y_test, y_pred_ml).ravel()
    fpr = fp / (fp + tn) if (fp + tn) > 0 else 0
    print(f"False Positive Rate (FPR): {fpr:.4%}")
    print(f"Miss Rate (False Negative Rate): {fn / (fn + tp):.4%}")
    print("=====================================================================")

if __name__ == "__main__":
    evaluate()
