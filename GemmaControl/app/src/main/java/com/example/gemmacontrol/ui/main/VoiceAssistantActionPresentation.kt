package com.example.gemmacontrol.ui.main

enum class VoiceActionPresentation {
    None,
    Inline,
    BottomSheet
}

fun voiceAssistantActionPresentation(state: VoiceAssistantState): VoiceActionPresentation {
    return when (state) {
        VoiceAssistantState.Idle,
        is VoiceAssistantState.Streaming,
        is VoiceAssistantState.SpeakingMessages -> VoiceActionPresentation.Inline

        is VoiceAssistantState.CommandReady -> {
            if (state.command is VoiceReadCommand) {
                VoiceActionPresentation.BottomSheet
            } else {
                VoiceActionPresentation.None
            }
        }

        is VoiceAssistantState.ConfirmationRequired,
        is VoiceAssistantState.LocalToolConfirmationRequired,
        is VoiceAssistantState.LocalToolSucceeded,
        is VoiceAssistantState.Failure,
        VoiceAssistantState.LanguagePackMissingError,
        VoiceAssistantState.ConfirmSystemRecognitionConsent -> VoiceActionPresentation.BottomSheet

        VoiceAssistantState.RequestingMicrophonePermission,
        VoiceAssistantState.Listening,
        is VoiceAssistantState.TranscriptReady -> VoiceActionPresentation.None
    }
}
