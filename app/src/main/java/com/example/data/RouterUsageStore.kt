package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe persistent SharedPreferences repository that stores all router usage metrics
 * on every update and broadcasts live snapshots via [StateFlow].
 */
class RouterUsageStore private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val lock = Any()
    private val _state = MutableStateFlow(loadInitialSnapshot())
    val state: StateFlow<RouterUsageSnapshot> = _state.asStateFlow()

    private fun loadInitialSnapshot(): RouterUsageSnapshot {
        val now = System.currentTimeMillis()
        val currentDate = Formatters.todayDateKey(now)
        val savedStartedAt = prefs.getLong(KEY_STARTED_AT, 0L)
        val startedAt = if (savedStartedAt > 0L) savedStartedAt else now

        val savedDate = prefs.getString(KEY_TODAY_DATE, "") ?: ""
        val isDateRollover = savedDate.isNotEmpty() && savedDate != currentDate

        val totalRx = prefs.getLong(KEY_TOTAL_RX, 0L).coerceAtLeast(0L)
        val totalTx = prefs.getLong(KEY_TOTAL_TX, 0L).coerceAtLeast(0L)
        val todayRx = if (isDateRollover) 0L else prefs.getLong(KEY_TODAY_RX, 0L).coerceAtLeast(0L)
        val todayTx = if (isDateRollover) 0L else prefs.getLong(KEY_TODAY_TX, 0L).coerceAtLeast(0L)
        val lastRx = prefs.getLong(KEY_LAST_RX, -1L)
        val lastTx = prefs.getLong(KEY_LAST_TX, -1L)
        val rebootCount = prefs.getInt(KEY_REBOOT_COUNT, 0).coerceAtLeast(0)

        if (savedStartedAt <= 0L || isDateRollover || savedDate.isEmpty()) {
            prefs.edit()
                .putLong(KEY_TOTAL_RX, totalRx)
                .putLong(KEY_TOTAL_TX, totalTx)
                .putLong(KEY_TODAY_RX, todayRx)
                .putLong(KEY_TODAY_TX, todayTx)
                .putString(KEY_TODAY_DATE, currentDate)
                .putLong(KEY_LAST_RX, lastRx)
                .putLong(KEY_LAST_TX, lastTx)
                .putInt(KEY_REBOOT_COUNT, rebootCount)
                .putLong(KEY_STARTED_AT, startedAt)
                .apply()
        }

        return RouterUsageSnapshot(
            totalRxBytes = totalRx,
            totalTxBytes = totalTx,
            todayRxBytes = todayRx,
            todayTxBytes = todayTx,
            todayDate = currentDate,
            lastRxBytes = lastRx,
            lastTxBytes = lastTx,
            rebootCount = rebootCount,
            startedAtMillis = startedAt,
            connectionStatus = ConnectionStatus.DISCOVERING
        )
    }

    fun recordPollSample(
        currentRxBytes: Long,
        currentTxBytes: Long,
        elapsedSeconds: Double,
        endpoint: RouterEndpoint
    ): RouterUsageSnapshot = synchronized(lock) {
        val now = System.currentTimeMillis()
        val currentDate = Formatters.todayDateKey(now)
        val previous = _state.value

        val updated = CounterDeltaEngine.applyPollSample(
            previous = previous,
            currentRxBytes = currentRxBytes,
            currentTxBytes = currentTxBytes,
            currentDate = currentDate,
            elapsedSeconds = elapsedSeconds
        )

        persistFields(
            totalRx = updated.totalRxBytes,
            totalTx = updated.totalTxBytes,
            todayRx = updated.todayRxBytes,
            todayTx = updated.todayTxBytes,
            todayDate = updated.todayDate,
            lastRx = updated.lastRxBytes,
            lastTx = updated.lastTxBytes,
            rebootCount = updated.rebootCount,
            startedAt = previous.startedAtMillis
        )

        val nextHistoryRx = (previous.speedHistoryRx + updated.downloadSpeedBps)
            .takeLast(MAX_SPEED_HISTORY_POINTS)
        val nextHistoryTx = (previous.speedHistoryTx + updated.uploadSpeedBps)
            .takeLast(MAX_SPEED_HISTORY_POINTS)

        val nextSnapshot = previous.copy(
            totalRxBytes = updated.totalRxBytes,
            totalTxBytes = updated.totalTxBytes,
            todayRxBytes = updated.todayRxBytes,
            todayTxBytes = updated.todayTxBytes,
            todayDate = updated.todayDate,
            lastRxBytes = updated.lastRxBytes,
            lastTxBytes = updated.lastTxBytes,
            rebootCount = updated.rebootCount,
            downloadSpeedBps = updated.downloadSpeedBps,
            uploadSpeedBps = updated.uploadSpeedBps,
            connectionStatus = ConnectionStatus.CONNECTED,
            routerName = endpoint.friendlyName,
            routerHost = endpoint.host,
            lastUpdatedMillis = now,
            speedHistoryRx = nextHistoryRx,
            speedHistoryTx = nextHistoryTx
        )
        _state.value = nextSnapshot
        nextSnapshot
    }

    fun updateConnectionStatus(status: ConnectionStatus): RouterUsageSnapshot = synchronized(lock) {
        val now = System.currentTimeMillis()
        val currentDate = Formatters.todayDateKey(now)
        val previous = _state.value

        val isDateRollover = previous.todayDate.isNotEmpty() && previous.todayDate != currentDate
        val todayRx = if (isDateRollover) 0L else previous.todayRxBytes
        val todayTx = if (isDateRollover) 0L else previous.todayTxBytes

        if (isDateRollover) {
            persistFields(
                totalRx = previous.totalRxBytes,
                totalTx = previous.totalTxBytes,
                todayRx = 0L,
                todayTx = 0L,
                todayDate = currentDate,
                lastRx = previous.lastRxBytes,
                lastTx = previous.lastTxBytes,
                rebootCount = previous.rebootCount,
                startedAt = previous.startedAtMillis
            )
        }

        val shouldZeroSpeed = status != ConnectionStatus.CONNECTED
        val nextSnapshot = previous.copy(
            todayRxBytes = todayRx,
            todayTxBytes = todayTx,
            todayDate = currentDate,
            downloadSpeedBps = if (shouldZeroSpeed) 0.0 else previous.downloadSpeedBps,
            uploadSpeedBps = if (shouldZeroSpeed) 0.0 else previous.uploadSpeedBps,
            connectionStatus = status
        )
        _state.value = nextSnapshot
        nextSnapshot
    }

    fun addPriorUsageGb(downloadGb: Double, uploadGb: Double): RouterUsageSnapshot = synchronized(lock) {
        val addRx = (downloadGb.coerceAtLeast(0.0) * Formatters.BYTES_PER_GB).toLong()
        val addTx = (uploadGb.coerceAtLeast(0.0) * Formatters.BYTES_PER_GB).toLong()
        val previous = _state.value
        val newTotalRx = previous.totalRxBytes + addRx
        val newTotalTx = previous.totalTxBytes + addTx

        persistFields(
            totalRx = newTotalRx,
            totalTx = newTotalTx,
            todayRx = previous.todayRxBytes,
            todayTx = previous.todayTxBytes,
            todayDate = previous.todayDate.ifEmpty { Formatters.todayDateKey() },
            lastRx = previous.lastRxBytes,
            lastTx = previous.lastTxBytes,
            rebootCount = previous.rebootCount,
            startedAt = previous.startedAtMillis
        )

        val nextSnapshot = previous.copy(
            totalRxBytes = newTotalRx,
            totalTxBytes = newTotalTx
        )
        _state.value = nextSnapshot
        nextSnapshot
    }

    fun resetMonthlyTotals(): RouterUsageSnapshot = synchronized(lock) {
        val now = System.currentTimeMillis()
        val currentDate = Formatters.todayDateKey(now)
        val previous = _state.value

        persistFields(
            totalRx = 0L,
            totalTx = 0L,
            todayRx = 0L,
            todayTx = 0L,
            todayDate = currentDate,
            lastRx = previous.lastRxBytes,
            lastTx = previous.lastTxBytes,
            rebootCount = 0,
            startedAt = now
        )

        val nextSnapshot = previous.copy(
            totalRxBytes = 0L,
            totalTxBytes = 0L,
            todayRxBytes = 0L,
            todayTxBytes = 0L,
            todayDate = currentDate,
            rebootCount = 0,
            startedAtMillis = now
        )
        _state.value = nextSnapshot
        nextSnapshot
    }

    private fun persistFields(
        totalRx: Long,
        totalTx: Long,
        todayRx: Long,
        todayTx: Long,
        todayDate: String,
        lastRx: Long,
        lastTx: Long,
        rebootCount: Int,
        startedAt: Long
    ) {
        prefs.edit()
            .putLong(KEY_TOTAL_RX, totalRx)
            .putLong(KEY_TOTAL_TX, totalTx)
            .putLong(KEY_TODAY_RX, todayRx)
            .putLong(KEY_TODAY_TX, todayTx)
            .putString(KEY_TODAY_DATE, todayDate)
            .putLong(KEY_LAST_RX, lastRx)
            .putLong(KEY_LAST_TX, lastTx)
            .putInt(KEY_REBOOT_COUNT, rebootCount)
            .putLong(KEY_STARTED_AT, startedAt)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "router_tracker_usage_prefs"
        private const val MAX_SPEED_HISTORY_POINTS = 24

        const val KEY_TOTAL_RX = "total_rx"
        const val KEY_TOTAL_TX = "total_tx"
        const val KEY_TODAY_RX = "today_rx"
        const val KEY_TODAY_TX = "today_tx"
        const val KEY_TODAY_DATE = "today_date"
        const val KEY_LAST_RX = "last_rx"
        const val KEY_LAST_TX = "last_tx"
        const val KEY_REBOOT_COUNT = "reboot_count"
        const val KEY_STARTED_AT = "started_at"

        @Volatile
        private var instance: RouterUsageStore? = null

        fun getInstance(context: Context): RouterUsageStore {
            return instance ?: synchronized(this) {
                instance ?: RouterUsageStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
