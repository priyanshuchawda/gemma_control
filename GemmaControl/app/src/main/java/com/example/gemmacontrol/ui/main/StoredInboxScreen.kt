package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.data.repository.StoredInboxRepository.DecryptedMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val Green800  = Color(0xFF2E7D32)
private val Blue800   = Color(0xFF1565C0)
private val Red800    = Color(0xFFC62828)
private val Teal700   = Color(0xFF00796B)
private val Purple700 = Color(0xFF5E35B1)
private val Orange800 = Color(0xFFE65100)

private val GreenBg  = Color(0xFFE8F5E9)
private val BlueBg   = Color(0xFFE3F2FD)
private val RedBg    = Color(0xFFFFEBEE)
private val TealBg   = Color(0xFFE0F2F1)
private val PurpleBg = Color(0xFFEDE7F6)
private val OrangeBg = Color(0xFFFFF3E0)

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
    val aiDraftState by viewModel.aiDraftState.collectAsStateWithLifecycle()

    var showStorageConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var activeReplyMessage by remember { mutableStateOf<DecryptedMessage?>(null) }
    var replyText by remember { mutableStateOf("") }
    var showReplyDialog by remember { mutableStateOf(false) }
    var showReplyConfirmDialog by remember { mutableStateOf(false) }
    var showAiDraftDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val formatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US) }

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
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
                                        showStorageConfirmDialog = true
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
                            activeReplyMessage = message
                            replyText = ""
                            showReplyDialog = true
                        },
                        onSuggestReplyClick = {
                            activeReplyMessage = message
                            replyText = ""
                            showAiDraftDialog = true
                        }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for enabling storage
    if (showStorageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStorageConfirmDialog = false },
            title = { Text("Enable Local Storage?") },
            text = {
                Text("This will encrypt and persist incoming WhatsApp message previews locally on this device. Your data will never leave your phone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setStorageEnabled(true)
                        showStorageConfirmDialog = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStorageConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog for deleting all data
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete All Messages?") },
            text = {
                Text("Are you sure you want to permanently delete all locally stored WhatsApp messages? This action cannot be undone and will not affect your chats in WhatsApp.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteAllMessages()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reply Composer Dialog
    if (showReplyDialog && activeReplyMessage != null) {
        val message = activeReplyMessage!!
        AlertDialog(
            onDismissRequest = {
                showReplyDialog = false
                replyText = ""
                activeReplyMessage = null
            },
            title = {
                Text(
                    text = "Reply to ${message.conversationId}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (message.senderName != null && message.senderName != message.conversationId) {
                                Text(
                                    text = message.senderName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = message.decryptedText ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { if (it.length <= 1000) replyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
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
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyText.trim().isNotEmpty()) {
                            showReplyConfirmDialog = true
                        }
                    },
                    enabled = replyText.trim().isNotEmpty()
                ) {
                    Text("Review Send")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReplyDialog = false
                        replyText = ""
                        activeReplyMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Suggest Reply Dialog
    if (showAiDraftDialog && activeReplyMessage != null) {
        val message = activeReplyMessage!!
        AlertDialog(
            onDismissRequest = {
                showAiDraftDialog = false
                viewModel.clearAiProposal()
                replyText = ""
                activeReplyMessage = null
            },
            title = {
                Text(
                    text = "FunctionGemma Proposal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (message.senderName != null && message.senderName != message.conversationId) {
                                Text(
                                    text = message.senderName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = message.decryptedText ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    when (val draftState = aiDraftState) {
                        StoredInboxViewModel.AiDraftState.Idle -> {
                            LaunchedEffect(Unit) {
                                viewModel.generateAiProposal(message)
                            }
                        }
                        StoredInboxViewModel.AiDraftState.Loading -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    "Analyzing local context...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        is StoredInboxViewModel.AiDraftState.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Suggested Reply (Review & Edit):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { if (it.length <= 1000) replyText = it },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    label = { Text("AI Proposal") },
                                    maxLines = 5,
                                    supportingText = {
                                        Text(
                                            text = "${replyText.length} / 1000",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                )

                                LaunchedEffect(draftState.draft) {
                                    replyText = draftState.draft.replyText
                                }
                            }
                        }
                        is StoredInboxViewModel.AiDraftState.Failure -> {
                            Text(
                                text = "Draft generation failed: ${draftState.error}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        StoredInboxViewModel.AiDraftState.ModelNotInstalled -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "FunctionGemma model is not installed on this device.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "One-time model provisioning is required to enable on-device local AI reply suggestions.",
                                    color = MaterialTheme.colorScheme.outline,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (aiDraftState is StoredInboxViewModel.AiDraftState.Success) {
                    Button(
                        onClick = {
                            if (replyText.trim().isNotEmpty()) {
                                showReplyConfirmDialog = true
                            }
                        },
                        enabled = replyText.trim().isNotEmpty()
                    ) {
                        Text("Review Send")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAiDraftDialog = false
                        viewModel.clearAiProposal()
                        replyText = ""
                        activeReplyMessage = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Final Confirmation Dialog
    if (showReplyConfirmDialog && activeReplyMessage != null) {
        val message = activeReplyMessage!!
        AlertDialog(
            onDismissRequest = { showReplyConfirmDialog = false },
            title = { Text("Confirm Send?") },
            text = {
                Text("Send this reply through the active WhatsApp notification?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReplyConfirmDialog = false
                        showReplyDialog = false
                        showAiDraftDialog = false

                        val result = viewModel.sendConfirmedReply(message.notificationKey, replyText)

                        replyText = ""
                        activeReplyMessage = null
                        viewModel.clearAiProposal()

                        scope.launch {
                            val msg = when (result) {
                                com.example.gemmacontrol.notifications.ReplySendResult.Success ->
                                    "Reply sent through active notification."
                                com.example.gemmacontrol.notifications.ReplySendResult.NoActiveReplyAction,
                                com.example.gemmacontrol.notifications.ReplySendResult.NotificationExpired ->
                                    "Reply unavailable. The notification may have expired or been cleared."
                                else ->
                                    "Reply could not be sent safely. Try from a new notification."
                            }
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                ) {
                    Text("Confirm Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplyConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StoredMessageRow(
    message: DecryptedMessage,
    formatter: SimpleDateFormat,
    isReplyAvailable: Boolean,
    onReplyClick: () -> Unit,
    onSuggestReplyClick: () -> Unit
) {
    val dateStr = formatter.format(Date(message.postedAt))
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Display Name & Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.conversationId,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Message Bubble
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (message.senderName != null && message.senderName != message.conversationId) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = if (message.isContentUnavailable) "Content unavailable" else (message.decryptedText ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Info tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = message.parseSource.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "Key: …${if (message.notificationKey.length > 8) message.notificationKey.takeLast(8) else message.notificationKey}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isReplyAvailable) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSuggestReplyClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Suggest Reply", style = MaterialTheme.typography.labelMedium)
                        }

                        Button(
                            onClick = onReplyClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Reply", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Text(
                        text = "Reply unavailable — notification is no longer active",
                        style = MaterialTheme.typography.labelSmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
