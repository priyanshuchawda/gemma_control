package com.example.gemmacontrol.ui.main

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.example.gemmacontrol.data.preferences.VoiceInputMode
import kotlin.coroutines.cancellation.CancellationException

@Composable
internal fun VoiceAssistantMicButton(
    screenState: VoiceAssistantScreenState,
    actions: VoiceAssistantScreenActions,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(MicContainerSize)
    ) {
        if (screenState.state is VoiceAssistantState.Listening) {
            ListeningWaveformBackdrop(
                amplitude = screenState.amplitude,
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
            )
        }

        MicButtonCircle(
            buttonColor = voiceAssistantMicButtonColor(screenState.state),
            voiceInputMode = screenState.voiceInputMode,
            useTapToggle = screenState.state is VoiceAssistantState.SpeakingMessages,
            actions = actions
        )
    }
}

@Composable
private fun voiceAssistantMicButtonColor(state: VoiceAssistantState): Color = when (state) {
    VoiceAssistantState.Listening -> MaterialTheme.colorScheme.error
    is VoiceAssistantState.SpeakingMessages -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
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
    voiceInputMode: VoiceInputMode,
    useTapToggle: Boolean,
    actions: VoiceAssistantScreenActions,
) {
    val inputModifier = when {
        useTapToggle || voiceInputMode == VoiceInputMode.TapToggle -> {
            Modifier.clickable(onClick = actions.onMicClick)
        }
        else -> Modifier.pointerInput(
            actions.onHoldStart,
            actions.onHoldRelease,
            actions.onHoldCancel
        ) {
            detectTapGestures(
                onPress = {
                    actions.onHoldStart()
                    val releaseAction = try {
                        awaitRelease()
                        voiceHoldToSpeakReleaseAction(wasGestureCancelled = false)
                    } catch (e: CancellationException) {
                        voiceHoldToSpeakReleaseAction(wasGestureCancelled = true)
                    }
                    when (releaseAction) {
                        VoiceHoldToSpeakReleaseAction.Finalize -> actions.onHoldRelease()
                        VoiceHoldToSpeakReleaseAction.CancelRecognition -> actions.onHoldCancel()
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
