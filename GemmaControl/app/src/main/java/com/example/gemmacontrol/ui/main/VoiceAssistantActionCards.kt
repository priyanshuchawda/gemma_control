package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun VoiceAssistantActionSheetContent(
    state: VoiceAssistantState,
    actions: VoiceAssistantScreenActions,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        VoiceAssistantActionSheetBody(
            state = state,
            actions = actions
        )
    }
}

@Composable
private fun VoiceAssistantActionSheetBody(
    state: VoiceAssistantState,
    actions: VoiceAssistantScreenActions,
) {
    when (state) {
        VoiceAssistantState.Idle -> VoiceCommandExamplesCard()
        is VoiceAssistantState.CommandReady -> ReadLatestCommandCard(
            command = state.command,
            actions = actions
        )
        is VoiceAssistantState.ConfirmationRequired -> VoiceReplyConfirmationCard(
            draft = state.draft,
            onCancel = actions.onCancel,
            onConfirmSend = actions.onConfirmSend
        )
        is VoiceAssistantState.LocalToolConfirmationRequired -> LocalToolConfirmationCard(
            action = state.action,
            onCancel = actions.onCancel,
            onConfirm = actions.onConfirmLocalTool
        )
        is VoiceAssistantState.LocalToolSucceeded -> LocalToolSucceededCard(
            message = state.message,
            onDismiss = actions.onCancel
        )
        is VoiceAssistantState.Failure -> VoiceFailureCard(
            reason = state.safeReason,
            onDismiss = actions.onCancel
        )
        VoiceAssistantState.LanguagePackMissingError -> LanguagePackMissingCard(
            onOpenSpeechSettings = actions.onOpenSpeechSettings,
            onAllowSystemRecognition = actions.onAllowSystemRecognition,
            onCancel = actions.onCancel
        )
        VoiceAssistantState.ConfirmSystemRecognitionConsent -> SystemRecognitionConsentCard(
            onCancel = actions.onCancel,
            onContinue = actions.onContinueSystemRecognition
        )
        else -> Unit
    }
}

@Composable
private fun ReadLatestCommandCard(
    command: VoiceCommand,
    actions: VoiceAssistantScreenActions,
) {
    if (command is VoiceCommand.ReadLatestMessages) {
        ReadLatestConfirmationCard(
            onCancel = actions.onCancel,
            onReadAloud = actions.onReadAloud
        )
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
                modifier = Modifier.padding(top = 2.dp)
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
