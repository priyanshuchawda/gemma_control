package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun VoiceAssistantActionSheetContent(
    state: VoiceAssistantState,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
    onConfirmLocalTool: (PendingLocalToolAction) -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onContinueSystemRecognition: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        VoiceAssistantActionSheetBody(
            state = state,
            onCancel = onCancel,
            onReadAloud = onReadAloud,
            onConfirmSend = onConfirmSend,
            onConfirmLocalTool = onConfirmLocalTool,
            onOpenSpeechSettings = onOpenSpeechSettings,
            onAllowSystemRecognition = onAllowSystemRecognition,
            onContinueSystemRecognition = onContinueSystemRecognition
        )
    }
}

@Composable
private fun VoiceAssistantActionSheetBody(
    state: VoiceAssistantState,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
    onConfirmLocalTool: (PendingLocalToolAction) -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onContinueSystemRecognition: () -> Unit,
) {
    when (state) {
        VoiceAssistantState.Idle -> VoiceCommandExamplesCard()
        is VoiceAssistantState.CommandReady -> ReadLatestCommandCard(
            command = state.command,
            onCancel = onCancel,
            onReadAloud = onReadAloud
        )
        is VoiceAssistantState.ConfirmationRequired -> VoiceReplyConfirmationCard(
            draft = state.draft,
            onCancel = onCancel,
            onConfirmSend = onConfirmSend
        )
        is VoiceAssistantState.LocalToolConfirmationRequired -> LocalToolConfirmationCard(
            action = state.action,
            onCancel = onCancel,
            onConfirm = onConfirmLocalTool
        )
        is VoiceAssistantState.LocalToolSucceeded -> LocalToolSucceededCard(
            message = state.message,
            onDismiss = onCancel
        )
        is VoiceAssistantState.Failure -> VoiceFailureCard(
            reason = state.safeReason,
            onDismiss = onCancel
        )
        VoiceAssistantState.LanguagePackMissingError -> LanguagePackMissingCard(
            onOpenSpeechSettings = onOpenSpeechSettings,
            onAllowSystemRecognition = onAllowSystemRecognition,
            onCancel = onCancel
        )
        VoiceAssistantState.ConfirmSystemRecognitionConsent -> SystemRecognitionConsentCard(
            onCancel = onCancel,
            onContinue = onContinueSystemRecognition
        )
        else -> Unit
    }
}

@Composable
private fun ReadLatestCommandCard(
    command: VoiceCommand,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit
) {
    if (command is VoiceCommand.ReadLatestMessages) {
        ReadLatestConfirmationCard(
            onCancel = onCancel,
            onReadAloud = onReadAloud
        )
    }
}

@Composable
internal fun VoiceCommandExamplesCard() {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Supported English Commands:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CommandExample("Read my latest messages")
                CommandExample("Reply to the latest message: I am in a meeting")
            }
        }
    }
}

@Composable
private fun ReadLatestConfirmationCard(
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                "Read your latest captured WhatsApp messages aloud?",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            TwoButtonRow(
                secondaryText = "Cancel",
                primaryText = "Read Aloud",
                onSecondaryClick = onCancel,
                onPrimaryClick = onReadAloud
            )
        }
    }
}

@Composable
private fun VoiceReplyConfirmationCard(
    draft: PendingVoiceReply,
    onCancel: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReplyCardHeader()
            ReplyDraftPreview(draft)
            TwoButtonRow(
                secondaryText = "Cancel",
                primaryText = "Confirm Send",
                onSecondaryClick = onCancel,
                onPrimaryClick = { onConfirmSend(draft) }
            )
        }
    }
}

@Composable
private fun ReplyCardHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Reply to latest active message?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun ReplyDraftPreview(draft: PendingVoiceReply) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "To: ${draft.conversationTitle}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = VoiceCardShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = draft.replyText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LocalToolConfirmationCard(
    action: PendingLocalToolAction,
    onCancel: () -> Unit,
    onConfirm: (PendingLocalToolAction) -> Unit,
) {
    val details = remember(action) { toolCallDetailsUiState(action) }
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    action.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToolCallDetailsPanel(details)
            TwoButtonRow(
                secondaryText = "Cancel",
                primaryText = action.confirmText,
                onSecondaryClick = onCancel,
                onPrimaryClick = { onConfirm(action) }
            )
        }
    }
}

@Composable
private fun ToolCallDetailsPanel(details: ToolCallDetailsUiState) {
    Surface(
        shape = VoiceCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "FunctionGemma proposed ${details.toolName}. ${details.safetyLabel}."
            }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolCallDetailsHeader(details.toolName)
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    details.safetyLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                details.boundaryLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ToolCallArguments(details)
        }
    }
}

@Composable
private fun ToolCallDetailsHeader(toolName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "FunctionGemma call",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                toolName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ToolCallArguments(details: ToolCallDetailsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (details.arguments.isEmpty()) {
            Text(
                details.emptyArgumentsLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            details.arguments.forEach { row ->
                ToolCallArgumentRow(row)
            }
        }
    }
}

@Composable
private fun ToolCallArgumentRow(row: ToolCallDetailRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            row.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            row.value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun LocalToolSucceededCard(
    message: String,
    onDismiss: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }
        }
    }
}

@Composable
internal fun StreamingResponseCard(
    partialText: String,
    onStopResponse: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text(
                partialText.ifBlank { "Waiting for FunctionGemma..." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onStopResponse,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Response")
            }
        }
    }
}

@Composable
internal fun SpeakingMessagesCard(onStopSpeaking: () -> Unit) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            Text(
                "Reading latest messages aloud...",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Button(
                onClick = onStopSpeaking,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Speaking", color = Color.White)
            }
        }
    }
}

@Composable
private fun VoiceFailureCard(
    reason: String,
    onDismiss: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Text(
                reason,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Okay", color = Color.White)
            }
        }
    }
}

@Composable
private fun LanguagePackMissingCard(
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "Offline speech language data is not installed for this language. Download it in speech settings, or explicitly allow system recognition.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            LanguagePackActions(
                onOpenSpeechSettings = onOpenSpeechSettings,
                onAllowSystemRecognition = onAllowSystemRecognition,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun LanguagePackActions(
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onCancel: () -> Unit,
) {
    Button(
        onClick = onOpenSpeechSettings,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open Speech Settings")
    }
    Button(
        onClick = onAllowSystemRecognition,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Allow System Recognition")
    }
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Cancel")
    }
}

@Composable
private fun SystemRecognitionConsentCard(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "System speech recognition may use Google services or network processing. Your spoken command may leave the device. Continue for this command?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            TwoButtonRow(
                secondaryText = "Cancel",
                primaryText = "Continue",
                onSecondaryClick = onCancel,
                onPrimaryClick = onContinue
            )
        }
    }
}

@Composable
private fun TwoButtonRow(
    secondaryText: String,
    primaryText: String,
    onSecondaryClick: () -> Unit,
    onPrimaryClick: () -> Unit,
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
            modifier = Modifier.weight(1f)
        ) {
            Text(primaryText)
        }
    }
}

@Composable
private fun CommandExample(command: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Text(
            text = "\"$command\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
