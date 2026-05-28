package com.example.gemmacontrol.ui.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class SetupUiState(
    val isXiaomiLikeDevice: Boolean = false,
    val notificationAccessEnabled: Boolean = false,
    val batteryOptimizationIgnored: Boolean = false,
    val xiaomiAutostartAcknowledged: Boolean = false
) {
    val minimumAccessGranted: Boolean
        get() = notificationAccessEnabled

    val reliabilitySetupComplete: Boolean
        get() = if (isXiaomiLikeDevice) {
            notificationAccessEnabled &&
                batteryOptimizationIgnored &&
                xiaomiAutostartAcknowledged
        } else {
            notificationAccessEnabled
        }
}

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = ServiceLocator.getPreferencesRepository(application)

    private val isXiaomiLikeDevice = checkIsXiaomiLikeDevice()

    private val _batteryOptExempt = MutableStateFlow(false)
    private val _notificationListenerEnabled = MutableStateFlow(false)

    val uiState: StateFlow<SetupUiState> = combine(
        _notificationListenerEnabled,
        _batteryOptExempt,
        preferencesRepository.xiaomiAutostartAcknowledgedFlow
    ) { listenerEnabled, batteryExempt, autostartAck ->
        SetupUiState(
            isXiaomiLikeDevice = isXiaomiLikeDevice,
            notificationAccessEnabled = listenerEnabled,
            batteryOptimizationIgnored = batteryExempt,
            xiaomiAutostartAcknowledged = autostartAck
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SetupUiState(isXiaomiLikeDevice = isXiaomiLikeDevice)
    )

    fun refreshPermissions(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _batteryOptExempt.value = pm.isIgnoringBatteryOptimizations(context.packageName)

        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        _notificationListenerEnabled.value = !flat.isNullOrEmpty() && flat.split(":").any { name ->
            val cn = ComponentName.unflattenFromString(name)
            cn != null && cn.packageName == context.packageName
        }
    }

    fun acknowledgeAutostart(acknowledged: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setXiaomiAutostartAcknowledged(acknowledged)
        }
    }

    fun isSetupComplete(context: Context): Boolean {
        refreshPermissions(context)
        val autostartAck = try {
            runBlocking { preferencesRepository.xiaomiAutostartAcknowledgedFlow.first() }
        } catch (e: Exception) {
            false
        }
        val isXiaomi = checkIsXiaomiLikeDevice()
        return if (isXiaomi) {
            _notificationListenerEnabled.value && _batteryOptExempt.value && autostartAck
        } else {
            _notificationListenerEnabled.value
        }
    }

    private fun checkIsXiaomiLikeDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
               Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
               Build.MANUFACTURER.equals("POCO", ignoreCase = true)
    }
}
