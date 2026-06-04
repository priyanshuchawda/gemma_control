package com.example.gemmacontrol.ai.benchmark

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

class AndroidRuntimeBenchmarkSnapshotProvider(
    private val context: Context
) {
    fun capture(): RuntimeDeviceSnapshot {
        val applicationContext = context.applicationContext
        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val processMemoryInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(processMemoryInfo)
        val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIntent = applicationContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        return RuntimeDeviceSnapshot(
            deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL} Android ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}",
            totalMemoryBytes = memoryInfo.totalMem,
            availableMemoryBytes = memoryInfo.availMem,
            appPssKb = processMemoryInfo.totalPss.toLong(),
            appRssKb = null,
            batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                .takeIf { it >= 0 },
            charging = batteryManager.isCharging,
            batteryTemperatureC = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                ?.takeIf { it >= 0 }
                ?.let { it / 10f },
            thermalStatus = if (Build.VERSION.SDK_INT >= 29) powerManager.currentThermalStatus else null,
            notificationListenerEnabled = isNotificationListenerEnabled(applicationContext),
            postNotificationsGranted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED,
            recordAudioGranted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabled.contains(context.packageName, ignoreCase = true)
    }
}
