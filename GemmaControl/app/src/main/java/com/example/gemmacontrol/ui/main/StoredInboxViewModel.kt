package com.example.gemmacontrol.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.ai.tools.LocalActionableInboxItem
import com.example.gemmacontrol.ServiceLocator
import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.RecentOutgoingReplyEchoSuppressor
import com.example.gemmacontrol.notifications.ReplySendResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StoredInboxViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = ServiceLocator.getPreferencesRepository(application)
    private val storedInboxRepository = ServiceLocator.getStoredInboxRepository(application)

    val captureEnabled: StateFlow<Boolean> = preferencesRepository.captureEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val storageEnabled: StateFlow<Boolean> = preferencesRepository.storageEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val messages: StateFlow<List<StoredInboxRepository.DecryptedMessage>> = storedInboxRepository.getAllDecryptedMessagesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRepliesAvailability: StateFlow<Map<String, Boolean>> = com.example.gemmacontrol.notifications.InMemoryActiveReplyActionRegistry.availabilityFlow

    private val _actionableItems = MutableStateFlow<List<LocalActionableInboxItem>>(emptyList())
    val actionableItems: StateFlow<List<LocalActionableInboxItem>> = _actionableItems.asStateFlow()

    private val replyExecutor = com.example.gemmacontrol.notifications.ActiveNotificationReplyExecutor()

    init {
        refreshActionableInbox()
    }

    fun sendConfirmedReply(
        notificationKey: String,
        text: String
    ): com.example.gemmacontrol.notifications.ReplySendResult {
        val result = replyExecutor.sendConfirmedReply(
            context = getApplication(),
            notificationKey = notificationKey,
            text = text
        )
        if (result == ReplySendResult.Success) {
            RecentOutgoingReplyEchoSuppressor.register(notificationKey, text)
        }
        return result
    }

    fun setCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCaptureEnabled(enabled)
        }
    }

    fun setStorageEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setStorageEnabled(enabled)
            refreshActionableInbox()
        }
    }

    fun deleteAllMessages() {
        viewModelScope.launch {
            storedInboxRepository.deleteAllData()
            _actionableItems.value = emptyList()
        }
    }

    fun deleteConversationMessages(conversationName: String) {
        viewModelScope.launch {
            storedInboxRepository.deleteConversationData(conversationName)
            refreshActionableInbox()
        }
    }

    fun refreshActionableInbox() {
        viewModelScope.launch {
            _actionableItems.value = runCatching {
                storedInboxRepository.getActionableInbox(
                    status = null,
                    priority = null,
                    limit = ACTIONABLE_INBOX_LIMIT
                )
            }.getOrDefault(emptyList())
        }
    }

    private companion object {
        const val ACTIONABLE_INBOX_LIMIT = 20
    }
}
