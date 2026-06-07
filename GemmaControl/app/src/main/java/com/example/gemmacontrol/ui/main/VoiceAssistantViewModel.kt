package com.example.gemmacontrol.ui.main

import android.app.Application
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gemmacontrol.ServiceLocator
import com.example.gemmacontrol.ai.model.FunctionGemmaModelResolver
import com.example.gemmacontrol.ai.model.InstalledFunctionGemmaModel
import com.example.gemmacontrol.ai.runtime.GemmaEngineResult
import com.example.gemmacontrol.ai.tools.AndroidWhatsAppDraftLauncher
import com.example.gemmacontrol.ai.tools.GemmaMessageContext
import com.example.gemmacontrol.ai.tools.GemmaPromptBuilder
import com.example.gemmacontrol.ai.tools.PhoneContextSnapshotBuilder
import com.example.gemmacontrol.ai.tools.ToolExecutionResult
import com.example.gemmacontrol.ai.tools.WhatsAppToolAction
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import com.example.gemmacontrol.ai.tools.WhatsAppLocalToolExecutor
import com.example.gemmacontrol.data.preferences.VoiceInputMode
import com.example.gemmacontrol.notifications.ActiveNotificationReplyExecutor
import com.example.gemmacontrol.notifications.InMemoryActiveReplyActionRegistry
import com.example.gemmacontrol.notifications.RecentOutgoingReplyEchoSuppressor
import com.example.gemmacontrol.notifications.ReplySendResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val AUDIO_METER_MIN_DB = -2.0f
private const val AUDIO_METER_MAX_DB = 100.0f

class VoiceAssistantViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "VoiceAssistantVM"
    }

    private val repository = ServiceLocator.getStoredInboxRepository(application)
    private val preferencesRepository = ServiceLocator.getPreferencesRepository(application)
    private val gemmaModelManager = ServiceLocator.getGemmaModelManager(application)
    private val functionGemmaModelResolver = FunctionGemmaModelResolver(
        filesDir = application.filesDir,
        cacheDir = application.cacheDir
    )
    private val replyExecutor = ActiveNotificationReplyExecutor(InMemoryActiveReplyActionRegistry)
    private val toolProposalMapper = VoiceCommandToolProposalMapper()
    private val functionGemmaProposalHandler = FunctionGemmaVoiceProposalHandler(toolProposalMapper)
    private val localToolExecutor = WhatsAppLocalToolExecutor(
        preferencesRepository = preferencesRepository,
        localDataRepository = repository,
        draftLauncher = AndroidWhatsAppDraftLauncher(application)
    )
    private val gemmaPromptBuilder = GemmaPromptBuilder()
    private val readAloudBuilder = VoiceReadAloudBuilder()
    private val assistantPlanner = AssistantPlanner()
    private val phoneContextSnapshotBuilder = PhoneContextSnapshotBuilder()
    private val whatsAppToolRegistry = WhatsAppToolRegistry.default()
    private val spokenOutputDebugSink = application.voiceSpokenOutputDebugSink()
    private val textToSpeechController = VoiceTextToSpeechController(
        application = application,
        onFinished = ::handleTextToSpeechFinished
    )

    private val _state = MutableStateFlow<VoiceAssistantState>(VoiceAssistantState.Idle)
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private val _isOffline = MutableStateFlow(true)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    val voiceInputMode: StateFlow<VoiceInputMode> = preferencesRepository.voiceInputModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = VoiceInputMode.TapToggle
    )

    private var speechRecognizer: SpeechRecognizer? = null
    private var pendingStopListeningJob: Job? = null
    private var forceSystemRecognition = false
    private var isCurrentRecognizerOnDevice: Boolean? = null
    private var lastReadRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.Latest
    private var readCursorOffset: Int = 0

    private val recognitionCallbacks = VoiceRecognitionCallbacks(
        onReadyForSpeech = { _state.value = VoiceAssistantState.Listening },
        onAmplitudeChanged = { _amplitude.value = it },
        onPartialTranscriptChanged = { _partialTranscript.value = it },
        onTranscriptReady = ::processTranscript,
        onFailure = { _state.value = VoiceAssistantState.Failure(it) },
        onLanguagePackMissing = { _state.value = VoiceAssistantState.LanguagePackMissingError },
        onRecognitionFinished = {
            _partialTranscript.value = ""
            _amplitude.value = 0
        }
    )

    private suspend fun ensureFunctionGemmaReady(): Boolean {
        if (gemmaModelManager.isReady) {
            return true
        }
        return when (val model = functionGemmaModelResolver.resolveMobileActionsModel()) {
            is InstalledFunctionGemmaModel.Ready -> {
                gemmaModelManager.initialize(model.config) == GemmaEngineResult.Ready
            }
            is InstalledFunctionGemmaModel.Missing -> false
        }
    }

    fun startListening() {
        val context = getApplication<Application>()
        pendingStopListeningJob?.cancel()

        if (!context.hasRecordAudioPermission()) {
            _state.value = VoiceAssistantState.RequestingMicrophonePermission
            return
        }

        val useOnDevice = shouldUseOnDeviceRecognition(
            context = context,
            forceSystemRecognition = forceSystemRecognition
        )

        try {
            if (speechRecognizer == null || isCurrentRecognizerOnDevice != useOnDevice) {
                destroySpeechRecognizer()
                speechRecognizer = createVoiceSpeechRecognizer(context, useOnDevice)
                _isOffline.value = useOnDevice
                isCurrentRecognizerOnDevice = useOnDevice
            }

            speechRecognizer?.setRecognitionListener(
                voiceAssistantRecognitionListener(
                    useOnDevice = useOnDevice,
                    callbacks = recognitionCallbacks
                )
            )

            speechRecognizer?.startListening(voiceRecognitionIntent(useOnDevice))
            _state.value = VoiceAssistantState.Listening

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start speech recognizer", e)
            _state.value = VoiceAssistantState.Failure("Failed to start voice recognition: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        stopListeningAfterDelay(delayMillis = voiceRecognitionStopDelayMillis(VoiceInputMode.TapToggle))
    }

    fun stopListeningAfterHold() {
        stopListeningAfterDelay(delayMillis = voiceRecognitionStopDelayMillis(VoiceInputMode.HoldToSpeak))
    }

    private fun stopListeningAfterDelay(delayMillis: Long) {
        pendingStopListeningJob?.cancel()
        pendingStopListeningJob = viewModelScope.launch {
            if (delayMillis > 0L) {
                delay(delayMillis)
            }
            try {
                speechRecognizer?.stopListening()
                _amplitude.value = 0
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
        }
    }

    private fun processTranscript(
        transcript: String,
        source: AssistantInputSource = AssistantInputSource.Voice
    ) {
        _state.value = VoiceAssistantState.TranscriptReady(transcript)
        handleAssistantPlan(assistantPlanner.plan(transcript, source))
    }

    private fun handleAssistantPlan(plan: AssistantPlan) {
        when (plan) {
            is AssistantPlan.ReadCommand -> {
                _state.value = VoiceAssistantState.CommandReady(plan.command)
                // Read commands do not immediately speak; wait for user confirmation.
            }
            is AssistantPlan.ReplyCommand -> {
                _state.value = VoiceAssistantState.CommandReady(plan.command)
                prepareVoiceReply(plan.command)
            }
            is AssistantPlan.NamedReplyCommand -> {
                _state.value = VoiceAssistantState.CommandReady(plan.command)
                prepareNamedReply(plan.command)
            }
            is AssistantPlan.LocalToolCommand -> {
                _state.value = VoiceAssistantState.CommandReady(plan.command)
                prepareLocalToolAction(plan.command)
            }
            is AssistantPlan.AskClarification -> {
                _state.value = VoiceAssistantState.ClarificationRequired(plan.prompt)
            }
            is AssistantPlan.RequestModelProposal -> {
                requestFunctionGemmaProposal(plan.transcript, plan.fallbackState)
            }
        }
    }

    fun submitTypedCommand(commandText: String) {
        val transcript = commandText.trim()
        if (transcript.isEmpty()) {
            return
        }
        pendingStopListeningJob?.cancel()
        _partialTranscript.value = ""
        _amplitude.value = 0
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling SpeechRecognizer before typed command", e)
        }
        processTranscript(transcript, AssistantInputSource.Typed)
    }

    private fun requestFunctionGemmaProposal(transcript: String, fallbackState: VoiceAssistantState) {
        viewModelScope.launch {
            if (!ensureFunctionGemmaReady()) {
                _state.value = fallbackState
                return@launch
            }

            _state.value = VoiceAssistantState.Streaming("")
            val activeReplyKeys = InMemoryActiveReplyActionRegistry.availabilityFlow.value.keys
            val visibleActiveKeys = repository.getActiveNotificationKeys() + activeReplyKeys
            val decryptedMessages = repository.getAllDecryptedMessages()
            val promptContext = decryptedMessages.map { message ->
                GemmaMessageContext.fromDecryptedMessage(
                    messageEventId = message.id,
                    notificationKey = message.notificationKey,
                    conversationName = message.conversationId,
                    senderName = message.senderName,
                    decryptedText = message.decryptedText,
                    postedAt = message.postedAt,
                    priority = message.priority,
                    replyAvailable = message.notificationKey in activeReplyKeys,
                    contentKind = message.contentKind
                )
            }
            val latestActiveNotificationKey = decryptedMessages
                .filter { it.notificationKey in visibleActiveKeys }
                .maxByOrNull { it.postedAt }
                ?.notificationKey
                ?: visibleActiveKeys.firstOrNull()
            val phoneContext = phoneContextSnapshotBuilder.build(
                messages = promptContext,
                activeNotificationKeys = visibleActiveKeys,
                latestActiveNotificationKey = latestActiveNotificationKey
            )
            val prompt = gemmaPromptBuilder.buildForUserCommand(
                userCommand = transcript,
                phoneContext = phoneContext
            )
            val result = gemmaModelManager.generateToolProposal(
                prompt = prompt,
                registry = whatsAppToolRegistry
            ) { partialText ->
                _state.value = VoiceAssistantState.Streaming(partialText)
            }
            val conversationTitleByNotificationKey = decryptedMessages
                .associate { it.notificationKey to it.conversationId }
            _state.value = functionGemmaProposalHandler.resolve(
                result = result,
                context = FunctionGemmaVoiceProposalContext(
                    activeNotificationKeys = activeReplyKeys,
                    latestActiveNotificationKey = latestActiveNotificationKey,
                    conversationTitleByNotificationKey = conversationTitleByNotificationKey
                )
            )
        }
    }

    private fun prepareVoiceReply(command: VoiceCommand.ReplyToLatestActiveMessage) {
        viewModelScope.launch {
            val activeKeys = InMemoryActiveReplyActionRegistry.availabilityFlow.value.keys
            val decryptedMessages = repository.getAllDecryptedMessages()
            val targets = activeReplyTargets(activeKeys, decryptedMessages)

            applyReplyResolution(
                VoiceReplyTargetResolver.resolveLatest(
                    replyText = command.replyText,
                    explicitLatest = command.explicitLatest,
                    targets = targets
                )
            )
        }
    }

    private fun prepareNamedReply(command: VoiceCommand.ReplyToConversation) {
        viewModelScope.launch {
            val activeKeys = InMemoryActiveReplyActionRegistry.availabilityFlow.value.keys
            val decryptedMessages = repository.getAllDecryptedMessages()
            val targets = activeReplyTargets(activeKeys, decryptedMessages)

            applyReplyResolution(
                VoiceReplyTargetResolver.resolveNamed(
                    conversationName = command.conversationName,
                    replyText = command.replyText,
                    targets = targets
                )
            )
        }
    }

    private fun activeReplyTargets(
        activeKeys: Set<String>,
        decryptedMessages: List<com.example.gemmacontrol.data.repository.StoredInboxRepository.DecryptedMessage>
    ): List<ActiveReplyTarget> {
        val latestMessageByKey = decryptedMessages
            .filter { it.notificationKey in activeKeys }
            .groupBy { it.notificationKey }
            .mapValues { (_, messages) -> messages.maxBy { it.postedAt } }

        return activeKeys.map { notificationKey ->
            val message = latestMessageByKey[notificationKey]
            ActiveReplyTarget(
                notificationKey = notificationKey,
                conversationTitle = message?.conversationId ?: "Latest Conversation",
                postedAt = message?.postedAt ?: Long.MIN_VALUE
            )
        }
    }

    private fun applyReplyResolution(resolution: VoiceReplyTargetResolution) {
        when (resolution) {
            is VoiceReplyTargetResolution.Active -> {
                val proposal = toolProposalMapper.mapActiveNotificationReply(
                    notificationKey = resolution.draft.notificationKey,
                    replyText = resolution.draft.replyText
                )
                if (proposal is VoiceToolProposalResult.Invalid) {
                    _state.value = VoiceAssistantState.Failure(proposal.reason)
                    return
                }
                _state.value = VoiceAssistantState.ConfirmationRequired(resolution.draft)
            }
            is VoiceReplyTargetResolution.Draft -> {
                _state.value = functionGemmaProposalHandler.resolve(
                    result = GemmaEngineResult.NativeToolAction(
                        action = WhatsAppToolAction.DraftReply(
                            conversationName = resolution.conversationName,
                            messageText = resolution.replyText
                        )
                    ),
                    context = FunctionGemmaVoiceProposalContext(
                        activeNotificationKeys = InMemoryActiveReplyActionRegistry.availabilityFlow.value.keys
                    )
                )
            }
            is VoiceReplyTargetResolution.Clarification -> {
                _state.value = VoiceAssistantState.ClarificationRequired(resolution.prompt)
            }
        }
    }

    private fun prepareLocalToolAction(command: VoiceCommand.LocalToolAction) {
        _state.value = functionGemmaProposalHandler.resolve(
            result = GemmaEngineResult.NativeToolAction(action = command.action),
            context = FunctionGemmaVoiceProposalContext(
                activeNotificationKeys = InMemoryActiveReplyActionRegistry.availabilityFlow.value.keys
            )
        )
    }

    fun confirmSend(draft: PendingVoiceReply) {
        val context = getApplication<Application>()
        val result = replyExecutor.sendConfirmedReply(context, draft.notificationKey, draft.replyText)
        if (result == ReplySendResult.Success) {
            RecentOutgoingReplyEchoSuppressor.register(
                notificationKey = draft.notificationKey,
                replyText = draft.replyText,
                conversationTitle = draft.conversationTitle
            )
            resetToIdle()
        } else {
            val safeReason = when (result) {
                ReplySendResult.EmptyText -> "Reply text cannot be empty."
                ReplySendResult.NoActiveReplyAction -> NoActiveReplyTargetMessage
                ReplySendResult.NotificationExpired -> ExpiredActiveReplyTargetMessage
                ReplySendResult.CanceledBySystem -> "PendingIntent was canceled by the system."
                ReplySendResult.FailedSafely -> "Failed to send reply safely."
            }
            _state.value = VoiceAssistantState.Failure(safeReason)
        }
    }

    fun confirmLocalTool(action: PendingLocalToolAction) {
        viewModelScope.launch {
            when (val result = localToolExecutor.executeConfirmed(action.decision)) {
                is ToolExecutionResult.Success -> {
                    _state.value = VoiceAssistantState.LocalToolSucceeded(result.message)
                }
                is ToolExecutionResult.Rejected -> {
                    _state.value = VoiceAssistantState.Failure(result.reason)
                }
            }
        }
    }

    fun executeReadAloud() {
        viewModelScope.launch {
            val command = (_state.value as? VoiceAssistantState.CommandReady)
                ?.command as? VoiceReadCommand
                ?: VoiceCommand.ReadLatestMessages
            val requestedRead = command.readRequest
            val isContinuation = requestedRead == VoiceReadAloudRequest.Continue
            val builderRequest = if (isContinuation) lastReadRequest else requestedRead
            val offset = if (isContinuation) readCursorOffset else 0
            val plan = readAloudBuilder.build(
                messages = repository.getAllDecryptedMessages(),
                request = builderRequest,
                continueOffset = offset,
                forceDirect = isContinuation,
                activeNotificationKeys = repository.getActiveNotificationKeys() +
                    InMemoryActiveReplyActionRegistry.availabilityFlow.value.keys
            )

            if (!isContinuation) {
                lastReadRequest = requestedRead.continuationBase()
            }
            readCursorOffset = plan.nextOffset
            spokenOutputDebugSink.record(plan.spokenText)
            _state.value = VoiceAssistantState.SpeakingMessages(plan.spokenMessageCount)
            textToSpeechController.speak(plan.spokenText)
        }
    }

    fun stopSpeaking() {
        textToSpeechController.stop()
        _partialTranscript.value = ""
        _amplitude.value = 0
        if (_state.value is VoiceAssistantState.SpeakingMessages) {
            _state.value = VoiceAssistantState.Idle
        }
    }

    fun stopResponse() {
        gemmaModelManager.stopResponse()
        resetToIdle()
    }

    private fun handleTextToSpeechFinished() {
        if (_state.value is VoiceAssistantState.SpeakingMessages) {
            _state.value = VoiceAssistantState.Idle
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
        pendingStopListeningJob?.cancel()
        _partialTranscript.value = ""
        _amplitude.value = 0
        textToSpeechController.stop()
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling SpeechRecognizer", e)
        }
        _state.value = VoiceAssistantState.Idle
    }

    fun cancelListening() {
        resetToIdle()
    }

    private fun destroySpeechRecognizer() {
        pendingStopListeningJob?.cancel()
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
        gemmaModelManager.release()
        textToSpeechController.shutdown()
    }
}

private fun VoiceReadAloudRequest.continuationBase(): VoiceReadAloudRequest {
    return when (this) {
        VoiceReadAloudRequest.Continue -> VoiceReadAloudRequest.Latest
        VoiceReadAloudRequest.Summarize -> VoiceReadAloudRequest.StoredLatest
        is VoiceReadAloudRequest.ConversationSummary -> VoiceReadAloudRequest.Conversation(conversationName)
        else -> this
    }
}

internal fun convertRmsDbToAmplitude(rmsdB: Float): Int {
    val clamped = rmsdB.coerceIn(AUDIO_METER_MIN_DB, AUDIO_METER_MAX_DB)
    return ((clamped - AUDIO_METER_MIN_DB) * 65535f / (AUDIO_METER_MAX_DB - AUDIO_METER_MIN_DB)).toInt()
}
