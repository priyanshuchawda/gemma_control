package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.preferences.VoiceInputMode

private const val HOLD_TO_SPEAK_STOP_DELAY_MILLIS = 500L

internal enum class VoiceHoldToSpeakReleaseAction {
    Finalize,
    CancelRecognition
}

internal fun voiceHoldToSpeakReleaseAction(wasGestureCancelled: Boolean): VoiceHoldToSpeakReleaseAction {
    return if (wasGestureCancelled) {
        VoiceHoldToSpeakReleaseAction.CancelRecognition
    } else {
        VoiceHoldToSpeakReleaseAction.Finalize
    }
}

internal fun voiceRecognitionStopDelayMillis(mode: VoiceInputMode): Long {
    return when (mode) {
        VoiceInputMode.TapToggle -> 0L
        VoiceInputMode.HoldToSpeak -> HOLD_TO_SPEAK_STOP_DELAY_MILLIS
    }
}
