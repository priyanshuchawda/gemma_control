package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gemmacontrol.data.preferences.VoiceInputMode
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
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
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()
    val voiceInputMode by viewModel.voiceInputMode.collectAsStateWithLifecycle()

    MicrophonePermissionEffect(
        state = state,
        onPermissionGranted = viewModel::startListening
    )
    DisposeVoiceAssistantEffect(onDisposeCallback = viewModel::stopSpeaking)
    VoiceAssistantScaffold(
        state = state,
        isOffline = isOffline,
        voiceInputMode = voiceInputMode,
        partialTranscript = partialTranscript,
        amplitude = amplitude,
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
        onContinueSystemRecognition = viewModel::allowSystemRecognitionAndStart,
        modifier = modifier
    )
}

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceAssistantScaffold(
    state: VoiceAssistantState,
    isOffline: Boolean,
    voiceInputMode: VoiceInputMode,
    partialTranscript: String,
    amplitude: Int,
    onBack: () -> Unit,
    onMicClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldRelease: () -> Unit,
    onHoldCancel: () -> Unit,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
    onConfirmLocalTool: (PendingLocalToolAction) -> Unit,
    onStopSpeaking: () -> Unit,
    onStopResponse: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onContinueSystemRecognition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { VoiceAssistantTopBar(onBack) }
    ) { innerPadding ->
        VoiceAssistantBody(
            state = state,
            isOffline = isOffline,
            voiceInputMode = voiceInputMode,
            partialTranscript = partialTranscript,
            amplitude = amplitude,
            onMicClick = onMicClick,
            onHoldStart = onHoldStart,
            onHoldRelease = onHoldRelease,
            onHoldCancel = onHoldCancel,
            onCancel = onCancel,
            onReadAloud = onReadAloud,
            onConfirmSend = onConfirmSend,
            onConfirmLocalTool = onConfirmLocalTool,
            onStopSpeaking = onStopSpeaking,
            onStopResponse = onStopResponse,
            onOpenSpeechSettings = onOpenSpeechSettings,
            onAllowSystemRecognition = onAllowSystemRecognition,
            onContinueSystemRecognition = onContinueSystemRecognition,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceAssistantTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "Voice Assistant",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun VoiceAssistantBody(
    state: VoiceAssistantState,
    isOffline: Boolean,
    voiceInputMode: VoiceInputMode,
    partialTranscript: String,
    amplitude: Int,
    onMicClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldRelease: () -> Unit,
    onHoldCancel: () -> Unit,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
    onConfirmLocalTool: (PendingLocalToolAction) -> Unit,
    onStopSpeaking: () -> Unit,
    onStopResponse: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onContinueSystemRecognition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(VoiceScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        VoiceAssistantHeader(state, isOffline, voiceInputMode, partialTranscript)
        VoiceAssistantMicButton(
            state,
            voiceInputMode,
            amplitude,
            onMicClick,
            onHoldStart,
            onHoldRelease,
            onHoldCancel
        )
        VoiceAssistantActionPanel(
            state = state,
            onCancel = onCancel,
            onReadAloud = onReadAloud,
            onConfirmSend = onConfirmSend,
            onConfirmLocalTool = onConfirmLocalTool,
            onStopSpeaking = onStopSpeaking,
            onStopResponse = onStopResponse,
            onOpenSpeechSettings = onOpenSpeechSettings,
            onAllowSystemRecognition = onAllowSystemRecognition,
            onContinueSystemRecognition = onContinueSystemRecognition,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        )
    }
}

@Composable
private fun VoiceAssistantHeader(
    state: VoiceAssistantState,
    isOffline: Boolean,
    voiceInputMode: VoiceInputMode,
    partialTranscript: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = voiceAssistantStatusTitle(state),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        val subtitle = voiceAssistantSubtitle(state, isOffline, voiceInputMode)
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state is VoiceAssistantState.Listening && partialTranscript.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            PartialTranscriptSurface(partialTranscript)
        }
    }
}

@Composable
private fun PartialTranscriptSurface(text: String) {
    Surface(
        shape = VoiceCardShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun VoiceAssistantMicButton(
    state: VoiceAssistantState,
    voiceInputMode: VoiceInputMode,
    amplitude: Int,
    onClick: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldRelease: () -> Unit,
    onHoldCancel: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(MicContainerSize)
    ) {
        val isListening = state is VoiceAssistantState.Listening
        if (isListening) {
            ListeningWaveformBackdrop(
                amplitude = amplitude,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        }

        val buttonColor = when (state) {
            VoiceAssistantState.Listening -> MaterialTheme.colorScheme.error
            is VoiceAssistantState.SpeakingMessages -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
        MicButtonCircle(
            buttonColor = buttonColor,
            voiceInputMode = voiceInputMode,
            useTapToggle = state is VoiceAssistantState.SpeakingMessages,
            onClick = onClick,
            onHoldStart = onHoldStart,
            onHoldRelease = onHoldRelease,
            onHoldCancel = onHoldCancel
        )
    }
}

@Composable
private fun ListeningWaveformBackdrop(
    amplitude: Int,
    modifier: Modifier = Modifier,
) {
    WaveformAnimation(
        amplitude = amplitude,
        bgColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(PulseDurationMillis, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(MicButtonSize)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = CircleShape
            )
    )
}

@Composable
private fun MicButtonCircle(
    buttonColor: Color,
    onClick: () -> Unit,
    voiceInputMode: VoiceInputMode,
    useTapToggle: Boolean,
    onHoldStart: () -> Unit,
    onHoldRelease: () -> Unit,
    onHoldCancel: () -> Unit,
) {
    val inputModifier = when {
        useTapToggle || voiceInputMode == VoiceInputMode.TapToggle -> Modifier.clickable(onClick = onClick)
        else -> Modifier.pointerInput(onHoldStart, onHoldRelease, onHoldCancel) {
            detectTapGestures(
                onPress = {
                    onHoldStart()
                    val releaseAction = try {
                        awaitRelease()
                        voiceHoldToSpeakReleaseAction(wasGestureCancelled = false)
                    } catch (e: CancellationException) {
                        voiceHoldToSpeakReleaseAction(wasGestureCancelled = true)
                    }
                    when (releaseAction) {
                        VoiceHoldToSpeakReleaseAction.Finalize -> onHoldRelease()
                        VoiceHoldToSpeakReleaseAction.CancelRecognition -> onHoldCancel()
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .size(MicButtonSize)
            .clip(CircleShape)
            .background(buttonColor)
            .then(inputModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            modifier = Modifier.size(MicIconSize),
            tint = Color.White
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VoiceAssistantActionPanel(
    state: VoiceAssistantState,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
    onConfirmLocalTool: (PendingLocalToolAction) -> Unit,
    onStopSpeaking: () -> Unit,
    onStopResponse: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onContinueSystemRecognition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presentation = voiceAssistantActionPresentation(state)

    Box(modifier = modifier) {
        VoiceAssistantInlineActionContent(
            state = state,
            presentation = presentation,
            onStopSpeaking = onStopSpeaking,
            onStopResponse = onStopResponse
        )
    }

    if (presentation == VoiceActionPresentation.BottomSheet) {
        VoiceAssistantActionBottomSheet(
            state = state,
            sheetState = sheetState,
            onCancel = onCancel,
            onReadAloud = onReadAloud,
            onConfirmSend = onConfirmSend,
            onConfirmLocalTool = onConfirmLocalTool,
            onOpenSpeechSettings = onOpenSpeechSettings,
            onAllowSystemRecognition = onAllowSystemRecognition,
            onContinueSystemRecognition = onContinueSystemRecognition
        )
    }
}

@Composable
private fun VoiceAssistantInlineActionContent(
    state: VoiceAssistantState,
    presentation: VoiceActionPresentation,
    onStopSpeaking: () -> Unit,
    onStopResponse: () -> Unit,
) {
    when {
        presentation == VoiceActionPresentation.Inline && state == VoiceAssistantState.Idle -> VoiceCommandExamplesCard()
        presentation == VoiceActionPresentation.Inline && state is VoiceAssistantState.Streaming -> StreamingResponseCard(
            partialText = state.partialText,
            onStopResponse = onStopResponse
        )
        presentation == VoiceActionPresentation.Inline && state is VoiceAssistantState.SpeakingMessages -> {
            SpeakingMessagesCard(onStopSpeaking)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceAssistantActionBottomSheet(
    state: VoiceAssistantState,
    sheetState: SheetState,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
    onConfirmLocalTool: (PendingLocalToolAction) -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onContinueSystemRecognition: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState
    ) {
        VoiceAssistantActionSheetContent(
            state = state,
            onCancel = onCancel,
            onReadAloud = onReadAloud,
            onConfirmSend = onConfirmSend,
            onConfirmLocalTool = onConfirmLocalTool,
            onOpenSpeechSettings = onOpenSpeechSettings,
            onAllowSystemRecognition = onAllowSystemRecognition,
            onContinueSystemRecognition = onContinueSystemRecognition
        )
    }
}

private fun android.content.Context.openSpeechSettings() {
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
