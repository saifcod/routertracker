package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.RouterMonitorService
import com.example.ui.RouterTrackerScreen
import com.example.ui.RouterTrackerViewModel
import com.example.ui.theme.RouterTrackerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RouterTrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Start 24/7 foreground UPnP monitor service immediately
        runCatching { RouterMonitorService.start(this) }

        setContent {
            RouterTrackerTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current

                var hasNotificationPermission by remember {
                    mutableStateOf(isNotificationPermissionGranted())
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasNotificationPermission = granted
                    if (granted) {
                        RouterMonitorService.refreshNotification(this@MainActivity)
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshBatteryOptimizationState()
                            hasNotificationPermission = isNotificationPermissionGranted()
                            runCatching { RouterMonitorService.start(this@MainActivity) }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                RouterTrackerScreen(
                    uiState = uiState,
                    needsNotificationPermission =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission,
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestBatteryProtection = {
                        openBatteryOptimizationSettings()
                    },
                    onRediscoverClick = {
                        viewModel.requestRouterRediscovery()
                    },
                    onOpenAddPriorDialog = {
                        viewModel.openAddPriorDialog()
                    },
                    onDismissAddPriorDialog = {
                        viewModel.dismissAddPriorDialog()
                    },
                    onConfirmAddPrior = { rxGb, txGb ->
                        viewModel.addPriorUsage(rxGb, txGb)
                    },
                    onOpenResetDialog = {
                        viewModel.openResetConfirmDialog()
                    },
                    onDismissResetDialog = {
                        viewModel.dismissResetConfirmDialog()
                    },
                    onConfirmReset = {
                        viewModel.confirmMonthlyReset()
                    }
                )
            }
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationSettings() {
        val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        val launched = runCatching {
            startActivity(directIntent)
            true
        }.getOrDefault(false)

        if (!launched) {
            runCatching {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }
}
