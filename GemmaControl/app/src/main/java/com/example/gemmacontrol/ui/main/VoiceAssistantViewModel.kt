package com.example.gemmacontrol.ui.main

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.ServiceLocator
import com.example.gemmacontrol.notifications.ActiveNotificationReplyExecutor
import com.example.gemmacontrol.notifications.InMemoryActiveReplyActionRegistry
import com.example.gemmacontrol.notifications.ReplySendResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val AUDIO_METER_MIN_DB = -2.0f
private const val AUDIO_METER_MAX_DB = 100.0f

class VoiceAssistantViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoiceAssistantVM"
    }

    private val repository = ServiceLocator.getStoredInboxRepository(application)
    private val replyExecutor = ActiveNotificationReplyExecutor(InMemoryActiveReplyActionRegistry)
    private val toolProposalMapper = VoiceCommandToolProposalMapper()

    private val _state = MutableStateFlow<VoiceAssistantState>(VoiceAssistantState.Idle)
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private val _isOffline = MutableStateFlow(true)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var forceSystemRecognition = false
    private var isCurrentRecognizerOnDevice: Boolean? = null

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(getApplication()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                tts?.language = Locale.US
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS started: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS done: $utteranceId")
                        if (_state.value is VoiceAssistantState.SpeakingMessages) {
                            _state.value = VoiceAssistantState.Idle
                        }
                    }

                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) {
                        handleTtsError(utteranceId, null)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        handleTtsError(utteranceId, errorCode)
                    }

                    private fun handleTtsError(utteranceId: String?, errorCode: Int?) {
                        val suffix = errorCode?.let { " code=$it" }.orEmpty()
                        Log.e(TAG, "TTS error: $utteranceId$suffix")
                        if (_state.value is VoiceAssistantState.SpeakingMessages) {
                            _state.value = VoiceAssistantState.Idle
                        }
                    }
                })
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech")
            }
        }
    }

    fun startListening() {
        val context = getApplication<Application>()
        
        // 1. Check permission first
        val hasMicrophonePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!hasMicrophonePermission) {
            _state.value = VoiceAssistantState.RequestingMicrophonePermission
            return
        }

        // 2. Check if on-device SpeechRecognizer is available
        val isDeviceRecognitionAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            false
        }

        val useOnDevice = isDeviceRecognitionAvailable && !forceSystemRecognition

        // 3. Create SpeechRecognizer
        try {
            if (speechRecognizer == null || isCurrentRecognizerOnDevice != useOnDevice) {
                destroySpeechRecognizer()

                if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    _isOffline.value = true
                } else {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    _isOffline.value = false
                }
                isCurrentRecognizerOnDevice = useOnDevice
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                if (useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = VoiceAssistantState.Listening
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _amplitude.value = convertRmsDbToAmplitude(rmsdB)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    _partialTranscript.value = ""
                    _amplitude.value = 0
                    if (useOnDevice && (error == 13 || error == 12)) {
                        Log.w(TAG, "Offline recognition language pack unavailable (code $error).")
                        _state.value = VoiceAssistantState.LanguagePackMissingError
                        return
                    }

                    val safeReason = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error (requires connection)"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking clearly."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition engine is busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                        13 -> "Language pack for offline recognition is missing."
                        else -> "Speech recognition error (Code $error)"
                    }
                    _state.value = VoiceAssistantState.Failure(safeReason)
                }

                override fun onResults(results: Bundle?) {
                    _partialTranscript.value = ""
                    _amplitude.value = 0
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcript = matches?.firstOrNull() ?: ""
                    if (transcript.isNotEmpty()) {
                        processTranscript(transcript)
                    } else {
                        _state.value = VoiceAssistantState.Failure("No speech recognized.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    _partialTranscript.value = matches?.firstOrNull() ?: ""
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
            _state.value = VoiceAssistantState.Listening

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _state.value = VoiceAssistantState.Failure("Failed to start voice recognition: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            _amplitude.value = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping SpeechRecognizer", e)
        }
    }

    private fun processTranscript(transcript: String) {
        _state.value = VoiceAssistantState.TranscriptReady(transcript)
        val command = VoiceCommandParser.parse(transcript)
        _state.value = VoiceAssistantState.CommandReady(command)

        when (command) {
            is VoiceCommand.ReadLatestMessages -> {
                // ReadLatestMessages does not immediately speak; wait for user confirmation
            }
            is VoiceCommand.ReplyToLatestActiveMessage -> {
                prepareVoiceReply(command.replyText)
            }
            is VoiceCommand.Unsupported -> {
                _state.value = VoiceAssistantState.Failure(command.reason)
            }
        }
    }

    private fun prepareVoiceReply(replyText: String) {
        viewModelScope.launch {
            val activeKeys = InMemoryActiveReplyActionRegistry.availabilityFlow.value.keys
            if (activeKeys.isEmpty()) {
                _state.value = VoiceAssistantState.Failure("No active WhatsApp notification is available for reply.")
                return@launch
            }

            val decryptedMessages = repository.getAllDecryptedMessages()
            val latestActiveMessage = decryptedMessages
                .filter { it.notificationKey in activeKeys }
                .maxByOrNull { it.postedAt }

            if (latestActiveMessage == null) {
                // Fallback to active key directly if DB message is missing
                val fallbackKey = activeKeys.first()
                val proposal = toolProposalMapper.mapActiveNotificationReply(fallbackKey, replyText)
                if (proposal is VoiceToolProposalResult.Invalid) {
                    _state.value = VoiceAssistantState.Failure(proposal.reason)
                    return@launch
                }
                _state.value = VoiceAssistantState.ConfirmationRequired(
                    PendingVoiceReply(
                        notificationKey = fallbackKey,
                        replyText = replyText,
                        conversationTitle = "Latest Conversation"
                    )
                )
            } else {
                val proposal = toolProposalMapper.mapActiveNotificationReply(latestActiveMessage.notificationKey, replyText)
                if (proposal is VoiceToolProposalResult.Invalid) {
                    _state.value = VoiceAssistantState.Failure(proposal.reason)
                    return@launch
                }
                _state.value = VoiceAssistantState.ConfirmationRequired(
                    PendingVoiceReply(
                        notificationKey = latestActiveMessage.notificationKey,
                        replyText = replyText,
                        conversationTitle = latestActiveMessage.conversationId
                    )
                )
            }
        }
    }

    fun confirmSend(draft: PendingVoiceReply) {
        val context = getApplication<Application>()
        val result = replyExecutor.sendConfirmedReply(context, draft.notificationKey, draft.replyText)
        if (result == ReplySendResult.Success) {
            resetToIdle()
        } else {
            val safeReason = when (result) {
                ReplySendResult.EmptyText -> "Reply text cannot be empty."
                ReplySendResult.NoActiveReplyAction -> "No active WhatsApp notification is available for reply."
                ReplySendResult.NotificationExpired -> "The WhatsApp notification has expired or was cleared."
                ReplySendResult.CanceledBySystem -> "PendingIntent was canceled by the system."
                ReplySendResult.FailedSafely -> "Failed to send reply safely."
            }
            _state.value = VoiceAssistantState.Failure(safeReason)
        }
    }

    fun executeReadAloud() {
        viewModelScope.launch {
            val messages = repository.getAllDecryptedMessages()
                .sortedByDescending { it.postedAt }
                .take(3)

            if (messages.isEmpty()) {
                speakText("There are no captured messages to read.")
                _state.value = VoiceAssistantState.SpeakingMessages(0)
            } else {
                val count = messages.size
                _state.value = VoiceAssistantState.SpeakingMessages(count)
                
                val intro = "You have $count recent WhatsApp message${if (count == 1) "" else "s"}."
                val spokenContent = StringBuilder(intro)
                
                messages.forEachIndexed { index, msg ->
                    val ordinal = when (index) {
                        0 -> "First message"
                        1 -> "Second message"
                        2 -> "Third message"
                        else -> "Next message"
                    }
                    val sender = msg.senderName ?: "Someone"
                    val text = msg.decryptedText ?: "No message content"
                    spokenContent.append(" $ordinal: From $sender. $text.")
                }
                
                speakText(spokenContent.toString())
            }
        }
    }

    fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        _partialTranscript.value = ""
        _amplitude.value = 0
        if (_state.value is VoiceAssistantState.SpeakingMessages) {
            _state.value = VoiceAssistantState.Idle
        }
    }

    private fun speakText(text: String) {
        if (isTtsInitialized) {
            // Register an utterance ID so UtteranceProgressListener triggers on completion
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "GemmaControlTTS")
        } else {
            Log.e(TAG, "TTS not initialized")
        }
    }

    fun requestSystemRecognitionConsent() {
        _state.value = VoiceAssistantState.ConfirmSystemRecognitionConsent
    }

    fun allowSystemRecognitionAndStart() {
        forceSystemRecognition = true
        startListening()
    }

    fun resetToIdle() {
        forceSystemRecognition = false
        _partialTranscript.value = ""
        _amplitude.value = 0
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling SpeechRecognizer", e)
        }
        _state.value = VoiceAssistantState.Idle
    }

    private fun destroySpeechRecognizer() {
        _partialTranscript.value = ""
        _amplitude.value = 0
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying SpeechRecognizer", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        destroySpeechRecognizer()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}

internal fun convertRmsDbToAmplitude(rmsdB: Float): Int {
    val clamped = rmsdB.coerceIn(AUDIO_METER_MIN_DB, AUDIO_METER_MAX_DB)
    return ((clamped - AUDIO_METER_MIN_DB) * 65535f / (AUDIO_METER_MAX_DB - AUDIO_METER_MIN_DB)).toInt()
}
