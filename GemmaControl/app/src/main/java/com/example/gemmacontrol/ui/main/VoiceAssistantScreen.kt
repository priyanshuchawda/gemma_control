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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
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

private val VoiceCardShape = RoundedCornerShape(8.dp)
private val VoiceScreenPadding = 24.dp
private val MicContainerSize = 200.dp
private val MicButtonSize = 100.dp
private val MicIconSize = 40.dp
private const val PulseDurationMillis = 1000

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
        topBar = {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(VoiceScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            VoiceAssistantHeader(
                state = state,
                isOffline = isOffline,
                voiceInputMode = voiceInputMode,
                partialTranscript = partialTranscript
            )
            VoiceAssistantMicButton(
                state = state,
                voiceInputMode = voiceInputMode,
                amplitude = amplitude,
                onClick = onMicClick,
                onHoldStart = onHoldStart,
                onHoldRelease = onHoldRelease,
                onHoldCancel = onHoldCancel
            )
            VoiceAssistantActionPanel(
                state = state,
                onCancel = onCancel,
                onReadAloud = onReadAloud,
                onConfirmSend = onConfirmSend,
                onStopSpeaking = onStopSpeaking,
                onStopResponse = onStopResponse,
                onOpenSpeechSettings = onOpenSpeechSettings,
                onAllowSystemRecognition = onAllowSystemRecognition,
                onContinueSystemRecognition = onContinueSystemRecognition,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )
        }
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
private fun VoiceAssistantActionPanel(
    state: VoiceAssistantState,
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
    onStopSpeaking: () -> Unit,
    onStopResponse: () -> Unit,
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onContinueSystemRecognition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        when (state) {
            VoiceAssistantState.Idle -> VoiceCommandExamplesCard()
            is VoiceAssistantState.CommandReady -> {
                if (state.command is VoiceCommand.ReadLatestMessages) {
                    ReadLatestConfirmationCard(
                        onCancel = onCancel,
                        onReadAloud = onReadAloud
                    )
                }
            }
            is VoiceAssistantState.ConfirmationRequired -> VoiceReplyConfirmationCard(
                draft = state.draft,
                onCancel = onCancel,
                onConfirmSend = onConfirmSend
            )
            is VoiceAssistantState.Streaming -> StreamingResponseCard(
                partialText = state.partialText,
                onStopResponse = onStopResponse
            )
            is VoiceAssistantState.SpeakingMessages -> SpeakingMessagesCard(onStopSpeaking)
            is VoiceAssistantState.Failure -> VoiceFailureCard(
                reason = state.safeReason,
                onDismiss = onCancel
            )
            VoiceAssistantState.LanguagePackMissingError -> LanguagePackMissingCard(
                onOpenSpeechSettings = onOpenSpeechSettings,
                onAllowSystemRecognition = onAllowSystemRecognition,
                onCancel = onCancel
            )
            VoiceAssistantState.ConfirmSystemRecognitionConsent -> SystemRecognitionConsentCard(
                onCancel = onCancel,
                onContinue = onContinueSystemRecognition
            )
            else -> Unit
        }
    }
}

@Composable
private fun VoiceCommandExamplesCard() {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Supported English Commands:",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CommandExample("Read my latest messages")
                CommandExample("Reply to the latest message: I am in a meeting")
            }
        }
    }
}

@Composable
private fun ReadLatestConfirmationCard(
    onCancel: () -> Unit,
    onReadAloud: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                "Read your latest captured WhatsApp messages aloud?",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            TwoButtonRow(
                secondaryText = "Cancel",
                primaryText = "Read Aloud",
                onSecondaryClick = onCancel,
                onPrimaryClick = onReadAloud
            )
        }
    }
}

@Composable
private fun VoiceReplyConfirmationCard(
    draft: PendingVoiceReply,
    onCancel: () -> Unit,
    onConfirmSend: (PendingVoiceReply) -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReplyCardHeader()
            ReplyDraftPreview(draft)
            TwoButtonRow(
                secondaryText = "Cancel",
                primaryText = "Confirm Send",
                onSecondaryClick = onCancel,
                onPrimaryClick = { onConfirmSend(draft) }
            )
        }
    }
}

@Composable
private fun ReplyCardHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Reply to latest active message?",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun ReplyDraftPreview(draft: PendingVoiceReply) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "To: ${draft.conversationTitle}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = VoiceCardShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = draft.replyText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StreamingResponseCard(
    partialText: String,
    onStopResponse: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text(
                partialText.ifBlank { "Waiting for FunctionGemma..." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onStopResponse,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Response")
            }
        }
    }
}

@Composable
private fun SpeakingMessagesCard(onStopSpeaking: () -> Unit) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
            Text(
                "Reading latest messages aloud...",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Button(
                onClick = onStopSpeaking,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Speaking", color = Color.White)
            }
        }
    }
}

@Composable
private fun VoiceFailureCard(
    reason: String,
    onDismiss: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Text(
                reason,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Okay", color = Color.White)
            }
        }
    }
}

@Composable
private fun LanguagePackMissingCard(
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "Offline speech language data is not installed for this language. Download it in speech settings, or explicitly allow system recognition.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            LanguagePackActions(
                onOpenSpeechSettings = onOpenSpeechSettings,
                onAllowSystemRecognition = onAllowSystemRecognition,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun LanguagePackActions(
    onOpenSpeechSettings: () -> Unit,
    onAllowSystemRecognition: () -> Unit,
    onCancel: () -> Unit,
) {
    Button(
        onClick = onOpenSpeechSettings,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Open Speech Settings")
    }
    Button(
        onClick = onAllowSystemRecognition,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Allow System Recognition")
    }
    OutlinedButton(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Cancel")
    }
}

@Composable
private fun SystemRecognitionConsentCard(
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "System speech recognition may use Google services or network processing. Your spoken command may leave the device. Continue for this command?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            TwoButtonRow(
                secondaryText = "Cancel",
                primaryText = "Continue",
                onSecondaryClick = onCancel,
                onPrimaryClick = onContinue
            )
        }
    }
}

@Composable
private fun TwoButtonRow(
    secondaryText: String,
    primaryText: String,
    onSecondaryClick: () -> Unit,
    onPrimaryClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onSecondaryClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(secondaryText)
        }
        Button(
            onClick = onPrimaryClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(primaryText)
        }
    }
}

@Composable
private fun CommandExample(command: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Text(
            text = "\"$command\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
