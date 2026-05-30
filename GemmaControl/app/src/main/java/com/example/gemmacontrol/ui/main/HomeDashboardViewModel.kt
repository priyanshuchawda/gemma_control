package com.example.gemmacontrol.ui.main

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.ServiceLocator
import com.example.gemmacontrol.ai.model.FunctionGemmaModelResolver
import com.example.gemmacontrol.ai.model.InstalledFunctionGemmaModel
import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.WhatsAppNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeDashboardViewModel(
    application: Application,
    private val storedInboxRepository: StoredInboxRepository = ServiceLocator.getStoredInboxRepository(application),
    private val modelResolver: FunctionGemmaModelResolver = FunctionGemmaModelResolver(
        filesDir = application.filesDir,
        cacheDir = application.cacheDir
    )
) : AndroidViewModel(application) {
    private val isPermissionGranted = MutableStateFlow(false)
    private val actionableItemCount = MutableStateFlow(0)
    private val modelReadiness = MutableStateFlow(resolveModelReadiness())

    val uiState: StateFlow<HomeDashboardScreenState> = combine(
        WhatsAppNotificationListener.capturedNotifications,
        isPermissionGranted,
        storedInboxRepository.getAllDecryptedMessagesFlow(),
        actionableItemCount,
        modelReadiness
    ) { notifications, permission, storedMessages, actionables, model ->
        buildHomeDashboardReadyState(
            HomeDashboardData(
                notifications = notifications,
                isPermissionGranted = permission,
                storedMessageCount = storedMessages.size,
                actionableItemCount = actionables,
                modelReadiness = model
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeDashboardScreenState.Loading
    )

    init {
        refreshActionableItemCount()
    }

    fun refreshDashboard(context: Context) {
        refreshPermission(context)
        modelReadiness.value = resolveModelReadiness()
        refreshActionableItemCount()
    }

    fun clearNotifications() {
        WhatsAppNotificationListener.clearList()
    }

    private fun refreshPermission(context: Context) {
        val packageName = context.packageName
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        isPermissionGranted.value = enabledListeners
            ?.split(":")
            ?.mapNotNull(ComponentName::unflattenFromString)
            ?.any { it.packageName == packageName }
            ?: false
    }

    private fun refreshActionableItemCount() {
        viewModelScope.launch {
            actionableItemCount.value = runCatching {
                storedInboxRepository.getActionableInbox(
                    status = null,
                    priority = null,
                    limit = HOME_ACTIONABLE_LIMIT
                ).size
            }.getOrDefault(0)
        }
    }

    private fun resolveModelReadiness(): HomeModelReadiness {
        return when (modelResolver.resolveMobileActionsModel()) {
            is InstalledFunctionGemmaModel.Ready -> HomeModelReadiness.Ready
            is InstalledFunctionGemmaModel.Missing -> HomeModelReadiness.Missing
        }
    }

    private companion object {
        const val HOME_ACTIONABLE_LIMIT = 50
    }
}
