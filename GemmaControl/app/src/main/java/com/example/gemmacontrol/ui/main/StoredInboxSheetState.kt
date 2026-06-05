package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.repository.StoredInboxRepository

sealed interface StoredInboxSheet {
    data object EnableStorage : StoredInboxSheet
    data object DeleteAll : StoredInboxSheet
    data class DeleteConversation(val conversationName: String) : StoredInboxSheet
    data class ComposeReply(val message: StoredInboxRepository.DecryptedMessage) : StoredInboxSheet
    data class ConfirmReply(val message: StoredInboxRepository.DecryptedMessage) : StoredInboxSheet
}

fun storedInboxSheetTitle(sheet: StoredInboxSheet): String {
    return when (sheet) {
        StoredInboxSheet.EnableStorage -> "Enable Local Storage?"
        StoredInboxSheet.DeleteAll -> "Delete all stored messages?"
        is StoredInboxSheet.DeleteConversation -> "Delete stored messages from ${sheet.conversationName}?"
        is StoredInboxSheet.ComposeReply -> "Reply to ${sheet.message.conversationId}"
        is StoredInboxSheet.ConfirmReply -> "Confirm Send?"
    }
}

data class StoredInboxCleanupState(
    val hasMessages: Boolean,
    val deleteAllLabel: String,
    val conversationNames: List<String>
)

fun buildStoredInboxCleanupState(
    messages: List<StoredInboxRepository.DecryptedMessage>
): StoredInboxCleanupState {
    val conversationNames = messages
        .map { it.conversationId.trim() }
        .filter { it.isNotBlank() }
        .distinct()

    return StoredInboxCleanupState(
        hasMessages = messages.isNotEmpty(),
        deleteAllLabel = "Delete all stored messages",
        conversationNames = conversationNames
    )
}
