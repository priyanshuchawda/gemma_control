package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun VoiceAssistantActionPanel(
    state: VoiceAssistantState,
    actions: VoiceAssistantScreenActions,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presentation = voiceAssistantActionPresentation(state)

    Box(modifier = modifier) {
        VoiceAssistantInlineActionContent(
            state = state,
            presentation = presentation,
            onCommandSelected = actions.onTypedCommandSubmit,
            onStopSpeaking = actions.onStopSpeaking,
            onStopResponse = actions.onStopResponse
        )
    }

    if (presentation == VoiceActionPresentation.BottomSheet) {
        VoiceAssistantActionBottomSheet(
            state = state,
            sheetState = sheetState,
            actions = actions
        )
    }
}

@Composable
private fun VoiceAssistantInlineActionContent(
    state: VoiceAssistantState,
    presentation: VoiceActionPresentation,
    onCommandSelected: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onStopResponse: () -> Unit,
) {
    when {
        presentation == VoiceActionPresentation.Inline && state == VoiceAssistantState.Idle -> {
            VoiceCommandExamplesCard(onCommandSelected = onCommandSelected)
        }
        presentation == VoiceActionPresentation.Inline && state is VoiceAssistantState.Streaming -> {
            StreamingResponseCard(
                partialText = state.partialText,
                onStopResponse = onStopResponse
            )
        }
        presentation == VoiceActionPresentation.Inline && state is VoiceAssistantState.SpeakingMessages -> {
            SpeakingMessagesCard(onStopSpeaking)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceAssistantActionBottomSheet(
    state: VoiceAssistantState,
    sheetState: SheetState,
    actions: VoiceAssistantScreenActions,
) {
    ModalBottomSheet(
        onDismissRequest = actions.onCancel,
        sheetState = sheetState
    ) {
        VoiceAssistantActionSheetContent(
            state = state,
            actions = actions
        )
    }
}
