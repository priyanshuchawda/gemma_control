package com.example.gemmacontrol.ui.main

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

private const val TAG = "VoiceTtsController"
private const val VoiceAssistantUtteranceId = "GemmaControlTTS"

internal class VoiceTextToSpeechController(
    application: Application,
    private val onFinished: () -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(progressListener())
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech")
            }
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, VoiceAssistantUtteranceId)
        } else {
            Log.e(TAG, "TTS not initialized")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun shutdown() {
        try {
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }

    private fun progressListener(): UtteranceProgressListener {
        return object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS done: $utteranceId")
                onFinished()
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                handleTtsError(utteranceId, null)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                handleTtsError(utteranceId, errorCode)
            }
        }
    }

    private fun handleTtsError(utteranceId: String?, errorCode: Int?) {
        val suffix = errorCode?.let { " code=$it" }.orEmpty()
        Log.e(TAG, "TTS error: $utteranceId$suffix")
        onFinished()
    }
}
