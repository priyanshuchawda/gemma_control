package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gemmacontrol.ai.tools.LocalActionableInboxItem
import com.example.gemmacontrol.data.repository.StoredInboxRepository.DecryptedMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoredInboxScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoredInboxViewModel = viewModel()
) {
    val captureEnabled by viewModel.captureEnabled.collectAsStateWithLifecycle()
    val storageEnabled by viewModel.storageEnabled.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val activeRepliesAvailability by viewModel.activeRepliesAvailability.collectAsStateWithLifecycle()
    val actionableItems by viewModel.actionableItems.collectAsStateWithLifecycle()

    var activeSheet by remember { mutableStateOf<StoredInboxSheet?>(null) }
    var replyText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US) }
    val actionableState = buildActionableInboxSectionState(actionableItems)

    LaunchedEffect(messages) {
        viewModel.refreshActionableInbox()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Stored Inbox",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { activeSheet = StoredInboxSheet.DeleteAll }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete All",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Description & Info Panel
            item(key = "description_panel") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Secure Local Storage",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Stored message previews are encrypted locally on this device using AES-GCM backed by Android Keystore. Summary notifications are not stored as duplicate inbox messages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Consent Toggles Card
            item(key = "settings_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Store message previews locally",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Required to save inbox messages on disk",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = storageEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        activeSheet = StoredInboxSheet.EnableStorage
                                    } else {
                                        viewModel.setStorageEnabled(false)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Capture WhatsApp notifications",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Pause or resume the notification listener",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Switch(
                                checked = captureEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setCaptureEnabled(enabled)
                                }
                            )
                        }
                    }
                }
            }

            item(key = "actionable_inbox") {
                ActionableInboxSection(
                    items = actionableItems,
                    state = actionableState
                )
            }

            // Message List Header
            item(key = "inbox_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Encrypted Inbox",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${messages.size} message${if (messages.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Message List / Empty State
            if (messages.isEmpty()) {
                item(key = "empty_inbox") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No stored messages",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (storageEnabled)
                                "Waiting for incoming WhatsApp notifications to persist."
                            else
                                "Local storage is currently disabled. Enable it above to securely persist WhatsApp messages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    val isReplyAvailable = activeRepliesAvailability[message.notificationKey] ?: false
                    StoredMessageRow(
                        message = message,
                        formatter = formatter,
                        isReplyAvailable = isReplyAvailable,
                        onReplyClick = {
                            replyText = ""
                            activeSheet = StoredInboxSheet.ComposeReply(message)
                        }
                    )
                }
            }
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = {
                activeSheet = null
                replyText = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            StoredInboxSheetContent(
                sheet = sheet,
                replyText = replyText,
                onReplyTextChange = { if (it.length <= 1000) replyText = it },
                onEnableStorage = {
                    viewModel.setStorageEnabled(true)
                    activeSheet = null
                },
                onDeleteAll = {
                    viewModel.deleteAllMessages()
                    activeSheet = null
                },
                onCancel = {
                    activeSheet = null
                    replyText = ""
                },
                onReviewReply = { message ->
                    if (replyText.trim().isNotEmpty()) {
                        activeSheet = StoredInboxSheet.ConfirmReply(message)
                    }
                },
                onSendReply = { message ->
                    val result = viewModel.sendConfirmedReply(message.notificationKey, replyText)
                    activeSheet = null
                    replyText = ""
                    scope.launch {
                        snackbarHostState.showSnackbar(result.toSnackbarMessage())
                    }
                }
            )
        }
    }
}

@Composable
private fun StoredInboxSheetContent(
    sheet: StoredInboxSheet,
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onEnableStorage: () -> Unit,
    onDeleteAll: () -> Unit,
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
            .height(132.dp),
        label = { Text("Your reply") },
        placeholder = { Text("Type your reply...") },
        maxLines = 5,
        supportingText = {
            Text(
                text = "${replyText.length} / 1000",
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
                text = message.decryptedText ?: "",
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

private fun com.example.gemmacontrol.notifications.ReplySendResult.toSnackbarMessage(): String {
    return when (this) {
        com.example.gemmacontrol.notifications.ReplySendResult.Success ->
            "Reply sent through active notification."
        com.example.gemmacontrol.notifications.ReplySendResult.NoActiveReplyAction,
        com.example.gemmacontrol.notifications.ReplySendResult.NotificationExpired ->
            "Reply unavailable. The notification may have expired or been cleared."
        else ->
            "Reply could not be sent safely. Try from a new notification."
    }
}

@Composable
private fun ActionableInboxSection(
    items: List<LocalActionableInboxItem>,
    state: ActionableInboxSectionState
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Actionable Inbox",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionableCounterPill(
                        label = state.pendingCount.toString(),
                        icon = Icons.Default.CheckCircle
                    )
                    if (state.highPriorityCount > 0) {
                        ActionableCounterPill(
                            label = state.highPriorityCount.toString(),
                            icon = Icons.Default.Warning
                        )
                    }
                }
            }

            if (!state.hasItems) {
                ActionableEmptyState(state)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.take(3).forEach { item ->
                        ActionableInboxItemRow(item)
                    }
                    if (items.size > 3) {
                        Text(
                            text = "+${items.size - 3} more action item${if (items.size - 3 == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionableCounterPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ActionableEmptyState(state: ActionableInboxSectionState) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = state.emptyTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = state.emptySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionableInboxItemRow(item: LocalActionableInboxItem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.conversationName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = actionableInboxTypeLabel(item.type),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item.text?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ActionableMetaChip(item.priority)
                ActionableMetaChip(item.status)
                item.dueAt?.takeIf { it.isNotBlank() }?.let { dueAt ->
                    ActionableMetaChip(dueAt)
                }
            }
        }
    }
}

@Composable
private fun ActionableMetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
