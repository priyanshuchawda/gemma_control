package com.example.gemmacontrol.ui.main

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VoiceAssistantScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: VoiceAssistantViewModel = viewModel { VoiceAssistantViewModel(app) }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val voiceInputMode by viewModel.voiceInputMode.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()

    val screenState = VoiceAssistantScreenState(
        state = state,
        isOffline = isOffline,
        voiceInputMode = voiceInputMode,
        partialTranscript = partialTranscript,
        amplitude = amplitude
    )

    MicrophonePermissionEffect(
        state = state,
        onPermissionGranted = viewModel::startListening
    )
    DisposeVoiceAssistantEffect(onDisposeCallback = viewModel::stopSpeaking)
    VoiceAssistantScaffold(
        screenState = screenState,
        actions = voiceAssistantScreenActions(
            context = context,
            viewModel = viewModel,
            state = state,
            onBack = onBack
        ),
        modifier = modifier
    )
}

private fun voiceAssistantScreenActions(
    context: Context,
    viewModel: VoiceAssistantViewModel,
    state: VoiceAssistantState,
    onBack: () -> Unit,
) = VoiceAssistantScreenActions(
    onBack = onBack,
    onMicClick = {
        when (state) {
            VoiceAssistantState.Listening -> viewModel.stopListening()
            is VoiceAssistantState.SpeakingMessages -> viewModel.stopSpeaking()
            else -> viewModel.startListening()
        }
    },
    onHoldStart = viewModel::startListening,
    onHoldRelease = viewModel::stopListeningAfterHold,
    onHoldCancel = viewModel::cancelListening,
    onCancel = viewModel::resetToIdle,
    onReadAloud = viewModel::executeReadAloud,
    onConfirmSend = viewModel::confirmSend,
    onConfirmLocalTool = viewModel::confirmLocalTool,
    onStopSpeaking = viewModel::stopSpeaking,
    onStopResponse = viewModel::stopResponse,
    onOpenSpeechSettings = { context.openSpeechSettings() },
    onAllowSystemRecognition = viewModel::requestSystemRecognitionConsent,
    onContinueSystemRecognition = viewModel::allowSystemRecognitionAndStart
)

@Composable
private fun MicrophonePermissionEffect(
    state: VoiceAssistantState,
    onPermissionGranted: () -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        }
    }

    LaunchedEffect(state) {
        if (state is VoiceAssistantState.RequestingMicrophonePermission) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable
private fun DisposeVoiceAssistantEffect(onDisposeCallback: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose { onDisposeCallback() }
    }
}
