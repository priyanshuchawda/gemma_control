package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VoiceSpokenOutputDebugSinkTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun recordWritesTrimmedSpokenOutputWhenEnabled() {
        val file = temporaryFolder.root.resolve("debug/last_spoken_output.txt")
        val sink = FileVoiceSpokenOutputDebugSink(file = file, enabled = true)

        sink.record("  You have 3 WhatsApp messages.  ")

        assertTrue(file.exists())
        assertEquals("You have 3 WhatsApp messages.", file.readText())
    }

    @Test
    fun recordBlankClearsExistingOutput() {
        val file = temporaryFolder.root.resolve("debug/last_spoken_output.txt")
        val sink = FileVoiceSpokenOutputDebugSink(file = file, enabled = true)
        sink.record("Existing output")

        sink.record("   ")

        assertFalse(file.exists())
    }

    @Test
    fun clearRemovesExistingOutput() {
        val file = temporaryFolder.root.resolve("debug/last_spoken_output.txt")
        val sink = FileVoiceSpokenOutputDebugSink(file = file, enabled = true)
        sink.record("Existing output")

        sink.clear()

        assertFalse(file.exists())
    }

    @Test
    fun disabledSinkDoesNotWritePrivateOutput() {
        val file = temporaryFolder.root.resolve("debug/last_spoken_output.txt")
        val sink = FileVoiceSpokenOutputDebugSink(file = file, enabled = false)

        sink.record("Private WhatsApp text")

        assertFalse(file.exists())
    }
}
