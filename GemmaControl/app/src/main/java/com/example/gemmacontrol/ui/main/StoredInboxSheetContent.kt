package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.data.repository.StoredInboxRepository.DecryptedMessage
import com.example.gemmacontrol.notifications.localReadSummaryText
import com.example.gemmacontrol.notifications.ReplySendResult

@Composable
internal fun StoredInboxSheetContent(
    sheet: StoredInboxSheet,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onEnableStorage: () -> Unit,
    onDeleteAll: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onCancel: () -> Unit,
    onReviewReply: (DecryptedMessage) -> Unit,
    onSendReply: (DecryptedMessage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = storedInboxSheetTitle(sheet),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
        )

        when (sheet) {
            StoredInboxSheet.EnableStorage -> EnableStorageSheetBody(
                onCancel = onCancel,
                onEnableStorage = onEnableStorage
            )
            StoredInboxSheet.DeleteAll -> DeleteAllSheetBody(
                onCancel = onCancel,
                onDeleteAll = onDeleteAll
            )
            is StoredInboxSheet.DeleteConversation -> DeleteConversationSheetBody(
                conversationName = sheet.conversationName,
                onCancel = onCancel,
                onDeleteConversation = onDeleteConversation
            )
            is StoredInboxSheet.ComposeReply -> ComposeReplySheetBody(
                message = sheet.message,
                replyText = replyText,
                onReplyTextChange = onReplyTextChange,
                onCancel = onCancel,
                onReviewReply = onReviewReply
            )
            is StoredInboxSheet.ConfirmReply -> ConfirmReplySheetBody(
                message = sheet.message,
                replyText = replyText,
                onCancel = onCancel,
                onSendReply = onSendReply
            )
        }
    }
}

@Composable
private fun EnableStorageSheetBody(
    onCancel: () -> Unit,
    onEnableStorage: () -> Unit
) {
    Text(
        text = "Incoming WhatsApp message previews will be encrypted and stored locally on this device. Your data stays on the phone.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    TwoActionButtons(
        secondaryText = "Cancel",
        primaryText = "Enable",
        onSecondaryClick = onCancel,
        onPrimaryClick = onEnableStorage
    )
}

@Composable
private fun DeleteAllSheetBody(
    onCancel: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Text(
        text = "This permanently deletes locally stored WhatsApp messages, follow-ups, reminders, and active references from this app. It does not affect WhatsApp chats.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    TwoActionButtons(
        secondaryText = "Cancel",
        primaryText = "Delete All",
        onSecondaryClick = onCancel,
        onPrimaryClick = onDeleteAll,
        destructive = true
    )
}

@Composable
private fun DeleteConversationSheetBody(
    conversationName: String,
    onCancel: () -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    Text(
        text = "This permanently deletes locally stored WhatsApp message previews for $conversationName from this app. It does not delete anything inside WhatsApp.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    TwoActionButtons(
        secondaryText = "Cancel",
        primaryText = "Delete Chat",
        onSecondaryClick = onCancel,
        onPrimaryClick = { onDeleteConversation(conversationName) },
        destructive = true
    )
}

@Composable
private fun ComposeReplySheetBody(
    message: DecryptedMessage,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onReviewReply: (DecryptedMessage) -> Unit
) {
    ReplyMessagePreview(message)
    OutlinedTextField(
        value = replyText,
        onValueChange = onReplyTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(StoredInboxReplyFieldHeight),
        label = { Text("Your reply") },
        placeholder = { Text("Type your reply...") },
        maxLines = 5,
        supportingText = {
            Text(
                text = "${replyText.length} / $StoredInboxReplyTextLimit",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    )
    TwoActionButtons(
        secondaryText = "Cancel",
        primaryText = "Review Send",
        onSecondaryClick = onCancel,
        onPrimaryClick = { onReviewReply(message) },
        primaryEnabled = replyText.trim().isNotEmpty()
    )
}

@Composable
private fun ConfirmReplySheetBody(
    message: DecryptedMessage,
    replyText: String,
    onCancel: () -> Unit,
    onSendReply: (DecryptedMessage) -> Unit
) {
    Text(
        text = "Send this reply through the active WhatsApp notification?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    ReplyMessagePreview(message)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = replyText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
    TwoActionButtons(
        secondaryText = "Cancel",
        primaryText = "Confirm Send",
        onSecondaryClick = onCancel,
        onPrimaryClick = { onSendReply(message) }
    )
}

@Composable
private fun ReplyMessagePreview(message: DecryptedMessage) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (message.senderName != null && message.senderName != message.conversationId) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = message.contentKind.localReadSummaryText(message.decryptedText),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TwoActionButtons(
    secondaryText: String,
    primaryText: String,
    onSecondaryClick: () -> Unit,
    onPrimaryClick: () -> Unit,
    destructive: Boolean = false,
    primaryEnabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSecondaryClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(secondaryText)
        }
        Button(
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            colors = if (destructive) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(primaryText)
        }
    }
}

internal fun ReplySendResult.toSnackbarMessage(): String {
    return when (this) {
        ReplySendResult.Success ->
            "Reply sent through active notification."
        ReplySendResult.NoActiveReplyAction,
        ReplySendResult.NotificationExpired ->
            "Reply unavailable. The notification may have expired or been cleared."
        else ->
            "Reply could not be sent safely. Try from a new notification."
    }
}

internal const val StoredInboxReplyTextLimit = 1_000

private val StoredInboxReplyFieldHeight = 132.dp
