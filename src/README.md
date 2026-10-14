# SentinelPulse Android Client Source

This directory contains the native Android application source code (Kotlin + Jetpack Compose):

- `main/java/com/example/zerotrace/analyzer/`: Real-time sliding window behavior analyzer.
- `main/java/com/example/zerotrace/data/`: Data models and telemetry event schemas.
- `main/java/com/example/zerotrace/simulator/`: Safe sandbox simulation engine (Normal, Burst Storm, and Ransomware workloads).
- `main/java/com/example/zerotrace/ui/`: Jetpack Compose UI dashboards and risk warning banners.
- `test/java/com/example/zerotrace/`: JUnit test suite verifying sliding window heuristics.
