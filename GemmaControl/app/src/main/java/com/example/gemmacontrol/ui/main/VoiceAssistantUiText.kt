package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.preferences.VoiceInputMode

internal fun voiceAssistantStatusTitle(state: VoiceAssistantState): String {
    return when (state) {
        VoiceAssistantState.Idle -> "Speak commands for WhatsApp notifications"
        VoiceAssistantState.RequestingMicrophonePermission -> "Requesting microphone access..."
        VoiceAssistantState.Listening -> "Listening... Speak now"
        is VoiceAssistantState.TranscriptReady -> "Transcribing..."
        is VoiceAssistantState.CommandReady -> "Command recognized"
        is VoiceAssistantState.ConfirmationRequired -> "Review Dictated Reply"
        is VoiceAssistantState.SpeakingMessages -> "Reading latest messages"
        is VoiceAssistantState.Failure -> "Error encountered"
        VoiceAssistantState.LanguagePackMissingError -> "Language Pack Missing"
        VoiceAssistantState.ConfirmSystemRecognitionConsent -> "Allow System Recognition?"
    }
}

internal fun voiceAssistantSubtitle(
    state: VoiceAssistantState,
    isOffline: Boolean,
    voiceInputMode: VoiceInputMode = VoiceInputMode.TapToggle
): String {
    return when (state) {
        VoiceAssistantState.Idle -> when (voiceInputMode) {
            VoiceInputMode.TapToggle -> "Tap microphone below to start speaking."
            VoiceInputMode.HoldToSpeak -> "Hold microphone to speak. Release to process, slide off to cancel."
        }
        VoiceAssistantState.Listening -> {
            val privacyNote = if (isOffline) {
                "Private on-device speech recognition active."
            } else {
                "System recognition active - speech may be processed outside this device."
            }
            val inputModeHint = if (voiceInputMode == VoiceInputMode.HoldToSpeak) {
                "\nRelease to process, slide off to cancel."
            } else {
                ""
            }
            "$privacyNote$inputModeHint\n\nTry: 'Read my latest messages' or 'Reply to the latest message: I am in a meeting'"
        }
        is VoiceAssistantState.Failure -> state.safeReason
        VoiceAssistantState.LanguagePackMissingError -> "Offline language pack unavailable."
        VoiceAssistantState.ConfirmSystemRecognitionConsent -> "Requires explicit consent to use network-based recognition."
        else -> ""
    }
}
