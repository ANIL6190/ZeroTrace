package com.example.zerotrace

import com.example.zerotrace.analyzer.TwoStageBehaviorAnalyzer
import com.example.zerotrace.data.model.AlertLevel
import com.example.zerotrace.data.model.FileOpEvent
import com.example.zerotrace.data.model.FileOpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TwoStageBehaviorAnalyzerTest {

    private lateinit var analyzer: TwoStageBehaviorAnalyzer

    @Before
    fun setUp() {
        analyzer = TwoStageBehaviorAnalyzer(
            windowDurationMs = 5000L,
            stage1WriteRateThreshold = 1.6, // 8 ops / 5s
            stage1RenamesThreshold = 3,
            stage1SuspiciousExtThreshold = 1,
            stage2ConsecutiveWindowsThreshold = 2,
            stage2UniqueFilesThreshold = 5
        )
    }

    @Test
    fun `test Normal workload produces NORMAL alert level and correct metrics`() {
        val now = System.currentTimeMillis()
        val events = listOf(
            FileOpEvent(timestamp = now - 3000, filePath = "doc1.txt", operationType = FileOpType.CREATE, extensionAfter = "txt"),
            FileOpEvent(timestamp = now - 1000, filePath = "doc1.txt", operationType = FileOpType.WRITE, bytesAffected = 256, extensionAfter = "txt")
        )

        val result = analyzer.addEvents(events)

        assertEquals(AlertLevel.NORMAL, result.alertLevel)
        assertFalse(result.stage1BurstTriggered)
        assertFalse(result.stage2PersistenceTriggered)
        assertEquals(2, result.metrics.totalOperations)
        assertEquals(1, result.metrics.fileCreates)
        assertEquals(1, result.metrics.fileWrites)
        assertEquals(0, result.metrics.fileRenames)
        assertEquals(1, result.metrics.uniqueFiles)
        assertEquals(0.4, result.metrics.writeRate, 0.01)
        assertTrue(result.explainableReasons.any { it.contains("Normal Activity") })
    }

    @Test
    fun `test Stage 1 Burst Detection triggers on high write velocity`() {
        val now = System.currentTimeMillis()
        // 10 writes across 2 files -> high write rate, unique files = 2 (< threshold 5)
        val events = (1..10).map { i ->
            FileOpEvent(
                timestamp = now - (i * 200),
                filePath = "file_${i % 2}.txt",
                operationType = FileOpType.WRITE,
                bytesAffected = 100,
                extensionAfter = "txt"
            )
        }

        val result = analyzer.addEvents(events)

        assertTrue(result.stage1BurstTriggered)
        assertFalse(result.stage2PersistenceTriggered)
        assertEquals(AlertLevel.ELEVATED_ACTIVITY, result.alertLevel)
        assertEquals(10, result.metrics.totalOperations)
        assertEquals(2, result.metrics.uniqueFiles)
        assertEquals(2.0, result.metrics.writeRate, 0.01)
        assertTrue(result.explainableReasons.any { it.contains("Stage 1 High Velocity Burst") })
    }

    @Test
    fun `test Stage 1 Burst Detection triggers on rename threshold`() {
        val now = System.currentTimeMillis()
        val events = (1..4).map { i ->
            FileOpEvent(
                timestamp = now - (i * 300),
                filePath = "file_$i.txt -> file_$i.bak",
                operationType = FileOpType.RENAME,
                extensionAfter = "bak"
            )
        }

        val result = analyzer.addEvents(events)

        assertTrue(result.stage1BurstTriggered)
        assertEquals(4, result.metrics.fileRenames)
        assertTrue(result.explainableReasons.any { it.contains("Stage 1 Rename Burst") })
    }

    @Test
    fun `test Stage 2 triggers on persistent burst across consecutive windows`() {
        val t1 = 1000000L
        // Window 1: 10 writes to 2 unique files -> Stage 1 burst, unique files < 5
        val eventsWindow1 = (1..10).map { i ->
            FileOpEvent(
                timestamp = t1 - (i * 100),
                filePath = "f_${i % 2}.txt",
                operationType = FileOpType.WRITE,
                extensionAfter = "txt"
            )
        }

        // Window 1: Stage 1 triggered
        val res1 = analyzer.addEvents(eventsWindow1)
        assertTrue(res1.stage1BurstTriggered)
        assertFalse(res1.stage2PersistenceTriggered)
        assertEquals(1, res1.consecutiveBurstCount)
        assertEquals(AlertLevel.ELEVATED_ACTIVITY, res1.alertLevel)

        // Window 2: 6 seconds later, new burst
        val t2 = t1 + 6000L
        val eventsWindow2 = (1..10).map { i ->
            FileOpEvent(
                timestamp = t2 - (i * 100),
                filePath = "f2_${i % 2}.txt",
                operationType = FileOpType.WRITE,
                extensionAfter = "txt"
            )
        }

        // Window 2: Consecutive burst count reaches 2 => Stage 2 triggered!
        val res2 = analyzer.addEvents(eventsWindow2)
        assertTrue(res2.stage1BurstTriggered)
        assertTrue(res2.stage2PersistenceTriggered)
        assertEquals(2, res2.consecutiveBurstCount)
        assertEquals(AlertLevel.HIGH_RISK_WARNING, res2.alertLevel)
        assertTrue(res2.explainableReasons.any { it.contains("Stage 2 Persistence Verified") })
    }

    @Test
    fun `test Stage 2 direct escalation on multi-file encryption pattern`() {
        val now = System.currentTimeMillis()
        // 10 events across 8 distinct files
        val events = (1..10).map { i ->
            FileOpEvent(
                timestamp = now - (i * 200),
                filePath = "unique_file_$i.txt",
                operationType = FileOpType.WRITE,
                bytesAffected = 128,
                extensionAfter = "txt"
            )
        }

        val result = analyzer.addEvents(events)

        assertTrue(result.stage1BurstTriggered)
        assertTrue(result.stage2PersistenceTriggered)
        assertEquals(AlertLevel.HIGH_RISK_WARNING, result.alertLevel)
        assertTrue(result.metrics.uniqueFiles >= 5)
        assertTrue(result.explainableReasons.any { it.contains("Stage 2 Multi-File Encryption Pattern") })
    }

    @Test
    fun `test Stage 2 direct escalation when burst includes suspicious ransomware extensions`() {
        val now = System.currentTimeMillis()
        val events = (1..9).map { i ->
            FileOpEvent(
                timestamp = now - (i * 200),
                filePath = if (i <= 2) "doc_$i.txt -> doc_$i.txt.locked" else "doc_$i.txt",
                operationType = if (i <= 2) FileOpType.RENAME else FileOpType.WRITE,
                extensionAfter = if (i <= 2) "locked" else "txt",
                isSuspiciousExtension = i <= 2
            )
        }

        val result = analyzer.addEvents(events)

        assertTrue(result.stage1BurstTriggered)
        assertTrue(result.stage2PersistenceTriggered)
        assertEquals(AlertLevel.HIGH_RISK_WARNING, result.alertLevel)
        assertEquals(2, result.metrics.suspiciousRenamesCount)
        assertTrue(result.explainableReasons.any { it.contains("Stage 2 High Severity Threat Pattern") })
    }

    @Test
    fun `test Expired events are purged outside sliding window`() {
        val now = System.currentTimeMillis()
        val oldEvents = listOf(
            FileOpEvent(timestamp = now - 10000, filePath = "old.txt", operationType = FileOpType.WRITE)
        )
        val newEvents = listOf(
            FileOpEvent(timestamp = now - 1000, filePath = "new.txt", operationType = FileOpType.WRITE)
        )

        analyzer.addEvents(oldEvents)
        val result = analyzer.addEvents(newEvents)

        assertEquals(1, result.metrics.totalOperations)
        assertEquals(1, result.metrics.uniqueFiles)
    }
}
