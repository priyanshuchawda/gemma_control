package com.example.gemmacontrol.ui.main

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

private const val TAG = "VoiceRecognitionRuntime"
private const val ERROR_LANGUAGE_PACK_MISSING = 13
private const val ERROR_LANGUAGE_UNAVAILABLE = 12

internal data class VoiceRecognitionCallbacks(
    val onReadyForSpeech: () -> Unit,
    val onAmplitudeChanged: (Int) -> Unit,
    val onPartialTranscriptChanged: (String) -> Unit,
    val onTranscriptReady: (String) -> Unit,
    val onFailure: (String) -> Unit,
    val onLanguagePackMissing: () -> Unit,
    val onRecognitionFinished: () -> Unit
)

internal fun Context.hasRecordAudioPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

internal fun shouldUseOnDeviceRecognition(
    context: Context,
    forceSystemRecognition: Boolean,
): Boolean {
    val isDeviceRecognitionAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    } else {
        false
    }
    return isDeviceRecognitionAvailable && !forceSystemRecognition
}

internal fun createVoiceSpeechRecognizer(
    context: Context,
    useOnDevice: Boolean,
): SpeechRecognizer {
    return if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
    } else {
        SpeechRecognizer.createSpeechRecognizer(context)
    }
}

internal fun voiceRecognitionIntent(useOnDevice: Boolean): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }
}

internal fun voiceAssistantRecognitionListener(
    useOnDevice: Boolean,
    callbacks: VoiceRecognitionCallbacks,
): RecognitionListener = object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {
        callbacks.onReadyForSpeech()
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) {
        callbacks.onAmplitudeChanged(convertRmsDbToAmplitude(rmsdB))
    }

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() = Unit

    override fun onError(error: Int) {
        callbacks.onRecognitionFinished()
        if (useOnDevice && isOfflineLanguagePackError(error)) {
            Log.w(TAG, "Offline recognition language pack unavailable (code $error).")
            callbacks.onLanguagePackMissing()
            return
        }

        callbacks.onFailure(voiceRecognitionErrorMessage(error))
    }

    override fun onResults(results: Bundle?) {
        callbacks.onRecognitionFinished()
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()

        if (transcript.isNotEmpty()) {
            callbacks.onTranscriptReady(transcript)
        } else {
            callbacks.onFailure("No speech recognized.")
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val transcript = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        callbacks.onPartialTranscriptChanged(transcript)
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

private fun isOfflineLanguagePackError(error: Int): Boolean {
    return error == ERROR_LANGUAGE_PACK_MISSING || error == ERROR_LANGUAGE_UNAVAILABLE
}

private fun voiceRecognitionErrorMessage(error: Int): String {
    return when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error (requires connection)"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking clearly."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition engine is busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        ERROR_LANGUAGE_PACK_MISSING -> "Language pack for offline recognition is missing."
        else -> "Speech recognition error (Code $error)"
    }
}
