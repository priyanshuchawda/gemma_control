package com.example.gemmacontrol.ui.main

import android.content.Context
import android.content.Intent
import android.provider.Settings

internal fun Context.openSpeechSettings() {
    try {
        startActivity(Intent("android.settings.VOICE_INPUT_SETTINGS"))
    } catch (e: Exception) {
        try {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        } catch (ex: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
