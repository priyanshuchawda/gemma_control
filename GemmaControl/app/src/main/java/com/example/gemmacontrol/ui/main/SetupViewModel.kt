package com.example.gemmacontrol.ui.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val _batteryOptExempt = MutableStateFlow(false)
    val batteryOptExempt: StateFlow<Boolean> = _batteryOptExempt

    private val _notificationListenerEnabled = MutableStateFlow(false)
    val notificationListenerEnabled: StateFlow<Boolean> = _notificationListenerEnabled

    /** Call on every ON_RESUME to refresh permission states. */
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

    /** Returns true if all required permissions are granted and setup is complete. */
    fun isSetupComplete(context: Context): Boolean {
        refreshPermissions(context)
        return _batteryOptExempt.value && _notificationListenerEnabled.value
    }
}
