package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val cleanupState = buildStoredInboxCleanupState(messages)

    LaunchedEffect(messages) { viewModel.refreshActionableInbox() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StoredInboxTopBar(
                hasMessages = messages.isNotEmpty(),
                onBack = onBack,
                onDeleteAll = { activeSheet = StoredInboxSheet.DeleteAll }
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
                StoredInboxInfoCard()
            }

            // Consent Toggles Card
            item(key = "settings_card") {
                StoredInboxSettingsCard(
                    storageEnabled = storageEnabled,
                    captureEnabled = captureEnabled,
                    onStorageCheckedChange = { checked ->
                        if (checked) {
                            activeSheet = StoredInboxSheet.EnableStorage
                        } else {
                            viewModel.setStorageEnabled(false)
                        }
                    },
                    onCaptureCheckedChange = viewModel::setCaptureEnabled
                )
            }

            item(key = "actionable_inbox") {
                ActionableInboxSection(
                    items = actionableItems,
                    state = actionableState
                )
            }

            item(key = "stored_inbox_cleanup") {
                StoredInboxCleanupCard(
                    state = cleanupState,
                    onDeleteAll = { activeSheet = StoredInboxSheet.DeleteAll },
                    onDeleteConversation = { conversationName ->
                        activeSheet = StoredInboxSheet.DeleteConversation(conversationName)
                    }
                )
            }

            item(key = "inbox_header") {
                StoredInboxListHeader(messages.size)
            }

            if (messages.isEmpty()) {
                item(key = "empty_inbox") {
                    StoredInboxEmptyState(storageEnabled)
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
                onReplyTextChange = { if (it.length <= StoredInboxReplyTextLimit) replyText = it },
                onEnableStorage = {
                    viewModel.setStorageEnabled(true)
                    activeSheet = null
                },
                onDeleteAll = {
                    viewModel.deleteAllMessages()
                    activeSheet = null
                },
                onDeleteConversation = { conversationName ->
                    viewModel.deleteConversationMessages(conversationName)
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
