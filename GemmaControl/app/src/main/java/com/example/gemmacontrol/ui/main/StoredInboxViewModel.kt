package com.example.gemmacontrol.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.ServiceLocator
import com.example.gemmacontrol.data.repository.StoredInboxRepository
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
