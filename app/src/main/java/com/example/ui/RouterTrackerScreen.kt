package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.components.AddPriorUsageDialog
import com.example.ui.components.BatteryProtectionButton
import com.example.ui.components.BottomActionsRow
import com.example.ui.components.CumulativeHeroCard
import com.example.ui.components.LiveSpeedRow
import com.example.ui.components.MonthlyResetConfirmDialog
import com.example.ui.components.NotificationPermissionBanner
import com.example.ui.components.RebootProtectionBanner
import com.example.ui.components.RouterDashboardHeader
import com.example.ui.components.RouterSessionCard
import com.example.ui.components.TodayUsageCard
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.PurpleAccent

@Composable
fun RouterTrackerScreen(
    uiState: RouterTrackerUiState,
    needsNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryProtection: () -> Unit,
    onRediscoverClick: () -> Unit,
    onOpenAddPriorDialog: () -> Unit,
    onDismissAddPriorDialog: () -> Unit,
    onConfirmAddPrior: (downloadGb: Double, uploadGb: Double) -> Unit,
    onOpenResetDialog: () -> Unit,
    onDismissResetDialog: () -> Unit,
    onConfirmReset: () -> Unit
) {
    Scaffold(
        containerColor = MidnightBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MidnightBg)
                .drawBehind {
                    // Subtle atmospheric cyan & purple telemetry glow at top of viewport
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CyanAccent.copy(alpha = 0.14f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.08f),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = Offset(size.width * 0.18f, size.height * 0.08f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PurpleAccent.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.16f),
                            radius = size.width * 0.65f
                        ),
                        radius = size.width * 0.65f,
                        center = Offset(size.width * 0.88f, size.height * 0.16f)
                    )
                }
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(key = "header") {
                    RouterDashboardHeader(
                        connectionStatus = uiState.snapshot.connectionStatus
                    )
                }

                if (needsNotificationPermission) {
                    item(key = "notification_permission") {
                        NotificationPermissionBanner(
                            onRequestPermission = onRequestNotificationPermission
                        )
                    }
                }

                item(key = "hero_total") {
                    CumulativeHeroCard(snapshot = uiState.snapshot)
                }

                item(key = "live_speeds") {
                    LiveSpeedRow(
                        downloadSpeedBps = uiState.snapshot.downloadSpeedBps,
                        uploadSpeedBps = uiState.snapshot.uploadSpeedBps,
                        historyRx = uiState.snapshot.speedHistoryRx,
                        historyTx = uiState.snapshot.speedHistoryTx
                    )
                }

                item(key = "reboot_banner") {
                    RebootProtectionBanner(rebootCount = uiState.snapshot.rebootCount)
                }

                item(key = "battery_button") {
                    BatteryProtectionButton(
                        isProtected = uiState.isBatteryOptimizationIgnored,
                        onRequestProtection = onRequestBatteryProtection
                    )
                }

                item(key = "today_card") {
                    TodayUsageCard(snapshot = uiState.snapshot)
                }

                item(key = "session_card") {
                    RouterSessionCard(
                        snapshot = uiState.snapshot,
                        onRediscoverClick = onRediscoverClick
                    )
                }

                item(key = "bottom_actions") {
                    BottomActionsRow(
                        onAddPriorClick = onOpenAddPriorDialog,
                        onMonthlyResetClick = onOpenResetDialog,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                }
            }
        }
    }

    if (uiState.showAddPriorDialog) {
        AddPriorUsageDialog(
            onDismiss = onDismissAddPriorDialog,
            onConfirm = onConfirmAddPrior
        )
    }

    if (uiState.showResetConfirmDialog) {
        MonthlyResetConfirmDialog(
            onDismiss = onDismissResetDialog,
            onConfirm = onConfirmReset
        )
    }
}
