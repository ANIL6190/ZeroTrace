package com.example.zerotrace

import com.example.zerotrace.data.model.BaselineDeltas
import com.example.zerotrace.data.model.SlidingWindowMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineDeltasTest {

    @Test
    fun `test BaselineDeltas calculation within safe parameters`() {
        val metrics = SlidingWindowMetrics(
            writeRate = 1.0,
            fileRenames = 1,
            uniqueFiles = 2,
            suspiciousRenamesCount = 0
        )

        val deltas = BaselineDeltas.calculate(metrics)

        assertFalse(deltas.isWriteRateExceeded)
        assertFalse(deltas.isRenamesExceeded)
        assertFalse(deltas.isUniqueFilesExceeded)
        assertFalse(deltas.isSuspiciousExtExceeded)
        assertEquals(-0.6, deltas.writeRateDelta, 0.01)
        assertEquals(-2, deltas.renamesDelta)
        assertEquals(-3, deltas.uniqueFilesDelta)
        assertEquals(0, deltas.suspiciousExtDelta)
    }

    @Test
    fun `test BaselineDeltas calculation when thresholds are exceeded`() {
        val metrics = SlidingWindowMetrics(
            writeRate = 4.0,
            fileRenames = 5,
            uniqueFiles = 8,
            suspiciousRenamesCount = 3
        )

        val deltas = BaselineDeltas.calculate(metrics)

        assertTrue(deltas.isWriteRateExceeded)
        assertTrue(deltas.isRenamesExceeded)
        assertTrue(deltas.isUniqueFilesExceeded)
        assertTrue(deltas.isSuspiciousExtExceeded)
        assertEquals(2.4, deltas.writeRateDelta, 0.01)
        assertEquals(2, deltas.renamesDelta)
        assertEquals(3, deltas.uniqueFilesDelta)
        assertEquals(3, deltas.suspiciousExtDelta)
    }
}
