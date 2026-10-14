package com.example.zerotrace.analyzer

import com.example.zerotrace.data.model.AlertLevel
import com.example.zerotrace.data.model.FileOpEvent
import com.example.zerotrace.data.model.FileOpType
import com.example.zerotrace.data.model.SlidingWindowMetrics
import com.example.zerotrace.data.model.TwoStageAnalysisResult
import java.util.concurrent.ConcurrentLinkedQueue

class TwoStageBehaviorAnalyzer(
    val windowDurationMs: Long = 5000L,
    val stage1WriteRateThreshold: Double = 1.6, // ops/sec
    val stage1RenamesThreshold: Int = 3,
    val stage1SuspiciousExtThreshold: Int = 1,
    val stage2ConsecutiveWindowsThreshold: Int = 2,
    val stage2UniqueFilesThreshold: Int = 5
) {
    private val eventHistory = ConcurrentLinkedQueue<FileOpEvent>()
    private var consecutiveBurstWindows = 0

    fun addEvent(event: FileOpEvent, nowTimestamp: Long = event.timestamp): TwoStageAnalysisResult {
        eventHistory.add(event)
        return analyze(nowTimestamp)
    }

    fun addEvents(events: List<FileOpEvent>, nowTimestamp: Long = events.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()): TwoStageAnalysisResult {
        eventHistory.addAll(events)
        return analyze(nowTimestamp)
    }

    fun reset() {
        eventHistory.clear()
        consecutiveBurstWindows = 0
    }

    fun analyze(nowTimestamp: Long = System.currentTimeMillis()): TwoStageAnalysisResult {
        val windowStartTime = nowTimestamp - windowDurationMs

        // Clean expired events outside sliding window
        eventHistory.removeIf { it.timestamp < windowStartTime }

        val windowEvents = eventHistory.filter { it.timestamp in windowStartTime..nowTimestamp }

        // 1. Calculate Sliding Window Metrics
        val windowSeconds = windowDurationMs / 1000.0
        val totalOps = windowEvents.size
        val creates = windowEvents.count { it.operationType == FileOpType.CREATE }
        val writes = windowEvents.count { it.operationType == FileOpType.WRITE }
        val renames = windowEvents.count { it.operationType == FileOpType.RENAME }
        val deletes = windowEvents.count { it.operationType == FileOpType.DELETE }

        val uniqueFilesCount = windowEvents.map { extractFileBaseName(it.filePath) }.distinct().size
        val writeRate = if (windowSeconds > 0) totalOps / windowSeconds else 0.0
        val suspiciousExts = windowEvents.count { it.isSuspiciousExtension }
        val totalBytes = windowEvents.sumOf { it.bytesAffected }

        val metrics = SlidingWindowMetrics(
            windowDurationMs = windowDurationMs,
            totalOperations = totalOps,
            fileCreates = creates,
            fileWrites = writes,
            fileRenames = renames,
            fileDeletes = deletes,
            uniqueFiles = uniqueFilesCount,
            writeRate = writeRate,
            suspiciousRenamesCount = suspiciousExts,
            totalBytesWritten = totalBytes
        )

        // 2. Stage 1 Evaluation (Burst Detection)
        val stage1Reasons = mutableListOf<String>()
        var stage1Triggered = false

        if (writeRate >= stage1WriteRateThreshold) {
            stage1Triggered = true
            stage1Reasons.add(
                "Stage 1 High Velocity Burst: Operation rate of %.2f ops/sec (%d operations in %.1fs window) exceeded threshold (%.2f ops/sec)."
                    .format(writeRate, totalOps, windowSeconds, stage1WriteRateThreshold)
            )
        }

        if (renames >= stage1RenamesThreshold) {
            stage1Triggered = true
            stage1Reasons.add(
                "Stage 1 Rename Burst: %d file renames detected in %.1fs window (threshold: %d)."
                    .format(renames, windowSeconds, stage1RenamesThreshold)
            )
        }

        if (suspiciousExts >= stage1SuspiciousExtThreshold) {
            stage1Triggered = true
            stage1Reasons.add(
                "Stage 1 Suspicious Extension Detection: %d files transformed to suspicious/encrypted extensions in window."
                    .format(suspiciousExts)
            )
        }

        // Update consecutive burst count
        if (stage1Triggered) {
            consecutiveBurstWindows++
        } else {
            consecutiveBurstWindows = 0
        }

        // 3. Stage 2 Evaluation (Persistence & Ransomware Anomaly Verification)
        val stage2Reasons = mutableListOf<String>()
        var stage2Triggered = false

        if (stage1Triggered) {
            if (consecutiveBurstWindows >= stage2ConsecutiveWindowsThreshold) {
                stage2Triggered = true
                stage2Reasons.add(
                    "Stage 2 Persistence Verified: High-activity burst persisted for %d consecutive time windows."
                        .format(consecutiveBurstWindows)
                )
            }

            if (suspiciousExts >= 1) {
                stage2Triggered = true
                stage2Reasons.add(
                    "Stage 2 High Severity Threat Pattern: Operation burst combined with %d suspicious extension rename(s) (.locked / .crypto / .enc)."
                        .format(suspiciousExts)
                )
            }

            if (uniqueFilesCount >= stage2UniqueFilesThreshold && writeRate >= stage1WriteRateThreshold) {
                stage2Triggered = true
                stage2Reasons.add(
                    "Stage 2 Multi-File Encryption Pattern: Rapid modifications detected across %d distinct files simultaneously."
                        .format(uniqueFilesCount)
                )
            }
        }

        // Determine Alert Level & Compile Final Explainable Reasons
        val alertLevel: AlertLevel
        val allReasons = mutableListOf<String>()

        when {
            stage2Triggered -> {
                alertLevel = AlertLevel.HIGH_RISK_WARNING
                allReasons.addAll(stage2Reasons)
                allReasons.addAll(stage1Reasons)
            }
            stage1Triggered -> {
                alertLevel = AlertLevel.ELEVATED_ACTIVITY
                allReasons.addAll(stage1Reasons)
                allReasons.add("Monitoring activity window: Burst detected, waiting for Stage 2 persistence verification.")
            }
            else -> {
                alertLevel = AlertLevel.NORMAL
                if (totalOps == 0) {
                    allReasons.add("Normal Baseline: No file operations recorded in the sliding window.")
                } else {
                    allReasons.add(
                        "Normal Activity: %d operations (%.2f ops/sec across %d unique files) within normal safe parameters."
                            .format(totalOps, writeRate, uniqueFilesCount)
                    )
                }
            }
        }

        return TwoStageAnalysisResult(
            stage1BurstTriggered = stage1Triggered,
            stage2PersistenceTriggered = stage2Triggered,
            alertLevel = alertLevel,
            explainableReasons = allReasons,
            metrics = metrics,
            consecutiveBurstCount = consecutiveBurstWindows
        )
    }

    private fun extractFileBaseName(path: String): String {
        val cleanPath = if (path.contains("->")) {
            path.substringAfter("->").trim()
        } else {
            path
        }
        return cleanPath.substringAfterLast("/").substringAfterLast("\\")
    }
}
