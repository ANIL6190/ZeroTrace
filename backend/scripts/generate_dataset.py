"""
Synthetic Dataset Generator for Mobile Ransomware Sliding-Window Detection.
Generates labeled time-series window records mirroring SentinelPulse Android telemetry.

Class 0: Normal Activity & Benign Burst Storms (e.g. Photo backup, Cache cleanup)
Class 1: Ransomware-like Activity (Rapid writes, Mass renames, Suspicious extensions, Multi-file spread)
"""

import os
import random
import pandas as pd
import numpy as np

def generate_normal_window():
    """Simulates casual user file operations (browsing, taking a photo, editing a document)."""
    write_count = random.randint(0, 3)
    rename_count = random.randint(0, 1)
    delete_count = random.randint(0, 1)
    unique_file_count = random.randint(0, 3)
    suspicious_rename_count = 0  # Clean extensions
    write_rate = round(write_count / 5.0, 2)
    consecutive_alert_count = 0
    total_bytes = write_count * random.randint(1024, 65536)

    return {
        "write_count": write_count,
        "rename_count": rename_count,
        "delete_count": delete_count,
        "unique_file_count": unique_file_count,
        "suspicious_rename_count": suspicious_rename_count,
        "write_rate_ops_per_sec": write_rate,
        "consecutive_alert_count": consecutive_alert_count,
        "total_bytes_written": total_bytes,
        "label": 0
    }

def generate_benign_burst_window():
    """Simulates legitimate high-velocity bursts (app update, gallery sync, backup)."""
    write_count = random.randint(4, 15)
    rename_count = random.randint(0, 2)
    delete_count = random.randint(0, 4)
    unique_file_count = random.randint(2, 6)
    suspicious_rename_count = 0  # No suspicious extensions (.jpg, .tmp, .dat)
    write_rate = round((write_count + rename_count) / 5.0, 2)
    # Benign bursts rarely persist for many windows
    consecutive_alert_count = random.choice([0, 0, 1])
    total_bytes = write_count * random.randint(32768, 524288)

    return {
        "write_count": write_count,
        "rename_count": rename_count,
        "delete_count": delete_count,
        "unique_file_count": unique_file_count,
        "suspicious_rename_count": suspicious_rename_count,
        "write_rate_ops_per_sec": write_rate,
        "consecutive_alert_count": consecutive_alert_count,
        "total_bytes_written": total_bytes,
        "label": 0
    }

def generate_ransomware_window(stage: int = 1):
    """Simulates early burst (Stage 1) or sustained encryption (Stage 2)."""
    if stage == 1:
        # Initial rapid burst
        write_count = random.randint(5, 18)
        rename_count = random.randint(3, 10)
        delete_count = random.randint(0, 2)
        unique_file_count = random.randint(4, 15)
        # Some ransomware starts renames immediately, others just encrypt
        suspicious_rename_count = random.randint(1, 4)
        consecutive_alert_count = 1
    else:
        # Stage 2 sustained persistence & traversal
        write_count = random.randint(10, 30)
        rename_count = random.randint(5, 20)
        delete_count = random.randint(0, 5)
        unique_file_count = random.randint(8, 25)
        suspicious_rename_count = random.randint(3, 15)
        consecutive_alert_count = random.randint(2, 6)

    total_ops = write_count + rename_count + delete_count
    write_rate = round(total_ops / 5.0, 2)
    total_bytes = write_count * random.randint(65536, 1048576)

    return {
        "write_count": write_count,
        "rename_count": rename_count,
        "delete_count": delete_count,
        "unique_file_count": unique_file_count,
        "suspicious_rename_count": suspicious_rename_count,
        "write_rate_ops_per_sec": write_rate,
        "consecutive_alert_count": consecutive_alert_count,
        "total_bytes_written": total_bytes,
        "label": 1
    }

def create_synthetic_dataset(total_samples: int = 2500, output_dir: str = "data"):
    os.makedirs(output_dir, exist_ok=True)
    records = []

    # 55% normal casual user activity
    normal_count = int(total_samples * 0.55)
    for _ in range(normal_count):
        records.append(generate_normal_window())

    # 15% benign bursts (stress tests false positive rate)
    burst_count = int(total_samples * 0.15)
    for _ in range(burst_count):
        records.append(generate_benign_burst_window())

    # 30% ransomware attacks (half stage 1, half stage 2)
    malicious_count = total_samples - (normal_count + burst_count)
    for i in range(malicious_count):
        stage = 1 if i % 2 == 0 else 2
        records.append(generate_ransomware_window(stage))

    random.shuffle(records)
    df = pd.DataFrame(records)

    # 80/20 train/test split
    train_size = int(len(df) * 0.8)
    train_df = df.iloc[:train_size]
    test_df = df.iloc[train_size:]

    train_path = os.path.join(output_dir, "train_windows.csv")
    test_path = os.path.join(output_dir, "test_windows.csv")
    full_path = os.path.join(output_dir, "full_dataset.csv")

    df.to_csv(full_path, index=False)
    train_df.to_csv(train_path, index=False)
    test_df.to_csv(test_path, index=False)

    print(f"Generated {len(df)} total samples.")
    print(f"Train samples: {len(train_df)} -> {train_path}")
    print(f"Test samples:  {len(test_df)} -> {test_path}")
    print(f"Normal/Burst (0): {(df['label'] == 0).sum()} | Ransomware (1): {(df['label'] == 1).sum()}")

if __name__ == "__main__":
    create_synthetic_dataset()
