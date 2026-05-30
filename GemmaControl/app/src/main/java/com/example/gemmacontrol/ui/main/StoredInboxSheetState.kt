package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.repository.StoredInboxRepository

sealed interface StoredInboxSheet {
    data object EnableStorage : StoredInboxSheet
    data object DeleteAll : StoredInboxSheet
    data class ComposeReply(val message: StoredInboxRepository.DecryptedMessage) : StoredInboxSheet
    data class ConfirmReply(val message: StoredInboxRepository.DecryptedMessage) : StoredInboxSheet
}

fun storedInboxSheetTitle(sheet: StoredInboxSheet): String {
    return when (sheet) {
        StoredInboxSheet.EnableStorage -> "Enable Local Storage?"
        StoredInboxSheet.DeleteAll -> "Delete All Messages?"
        is StoredInboxSheet.ComposeReply -> "Reply to ${sheet.message.conversationId}"
        is StoredInboxSheet.ConfirmReply -> "Confirm Send?"
    }
}
