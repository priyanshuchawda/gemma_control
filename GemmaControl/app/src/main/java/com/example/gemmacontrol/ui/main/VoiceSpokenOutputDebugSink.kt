package com.example.gemmacontrol.ui.main

import android.app.Application
import android.content.pm.ApplicationInfo
import java.io.File

internal interface VoiceSpokenOutputDebugSink {
    fun record(text: String)
    fun clear()
}

internal class FileVoiceSpokenOutputDebugSink(
    private val file: File,
    private val enabled: Boolean
) : VoiceSpokenOutputDebugSink {

    override fun record(text: String) {
        if (!enabled) {
            return
        }

        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            clear()
            return
        }

        file.parentFile?.mkdirs()
        file.writeText(trimmed)
    }

    override fun clear() {
        if (!enabled) {
            return
        }
        if (file.exists()) {
            file.delete()
        }
    }
}

internal fun Application.voiceSpokenOutputDebugSink(): VoiceSpokenOutputDebugSink {
    return FileVoiceSpokenOutputDebugSink(
        file = File(cacheDir, "debug/last_spoken_output.txt"),
        enabled = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    )
}
