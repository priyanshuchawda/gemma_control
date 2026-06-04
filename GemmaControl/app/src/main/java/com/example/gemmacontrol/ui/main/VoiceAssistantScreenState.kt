package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.preferences.VoiceInputMode

internal data class VoiceAssistantScreenState(
    val state: VoiceAssistantState,
    val isOffline: Boolean,
    val voiceInputMode: VoiceInputMode,
    val partialTranscript: String,
    val amplitude: Int
)

internal data class VoiceAssistantScreenActions(
    val onBack: () -> Unit,
    val onMicClick: () -> Unit,
    val onHoldStart: () -> Unit,
    val onHoldRelease: () -> Unit,
    val onHoldCancel: () -> Unit,
    val onTypedCommandSubmit: (String) -> Unit,
    val onCancel: () -> Unit,
    val onReadAloud: () -> Unit,
    val onConfirmSend: (PendingVoiceReply) -> Unit,
    val onConfirmLocalTool: (PendingLocalToolAction) -> Unit,
    val onStopSpeaking: () -> Unit,
    val onStopResponse: () -> Unit,
    val onOpenSpeechSettings: () -> Unit,
    val onAllowSystemRecognition: () -> Unit,
    val onContinueSystemRecognition: () -> Unit
)
