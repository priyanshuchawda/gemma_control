package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceQuickCommandExamplesTest {

    @Test
    fun quickCommandsStaySupportedByVoiceParser() {
        assertTrue(voiceQuickCommandExamples.isNotEmpty())

        voiceQuickCommandExamples.forEach { command ->
            assertFalse(
                "Quick command should be supported: $command",
                VoiceCommandParser.parse(command) is VoiceCommand.Unsupported
            )
        }
    }
}
