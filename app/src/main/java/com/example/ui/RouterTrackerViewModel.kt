package com.example.ui

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RouterUsageSnapshot
import com.example.data.RouterUsageStore
import com.example.service.RouterMonitorService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RouterTrackerUiState(
    val snapshot: RouterUsageSnapshot = RouterUsageSnapshot(),
    val isBatteryOptimizationIgnored: Boolean = false,
    val showAddPriorDialog: Boolean = false,
    val showResetConfirmDialog: Boolean = false
)

class RouterTrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val store = RouterUsageStore.getInstance(application)
    private val isBatteryIgnoredFlow = MutableStateFlow(checkBatteryOptimizationIgnored())
    private val showAddPriorDialogFlow = MutableStateFlow(false)
    private val showResetDialogFlow = MutableStateFlow(false)

    val uiState: StateFlow<RouterTrackerUiState> = combine(
        store.state,
        isBatteryIgnoredFlow,
        showAddPriorDialogFlow,
        showResetDialogFlow
    ) { snapshot, batteryIgnored, showAddDialog, showResetDialog ->
        RouterTrackerUiState(
            snapshot = snapshot,
            isBatteryOptimizationIgnored = batteryIgnored,
            showAddPriorDialog = showAddDialog,
            showResetConfirmDialog = showResetDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = RouterTrackerUiState(
            snapshot = store.state.value,
            isBatteryOptimizationIgnored = isBatteryIgnoredFlow.value
        )
    )

    fun refreshBatteryOptimizationState() {
        isBatteryIgnoredFlow.value = checkBatteryOptimizationIgnored()
    }

    fun openAddPriorDialog() {
        showAddPriorDialogFlow.value = true
    }

    fun dismissAddPriorDialog() {
        showAddPriorDialogFlow.value = false
    }

    fun addPriorUsage(downloadGb: Double, uploadGb: Double) {
        store.addPriorUsageGb(downloadGb = downloadGb, uploadGb = uploadGb)
        RouterMonitorService.refreshNotification(getApplication())
        showAddPriorDialogFlow.value = false
    }

    fun openResetConfirmDialog() {
        showResetDialogFlow.value = true
    }

    fun dismissResetConfirmDialog() {
        showResetDialogFlow.value = false
    }

    fun confirmMonthlyReset() {
        store.resetMonthlyTotals()
        RouterMonitorService.refreshNotification(getApplication())
        showResetDialogFlow.value = false
    }

    fun requestRouterRediscovery() {
        RouterMonitorService.requestRediscover(getApplication())
    }

    private fun checkBatteryOptimizationIgnored(): Boolean {
        val context = getApplication<Application>()
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
    }
}
