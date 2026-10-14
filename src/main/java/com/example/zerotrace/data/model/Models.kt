package com.example.zerotrace.data.model

import java.util.UUID

enum class FileOpType {
    CREATE,
    WRITE,
    RENAME,
    DELETE
}

data class FileOpEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String,
    val operationType: FileOpType,
    val bytesAffected: Long = 0L,
    val extensionAfter: String = "",
    val isSuspiciousExtension: Boolean = false
)

enum class WorkloadMode {
    IDLE,
    NORMAL_USER_WORKLOAD,
    RANSOMWARE_SIMULATION_WORKLOAD,
    BURST_STORM_WORKLOAD
}

enum class AlertLevel {
    NORMAL,
    ELEVATED_ACTIVITY, // Stage 1 burst detected
    HIGH_RISK_WARNING   // Stage 2 persistence or ransomware-like threat verified
}

data class SlidingWindowMetrics(
    val windowDurationMs: Long = 5000L,
    val totalOperations: Int = 0,
    val fileCreates: Int = 0,
    val fileWrites: Int = 0,
    val fileRenames: Int = 0,
    val fileDeletes: Int = 0,
    val uniqueFiles: Int = 0,
    val writeRate: Double = 0.0, // operations per second
    val suspiciousRenamesCount: Int = 0,
    val totalBytesWritten: Long = 0L
)

data class TwoStageAnalysisResult(
    val stage1BurstTriggered: Boolean = false,
    val stage2PersistenceTriggered: Boolean = false,
    val alertLevel: AlertLevel = AlertLevel.NORMAL,
    val explainableReasons: List<String> = emptyList(),
    val metrics: SlidingWindowMetrics = SlidingWindowMetrics(),
    val consecutiveBurstCount: Int = 0
)

data class SandboxFileInfo(
    val fileName: String,
    val relativePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isEncryptedExtension: Boolean
)

data class BaselineDeltas(
    val writeRateDelta: Double = 0.0,
    val renamesDelta: Int = 0,
    val uniqueFilesDelta: Int = 0,
    val suspiciousExtDelta: Int = 0,
    val isWriteRateExceeded: Boolean = false,
    val isRenamesExceeded: Boolean = false,
    val isUniqueFilesExceeded: Boolean = false,
    val isSuspiciousExtExceeded: Boolean = false
) {
    companion object {
        const val BASELINE_WRITE_RATE = 1.6
        const val BASELINE_RENAMES = 3
        const val BASELINE_UNIQUE_FILES = 5
        const val BASELINE_SUSPICIOUS_EXT = 0

        fun calculate(metrics: SlidingWindowMetrics): BaselineDeltas {
            val wrDelta = metrics.writeRate - BASELINE_WRITE_RATE
            val renDelta = metrics.fileRenames - BASELINE_RENAMES
            val uqDelta = metrics.uniqueFiles - BASELINE_UNIQUE_FILES
            val susDelta = metrics.suspiciousRenamesCount - BASELINE_SUSPICIOUS_EXT

            return BaselineDeltas(
                writeRateDelta = wrDelta,
                renamesDelta = renDelta,
                uniqueFilesDelta = uqDelta,
                suspiciousExtDelta = susDelta,
                isWriteRateExceeded = wrDelta > 0,
                isRenamesExceeded = renDelta >= 0 && metrics.fileRenames >= BASELINE_RENAMES,
                isUniqueFilesExceeded = uqDelta >= 0 && metrics.uniqueFiles >= BASELINE_UNIQUE_FILES,
                isSuspiciousExtExceeded = susDelta > 0
            )
        }
    }
}

data class AlertHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val alertLevel: AlertLevel,
    val consecutiveBursts: Int,
    val summary: String,
    val writeRate: Double,
    val suspiciousExtCount: Int
)

