package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

// ─── Custom Microphone Icon (Compile-Safe) ──────────────────────────────────
val MicrophoneIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Microphone",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).path(
        fill = null,
        stroke = SolidColor(Color.White),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(12f, 2f)
        curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
        lineTo(9f, 11f)
        curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
        curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
        lineTo(15f, 5f)
        curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
        close()
        moveTo(19f, 10f)
        lineTo(19f, 11f)
        curveTo(19f, 14.87f, 15.87f, 18f, 12f, 18f)
        curveTo(8.13f, 18f, 5f, 14.87f, 5f, 11f)
        lineTo(5f, 10f)
        moveTo(12f, 18f)
        lineTo(12f, 22f)
        moveTo(8f, 22f)
        lineTo(16f, 22f)
    }.build()

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            // Permission denied
        }
    }

    LaunchedEffect(state) {
        if (state is VoiceAssistantState.RequestingMicrophonePermission) {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section - Prompt or Status Description
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val statusText = when (state) {
                    VoiceAssistantState.Idle -> "Speak commands for WhatsApp notifications"
                    VoiceAssistantState.RequestingMicrophonePermission -> "Requesting microphone access..."
                    VoiceAssistantState.Listening -> "Listening... Speak now"
                    is VoiceAssistantState.TranscriptReady -> "Transcribing..."
                    is VoiceAssistantState.CommandReady -> "Command recognized"
                    is VoiceAssistantState.ConfirmationRequired -> "Review Dictated Reply"
                    is VoiceAssistantState.SpeakingMessages -> "Reading latest messages"
                    is VoiceAssistantState.Failure -> "Error encountered"
                    VoiceAssistantState.LanguagePackMissingError -> "Language Pack Missing"
                    VoiceAssistantState.ConfirmSystemRecognitionConsent -> "Allow System Recognition?"
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                val subText = when (state) {
                    VoiceAssistantState.Idle -> "Tap microphone below to start speaking."
                    VoiceAssistantState.Listening -> {
                        val privacyNote = if (isOffline) {
                            "Private on-device speech recognition active."
                        } else {
                            "System recognition active — speech may be processed outside this device."
                        }
                        "$privacyNote\n\nTry: 'Read my latest messages' or 'Reply to the latest message: I am in a meeting'"
                    }
                    is VoiceAssistantState.Failure -> (state as VoiceAssistantState.Failure).safeReason
                    VoiceAssistantState.LanguagePackMissingError -> "Offline language pack unavailable."
                    VoiceAssistantState.ConfirmSystemRecognitionConsent -> "Requires explicit consent to use network-based recognition."
                    else -> ""
                }

                if (subText.isNotEmpty()) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (state is VoiceAssistantState.Listening && partialTranscript.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = partialTranscript,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Middle Section - Interactive Mic Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                val isListening = state is VoiceAssistantState.Listening
                
                // Pulsing animation when listening
                if (isListening) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.4f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                shape = CircleShape
                            )
                            .align(Alignment.Center)
                    )
                }

                val buttonColor = when (state) {
                    VoiceAssistantState.Listening -> MaterialTheme.colorScheme.error
                    is VoiceAssistantState.SpeakingMessages -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(buttonColor)
                        .clickable {
                            when (state) {
                                VoiceAssistantState.Listening -> viewModel.stopListening()
                                is VoiceAssistantState.SpeakingMessages -> viewModel.stopSpeaking()
                                else -> viewModel.startListening()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MicrophoneIcon,
                        contentDescription = "Microphone",
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }
            }

            // Bottom Section - Action card based on state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                when (val s = state) {
                    VoiceAssistantState.Idle -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
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

                    is VoiceAssistantState.CommandReady -> {
                        if (s.command is VoiceCommand.ReadLatestMessages) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.resetToIdle() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel")
                                        }
                                        Button(
                                            onClick = { viewModel.executeReadAloud() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Read Aloud")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is VoiceAssistantState.ConfirmationRequired -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
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

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "To: ${s.draft.conversationTitle}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = s.draft.replyText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetToIdle() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = { viewModel.confirmSend(s.draft) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Confirm Send")
                                    }
                                }
                            }
                        }
                    }

                    is VoiceAssistantState.SpeakingMessages -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
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
                                    onClick = { viewModel.stopSpeaking() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop Speaking", color = Color.White)
                                }
                            }
                        }
                    }

                    is VoiceAssistantState.Failure -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
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
                                    s.safeReason,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { viewModel.resetToIdle() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Okay", color = Color.White)
                                }
                            }
                        }
                    }

                    VoiceAssistantState.LanguagePackMissingError -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
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
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent("android.settings.VOICE_INPUT_SETTINGS"))
                                        } catch (e: Exception) {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                                            } catch (ex: Exception) {
                                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Speech Settings")
                                }
                                Button(
                                    onClick = { viewModel.requestSystemRecognitionConsent() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Allow System Recognition")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetToIdle() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }

                    VoiceAssistantState.ConfirmSystemRecognitionConsent -> {
                        Card(
                            shape = RoundedCornerShape(16.dp),
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.resetToIdle() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = { viewModel.allowSystemRecognitionAndStart() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Continue")
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
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
