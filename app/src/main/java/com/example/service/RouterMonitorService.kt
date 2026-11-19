package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ConnectionStatus
import com.example.data.Formatters
import com.example.data.RouterEndpoint
import com.example.data.RouterUsageSnapshot
import com.example.data.RouterUsageStore
import com.example.upnp.UpnpRouterClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 24/7 foreground service (dataSync) that continuously polls the local home router
 * via UPnP every 3 seconds, persists cumulative totals on every sample, detects
 * power outages/reboots, and auto-rediscovers the router after 2 consecutive failures.
 */
class RouterMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var store: RouterUsageStore
    private lateinit var upnpClient: UpnpRouterClient
    private lateinit var notificationManager: NotificationManager

    private var pollingJob: Job? = null

    @Volatile
    private var cachedEndpoint: RouterEndpoint? = null

    @Volatile
    private var consecutiveFailures: Int = 0

    @Volatile
    private var lastSuccessElapsedMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        store = RouterUsageStore.getInstance(this)
        upnpClient = UpnpRouterClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        startForegroundSafe(buildNotification(store.state.value))
        startPollingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_REDISCOVER -> {
                cachedEndpoint = null
                consecutiveFailures = 0
                val snapshot = store.updateConnectionStatus(ConnectionStatus.DISCOVERING)
                updateNotification(snapshot)
            }

            ACTION_REFRESH_NOTIFICATION -> {
                updateNotification(store.state.value)
            }
        }

        if (pollingJob == null || pollingJob?.isActive != true) {
            startPollingLoop()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        pollingJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                val cycleStartMs = SystemClock.elapsedRealtime()
                runSinglePollCycle()
                val spentMs = SystemClock.elapsedRealtime() - cycleStartMs
                val remainingDelay = (POLL_INTERVAL_MS - spentMs).coerceAtLeast(MIN_LOOP_GAP_MS)
                delay(remainingDelay)
            }
        }
    }

    private fun runSinglePollCycle() {
        var endpoint = cachedEndpoint
        if (endpoint == null) {
            if (store.state.value.connectionStatus != ConnectionStatus.OFFLINE) {
                val discoveringSnapshot = store.updateConnectionStatus(ConnectionStatus.DISCOVERING)
                updateNotification(discoveringSnapshot)
            }
            endpoint = upnpClient.discoverRouter()
            if (endpoint == null) {
                val offlineSnapshot = store.updateConnectionStatus(ConnectionStatus.OFFLINE)
                updateNotification(offlineSnapshot)
                return
            }
            cachedEndpoint = endpoint
            consecutiveFailures = 0
        }

        try {
            val counters = upnpClient.queryTrafficCounters(endpoint)
            val nowElapsedMs = SystemClock.elapsedRealtime()
            val elapsedSeconds = if (lastSuccessElapsedMs > 0L) {
                (nowElapsedMs - lastSuccessElapsedMs) / 1000.0
            } else {
                POLL_INTERVAL_MS / 1000.0
            }
            lastSuccessElapsedMs = nowElapsedMs
            consecutiveFailures = 0

            val updatedSnapshot = store.recordPollSample(
                currentRxBytes = counters.totalBytesReceived,
                currentTxBytes = counters.totalBytesSent,
                elapsedSeconds = elapsedSeconds,
                endpoint = endpoint
            )
            updateNotification(updatedSnapshot)
        } catch (_: Exception) {
            consecutiveFailures += 1
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                // Many routers change UPnP URL paths after a reboot: clear cache & re-discover
                cachedEndpoint = null
                consecutiveFailures = 0
                lastSuccessElapsedMs = 0L
                val offlineSnapshot = store.updateConnectionStatus(ConnectionStatus.OFFLINE)
                updateNotification(offlineSnapshot)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundSafe(notification: Notification) {
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun updateNotification(snapshot: RouterUsageSnapshot) {
        runCatching {
            notificationManager.notify(NOTIFICATION_ID, buildNotification(snapshot))
        }
    }

    private fun buildNotification(snapshot: RouterUsageSnapshot): Notification {
        val totalFormatted = Formatters.formatBytes(snapshot.totalCombinedBytes)
        val todayFormatted = Formatters.formatBytes(snapshot.todayCombinedBytes)

        val title = getString(R.string.notification_title_format, totalFormatted, todayFormatted)
        val body = when (snapshot.connectionStatus) {
            ConnectionStatus.CONNECTED -> getString(
                R.string.notification_speed_format,
                Formatters.formatSpeed(snapshot.downloadSpeedBps),
                Formatters.formatSpeed(snapshot.uploadSpeedBps)
            )
            ConnectionStatus.DISCOVERING -> getString(R.string.notification_discovering_body)
            ConnectionStatus.OFFLINE -> getString(R.string.notification_offline_body)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            pendingIntentFlags
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_router)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentPendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "router_tracker_monitor_channel"
        private const val NOTIFICATION_ID = 1042
        private const val POLL_INTERVAL_MS = 3000L
        private const val MIN_LOOP_GAP_MS = 500L
        private const val MAX_CONSECUTIVE_FAILURES = 2

        const val ACTION_START = "com.routertracker.app.action.START"
        const val ACTION_REDISCOVER = "com.routertracker.app.action.REDISCOVER"
        const val ACTION_REFRESH_NOTIFICATION = "com.routertracker.app.action.REFRESH_NOTIFICATION"

        fun start(context: Context) {
            val intent = Intent(context.applicationContext, RouterMonitorService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun requestRediscover(context: Context) {
            val intent = Intent(context.applicationContext, RouterMonitorService::class.java).apply {
                action = ACTION_REDISCOVER
            }
            ContextCompat.startForegroundService(context.applicationContext, intent)
        }

        fun refreshNotification(context: Context) {
            val intent = Intent(context.applicationContext, RouterMonitorService::class.java).apply {
                action = ACTION_REFRESH_NOTIFICATION
            }
            runCatching {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            }
        }
    }
}
