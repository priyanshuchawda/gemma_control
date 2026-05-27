package com.example.gemmacontrol.ui.main

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import com.example.gemmacontrol.notifications.WhatsAppNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainScreenViewModel : ViewModel() {

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted

    val uiState: StateFlow<MainScreenUiState> = combine(
        WhatsAppNotificationListener.capturedNotifications,
        _isPermissionGranted
    ) { notifications, permission ->
        MainScreenUiState.Success(
            notifications = notifications,
            isPermissionGranted = permission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenUiState.Loading
    )

    fun checkPermission(context: Context) {
        val pkgName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        var enabled = false
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(":")
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && cn.packageName == pkgName) {
                    enabled = true
                    break
                }
            }
        }
        _isPermissionGranted.value = enabled
    }

    fun clearNotifications() {
        WhatsAppNotificationListener.clearList()
    }
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Success(
        val notifications: List<ParsedWhatsAppNotificationEvent>,
        val isPermissionGranted: Boolean
    ) : MainScreenUiState
}
