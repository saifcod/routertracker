package com.example.data

/**
 * Pure domain engine for calculating UPnP router byte counter deltas,
 * 32-bit (4 GiB) wrap-around handling, power-outage/reboot detection,
 * calendar day rollover, and real-time throughput speeds.
 */
object CounterDeltaEngine {

    /** 2^32 bytes (4 GiB) — standard 32-bit UPnP WANCommonInterfaceConfig counter rollover. */
    const val UINT32_MODULO: Long = 4_294_967_296L

    /** Threshold above which a drop to a tiny counter is treated as 32-bit wrap-around (~3.7 GB). */
    const val WRAP_LAST_MIN_BYTES: Long = 3_700_000_000L

    /** Threshold below which a drop from >3.7 GB is treated as 32-bit wrap-around (~0.3 GiB). */
    const val WRAP_CURRENT_MAX_BYTES: Long = 322_122_547L

    data class CounterDeltaResult(
        val deltaBytes: Long,
        val isReboot: Boolean,
        val isWrapAround: Boolean
    )

    data class SampleUpdate(
        val totalRxBytes: Long,
        val totalTxBytes: Long,
        val todayRxBytes: Long,
        val todayTxBytes: Long,
        val todayDate: String,
        val lastRxBytes: Long,
        val lastTxBytes: Long,
        val rebootCount: Int,
        val downloadSpeedBps: Double,
        val uploadSpeedBps: Double,
        val rebootDetected: Boolean
    )

    /**
     * Computes byte delta for a single directional counter (rx or tx).
     *
     * Rules:
     * - First observation (last < 0): baseline only (delta = 0, no reboot).
     * - current >= last: normal operation, delta = current - last.
     * - last > 3.7 GB and current < 0.3 GB: 32-bit wrap-around, delta = (4294967296 - last) + current,
     *   does NOT count as a router reboot.
     * - Otherwise (current < last): router reboot / power outage detected, delta = current, reboot = true.
     */
    fun computeCounterDelta(lastBytes: Long, currentBytes: Long): CounterDeltaResult {
        val safeCurrent = currentBytes.coerceAtLeast(0L)
        if (lastBytes < 0L) {
            return CounterDeltaResult(
                deltaBytes = 0L,
                isReboot = false,
                isWrapAround = false
            )
        }
        return when {
            safeCurrent >= lastBytes -> CounterDeltaResult(
                deltaBytes = safeCurrent - lastBytes,
                isReboot = false,
                isWrapAround = false
            )

            lastBytes > WRAP_LAST_MIN_BYTES && safeCurrent < WRAP_CURRENT_MAX_BYTES -> {
                val wrappedDelta = ((UINT32_MODULO - lastBytes).coerceAtLeast(0L)) + safeCurrent
                CounterDeltaResult(
                    deltaBytes = wrappedDelta,
                    isReboot = false,
                    isWrapAround = true
                )
            }

            else -> CounterDeltaResult(
                deltaBytes = safeCurrent,
                isReboot = true,
                isWrapAround = false
            )
        }
    }

    /**
     * Applies a new UPnP poll sample to the current persisted state.
     * Automatically rolls over today's counters when [currentDate] changes while preserving totals.
     * Increments rebootCount by at most 1 per poll cycle even if both rx and tx reset together.
     */
    fun applyPollSample(
        previous: RouterUsageSnapshot,
        currentRxBytes: Long,
        currentTxBytes: Long,
        currentDate: String,
        elapsedSeconds: Double
    ): SampleUpdate {
        val safeCurrentRx = currentRxBytes.coerceAtLeast(0L)
        val safeCurrentTx = currentTxBytes.coerceAtLeast(0L)

        val rxResult = computeCounterDelta(previous.lastRxBytes, safeCurrentRx)
        val txResult = computeCounterDelta(previous.lastTxBytes, safeCurrentTx)

        val isNewDay = previous.todayDate.isNotEmpty() && previous.todayDate != currentDate
        val baseTodayRx = if (isNewDay) 0L else previous.todayRxBytes.coerceAtLeast(0L)
        val baseTodayTx = if (isNewDay) 0L else previous.todayTxBytes.coerceAtLeast(0L)

        val rebootDetected = rxResult.isReboot || txResult.isReboot
        val newRebootCount = previous.rebootCount + if (rebootDetected) 1 else 0

        val validElapsed = elapsedSeconds > 0.1 && elapsedSeconds <= 120.0
        val rxSpeed = if (validElapsed && !rxResult.isReboot) {
            rxResult.deltaBytes.toDouble() / elapsedSeconds
        } else if (validElapsed && rxResult.isReboot && previous.lastRxBytes >= 0L) {
            // During a reboot poll, delta is the bytes accumulated since router booted; avoid fake spike
            0.0
        } else {
            0.0
        }
        val txSpeed = if (validElapsed && !txResult.isReboot) {
            txResult.deltaBytes.toDouble() / elapsedSeconds
        } else {
            0.0
        }

        return SampleUpdate(
            totalRxBytes = previous.totalRxBytes.coerceAtLeast(0L) + rxResult.deltaBytes,
            totalTxBytes = previous.totalTxBytes.coerceAtLeast(0L) + txResult.deltaBytes,
            todayRxBytes = baseTodayRx + rxResult.deltaBytes,
            todayTxBytes = baseTodayTx + txResult.deltaBytes,
            todayDate = currentDate,
            lastRxBytes = safeCurrentRx,
            lastTxBytes = safeCurrentTx,
            rebootCount = newRebootCount,
            downloadSpeedBps = rxSpeed.coerceAtLeast(0.0),
            uploadSpeedBps = txSpeed.coerceAtLeast(0.0),
            rebootDetected = rebootDetected
        )
    }
}
