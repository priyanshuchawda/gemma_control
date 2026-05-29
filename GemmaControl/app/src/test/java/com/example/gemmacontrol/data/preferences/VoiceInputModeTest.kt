package com.example.gemmacontrol.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceInputModeTest {

    @Test
    fun defaultsToTapToggleWhenStoredValueIsMissingOrUnknown() {
        assertEquals(VoiceInputMode.TapToggle, VoiceInputMode.fromStoredValue(null))
        assertEquals(VoiceInputMode.TapToggle, VoiceInputMode.fromStoredValue(""))
        assertEquals(VoiceInputMode.TapToggle, VoiceInputMode.fromStoredValue("legacy"))
    }

    @Test
    fun parsesPersistedHoldToSpeakMode() {
        assertEquals(VoiceInputMode.HoldToSpeak, VoiceInputMode.fromStoredValue("hold_to_speak"))
    }
}
