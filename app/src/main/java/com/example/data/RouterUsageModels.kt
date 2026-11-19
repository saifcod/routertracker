package com.example.data

enum class ConnectionStatus {
    DISCOVERING,
    CONNECTED,
    OFFLINE
}

data class RouterEndpoint(
    val locationUrl: String,
    val controlUrl: String,
    val serviceType: String,
    val friendlyName: String?,
    val host: String
)

data class RouterUsageSnapshot(
    val totalRxBytes: Long = 0L,
    val totalTxBytes: Long = 0L,
    val todayRxBytes: Long = 0L,
    val todayTxBytes: Long = 0L,
    val todayDate: String = "",
    val lastRxBytes: Long = -1L,
    val lastTxBytes: Long = -1L,
    val rebootCount: Int = 0,
    val startedAtMillis: Long = System.currentTimeMillis(),
    val downloadSpeedBps: Double = 0.0,
    val uploadSpeedBps: Double = 0.0,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCOVERING,
    val routerName: String? = null,
    val routerHost: String? = null,
    val lastUpdatedMillis: Long = 0L,
    val speedHistoryRx: List<Double> = emptyList(),
    val speedHistoryTx: List<Double> = emptyList()
) {
    val totalCombinedBytes: Long
        get() = (totalRxBytes.coerceAtLeast(0L)) + (totalTxBytes.coerceAtLeast(0L))

    val todayCombinedBytes: Long
        get() = (todayRxBytes.coerceAtLeast(0L)) + (todayTxBytes.coerceAtLeast(0L))

    val sessionRxBytes: Long
        get() = lastRxBytes.coerceAtLeast(0L)

    val sessionTxBytes: Long
        get() = lastTxBytes.coerceAtLeast(0L)

    val sessionCombinedBytes: Long
        get() = sessionRxBytes + sessionTxBytes
}
