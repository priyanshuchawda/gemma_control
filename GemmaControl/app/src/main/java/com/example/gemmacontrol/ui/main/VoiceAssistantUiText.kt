package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.preferences.VoiceInputMode

internal fun voiceAssistantStatusTitle(state: VoiceAssistantState): String {
    return when (state) {
        VoiceAssistantState.Idle -> "Command WhatsApp notifications"
        VoiceAssistantState.RequestingMicrophonePermission -> "Requesting microphone access..."
        VoiceAssistantState.Listening -> "Listening... Speak now"
        is VoiceAssistantState.TranscriptReady -> "Transcribing..."
        is VoiceAssistantState.CommandReady -> "Command recognized"
        is VoiceAssistantState.Streaming -> "Generating reply"
        is VoiceAssistantState.ConfirmationRequired -> "Review Dictated Reply"
        is VoiceAssistantState.LocalToolConfirmationRequired -> "Review Local Action"
        is VoiceAssistantState.LocalToolSucceeded -> "Action complete"
        is VoiceAssistantState.SpeakingMessages -> "Reading stored messages"
        is VoiceAssistantState.ClarificationRequired -> "Clarification needed"
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
            VoiceInputMode.TapToggle -> "Speak or type a command below."
            VoiceInputMode.HoldToSpeak -> "Hold microphone to speak, or type a command below."
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
            "$privacyNote$inputModeHint\n\nTry: 'Read my latest stored messages', 'Search WhatsApp for payment', or 'Reply to the latest message: I am in a meeting'"
        }
        is VoiceAssistantState.Failure -> state.safeReason
        is VoiceAssistantState.Streaming -> "FunctionGemma is drafting a local response."
        is VoiceAssistantState.LocalToolConfirmationRequired -> state.action.description
        is VoiceAssistantState.LocalToolSucceeded -> state.message
        is VoiceAssistantState.ClarificationRequired -> state.prompt
        VoiceAssistantState.LanguagePackMissingError -> "Offline language pack unavailable."
        VoiceAssistantState.ConfirmSystemRecognitionConsent -> "Requires explicit consent to use network-based recognition."
        else -> ""
    }
}
