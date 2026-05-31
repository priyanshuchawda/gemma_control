package com.example.gemmacontrol.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SetupScreenActions(
    val onRefresh: () -> Unit,
    val onContinue: () -> Unit,
    val onContinueAnyway: () -> Unit
)

data class SetupPermissionActions(
    val onOpenNotificationAccess: () -> Unit,
    val onOpenBatterySettings: () -> Unit,
    val onAcknowledgeAutostart: (Boolean) -> Unit
)

data class SetupScreenContentState(
    val uiState: SetupUiState,
    val totalSteps: Int,
    val displayDoneCount: Int
)

@Composable
internal fun SetupScreenContent(
    modifier: Modifier,
    state: SetupScreenContentState,
    setupActions: SetupScreenActions,
    permissionActions: SetupPermissionActions
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 56.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SetupHeader()
            SetupStepList(
                uiState = state.uiState,
                totalSteps = state.totalSteps,
                displayDoneCount = state.displayDoneCount,
                actions = permissionActions
            )
            if (state.uiState.reliabilitySetupComplete) {
                SetupReadyStatus()
            }
            Spacer(Modifier.height(4.dp))
            SetupCompletionActions(uiState = state.uiState, actions = setupActions)
        }
    }
}

@Composable
private fun SetupStepList(
    uiState: SetupUiState,
    totalSteps: Int,
    displayDoneCount: Int,
    actions: SetupPermissionActions
) {
    ProgressBadge(done = displayDoneCount, total = totalSteps)
    SetupStepCard(
        state = SetupStepCardState(
            step = 1,
            icon = Icons.Default.Notifications,
            title = "Grant Notification Access",
            description = "Allows GemmaControl to read WhatsApp notifications on-device. Nothing is sent off your phone.",
            isGranted = uiState.notificationAccessEnabled,
            buttonLabel = "Open Notification Access"
        ),
        onAction = actions.onOpenNotificationAccess
    )
    SetupStepCard(
        state = SetupStepCardState(
            step = 2,
            icon = Icons.Default.Battery5Bar,
            title = "Configure Battery Settings",
            description = "On the tested Redmi 13 5G, notification capture resumed after allowing unrestricted battery usage and enabling Autostart. Xiaomi/HyperOS background controls may affect reliability.",
            isGranted = uiState.batteryOptimizationIgnored,
            buttonLabel = "Open Battery Settings"
        ),
        onAction = actions.onOpenBatterySettings
    )
    if (uiState.isXiaomiLikeDevice) {
        MiuiAutostartCard(
            acknowledged = uiState.xiaomiAutostartAcknowledged,
            onAcknowledge = actions.onAcknowledgeAutostart
        )
    }
}

@Composable
internal fun SetupHeader() {
    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AccentBlue.copy(alpha = 0.3f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(40.dp)
                )
            }
            Text(
                "GemmaControl Setup",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "Configure notification access and background reliability settings to enable capture.",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
internal fun SetupReadyStatus() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AccentGreen.copy(alpha = 0.15f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Text(
            text = "Ready for reliable background capture on this tested device configuration.",
            color = AccentGreen,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
internal fun SetupCompletionActions(
    uiState: SetupUiState,
    actions: SetupScreenActions
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SetupPrimaryActionRow(uiState = uiState, actions = actions)
        SetupContinueAnywayAction(uiState = uiState, onContinueAnyway = actions.onContinueAnyway)
    }
}

@Composable
private fun SetupPrimaryActionRow(
    uiState: SetupUiState,
    actions: SetupScreenActions
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = actions.onRefresh,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
            border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Refresh Status", fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = actions.onContinue,
            enabled = uiState.reliabilitySetupComplete,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.reliabilitySetupComplete) AccentGreen else AccentBlue,
                disabledContainerColor = CardBorder
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                if (uiState.reliabilitySetupComplete) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(if (uiState.reliabilitySetupComplete) "All Set!" else "Continue", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SetupContinueAnywayAction(
    uiState: SetupUiState,
    onContinueAnyway: () -> Unit
) {
    if (uiState.notificationAccessEnabled && !uiState.reliabilitySetupComplete) {
        TextButton(
            onClick = onContinueAnyway,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = AccentOrange)
        ) {
            Text("Continue anyway", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
internal fun SetupWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unreliable Capture Warning", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Background notification capture may be unreliable until battery and Autostart settings are configured.",
                color = TextMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text("Continue Anyway", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Go Back", color = TextMuted)
            }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(16.dp)
    )
}
