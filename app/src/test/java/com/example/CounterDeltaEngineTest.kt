package com.example

import com.example.data.CounterDeltaEngine
import com.example.data.RouterUsageSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterDeltaEngineTest {

    @Test
    fun normalIncrementAddsDeltaAndComputesSpeed() {
        val previous = RouterUsageSnapshot(
            totalRxBytes = 1_000_000L,
            totalTxBytes = 200_000L,
            todayRxBytes = 500_000L,
            todayTxBytes = 100_000L,
            todayDate = "2026-11-19",
            lastRxBytes = 1_000_000L,
            lastTxBytes = 200_000L,
            rebootCount = 0
        )

        val update = CounterDeltaEngine.applyPollSample(
            previous = previous,
            currentRxBytes = 1_600_000L,
            currentTxBytes = 350_000L,
            currentDate = "2026-11-19",
            elapsedSeconds = 3.0
        )

        assertEquals(1_600_000L, update.totalRxBytes)
        assertEquals(350_000L, update.totalTxBytes)
        assertEquals(1_100_000L, update.todayRxBytes)
        assertEquals(250_000L, update.todayTxBytes)
        assertEquals(0, update.rebootCount)
        assertFalse(update.rebootDetected)
        assertEquals(200_000.0, update.downloadSpeedBps, 0.01)
        assertEquals(50_000.0, update.uploadSpeedBps, 0.01)
    }

    @Test
    fun uint32WrapAroundDoesNotIncrementRebootCount() {
        val lastRx = 4_200_000_000L // > 3.7 GB
        val currentRx = 100_000_000L // < 0.3 GB
        val expectedDelta = (4_294_967_296L - lastRx) + currentRx

        val result = CounterDeltaEngine.computeCounterDelta(lastRx, currentRx)
        assertEquals(expectedDelta, result.deltaBytes)
        assertTrue(result.isWrapAround)
        assertFalse(result.isReboot)

        val previous = RouterUsageSnapshot(
            totalRxBytes = 5_000_000_000L,
            todayRxBytes = 1_000_000_000L,
            todayDate = "2026-11-19",
            lastRxBytes = lastRx,
            lastTxBytes = 500_000L,
            rebootCount = 2
        )
        val update = CounterDeltaEngine.applyPollSample(
            previous = previous,
            currentRxBytes = currentRx,
            currentTxBytes = 510_000L,
            currentDate = "2026-11-19",
            elapsedSeconds = 3.0
        )
        assertEquals(5_000_000_000L + expectedDelta, update.totalRxBytes)
        assertEquals(2, update.rebootCount)
        assertFalse(update.rebootDetected)
    }

    @Test
    fun powerOutagePreservesCumulativeTotalAndIncrementsRebootOnce() {
        val previous = RouterUsageSnapshot(
            totalRxBytes = 10_000_000_000L,
            totalTxBytes = 2_000_000_000L,
            todayRxBytes = 1_500_000_000L,
            todayTxBytes = 300_000_000L,
            todayDate = "2026-11-19",
            lastRxBytes = 900_000_000L,
            lastTxBytes = 150_000_000L,
            rebootCount = 3
        )

        // Both rx and tx dropped after a router power outage
        val update = CounterDeltaEngine.applyPollSample(
            previous = previous,
            currentRxBytes = 25_000_000L,
            currentTxBytes = 5_000_000L,
            currentDate = "2026-11-19",
            elapsedSeconds = 3.0
        )

        assertEquals(10_025_000_000L, update.totalRxBytes)
        assertEquals(2_005_000_000L, update.totalTxBytes)
        assertEquals(1_525_000_000L, update.todayRxBytes)
        assertEquals(305_000_000L, update.todayTxBytes)
        assertEquals(4, update.rebootCount) // Incremented by 1, not 2
        assertTrue(update.rebootDetected)
    }

    @Test
    fun calendarDateRolloverResetsTodayOnlyAndKeepsTotalsIntact() {
        val previous = RouterUsageSnapshot(
            totalRxBytes = 20_000_000_000L,
            totalTxBytes = 4_000_000_000L,
            todayRxBytes = 3_000_000_000L,
            todayTxBytes = 500_000_000L,
            todayDate = "2026-11-18",
            lastRxBytes = 500_000_000L,
            lastTxBytes = 100_000_000L,
            rebootCount = 1
        )

        val update = CounterDeltaEngine.applyPollSample(
            previous = previous,
            currentRxBytes = 510_000_000L,
            currentTxBytes = 102_000_000L,
            currentDate = "2026-11-19",
            elapsedSeconds = 3.0
        )

        assertEquals("2026-11-19", update.todayDate)
        assertEquals(20_010_000_000L, update.totalRxBytes)
        assertEquals(4_002_000_000L, update.totalTxBytes)
        assertEquals(10_000_000L, update.todayRxBytes)
        assertEquals(2_000_000L, update.todayTxBytes)
        assertEquals(1, update.rebootCount)
    }
}
