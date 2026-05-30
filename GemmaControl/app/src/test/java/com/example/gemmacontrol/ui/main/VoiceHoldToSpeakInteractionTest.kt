package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.preferences.VoiceInputMode
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceHoldToSpeakInteractionTest {

    @Test
    fun releaseFinalizesWhenHoldGestureWasNotCancelled() {
        assertEquals(
            VoiceHoldToSpeakReleaseAction.Finalize,
            voiceHoldToSpeakReleaseAction(wasGestureCancelled = false)
        )
    }

    @Test
    fun slideOffCancelsRecognitionWithoutProcessingPartialSpeech() {
        assertEquals(
            VoiceHoldToSpeakReleaseAction.CancelRecognition,
            voiceHoldToSpeakReleaseAction(wasGestureCancelled = true)
        )
    }

    @Test
    fun holdModeUsesGalleryStopDelayToAvoidCuttingOffFinalWord() {
        assertEquals(500L, voiceRecognitionStopDelayMillis(VoiceInputMode.HoldToSpeak))
        assertEquals(0L, voiceRecognitionStopDelayMillis(VoiceInputMode.TapToggle))
    }
}
