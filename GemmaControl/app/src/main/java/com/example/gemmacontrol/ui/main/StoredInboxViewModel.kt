package com.example.gemmacontrol.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.ServiceLocator
import com.example.gemmacontrol.data.repository.StoredInboxRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val replyExecutor = com.example.gemmacontrol.notifications.ActiveNotificationReplyExecutor()
    private val modelAdapter = com.example.gemmacontrol.ai.GemmaModelAdapter()

    private val _aiDraftState = MutableStateFlow<AiDraftState>(AiDraftState.Idle)
    val aiDraftState: StateFlow<AiDraftState> = _aiDraftState.asStateFlow()

    sealed interface AiDraftState {
        data object Idle : AiDraftState
        data object Loading : AiDraftState
        data class Success(val draft: com.example.gemmacontrol.ai.AssistantToolProposal.DraftReply) : AiDraftState
        data class Failure(val error: String) : AiDraftState
        data object ModelNotInstalled : AiDraftState
    }

    fun generateAiProposal(message: StoredInboxRepository.DecryptedMessage) {
        viewModelScope.launch {
            _aiDraftState.value = AiDraftState.Loading
            val text = message.decryptedText ?: ""
            if (text.isEmpty()) {
                _aiDraftState.value = AiDraftState.Failure("Message text is empty")
                return@launch
            }

            val result = modelAdapter.generateDraftReply(listOf(text))

            when (result) {
                is com.example.gemmacontrol.ai.ProposalGenerationResult.Success -> {
                    _aiDraftState.value = AiDraftState.Success(
                        com.example.gemmacontrol.ai.AssistantToolProposal.DraftReply(
                            notificationKey = message.notificationKey,
                            replyText = result.proposalText
                        )
                    )
                }
                is com.example.gemmacontrol.ai.ProposalGenerationResult.ModelNotInstalled -> {
                    _aiDraftState.value = AiDraftState.ModelNotInstalled
                }
                is com.example.gemmacontrol.ai.ProposalGenerationResult.Failed -> {
                    _aiDraftState.value = AiDraftState.Failure(result.safeReason)
                }
                is com.example.gemmacontrol.ai.ProposalGenerationResult.InvalidOutput -> {
                    _aiDraftState.value = AiDraftState.Failure("Invalid model output format")
                }
            }
        }
    }

    fun clearAiProposal() {
        _aiDraftState.value = AiDraftState.Idle
    }

    fun sendConfirmedReply(
        notificationKey: String,
        text: String
    ): com.example.gemmacontrol.notifications.ReplySendResult {
        return replyExecutor.sendConfirmedReply(
            context = getApplication(),
            notificationKey = notificationKey,
            text = text
        )
    }

    fun setCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCaptureEnabled(enabled)
        }
    }

    fun setStorageEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setStorageEnabled(enabled)
        }
    }

    fun deleteAllMessages() {
        viewModelScope.launch {
            storedInboxRepository.deleteAllData()
        }
    }
}
