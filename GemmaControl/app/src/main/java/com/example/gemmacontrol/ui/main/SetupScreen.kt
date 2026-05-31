package com.example.gemmacontrol.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

internal val DarkBg = Color(0xFF0D1117)
internal val CardBg = Color(0xFF161B22)
internal val CardBorder = Color(0xFF30363D)
internal val AccentGreen = Color(0xFF3FB950)
internal val AccentBlue = Color(0xFF58A6FF)
internal val AccentOrange = Color(0xFFF0883E)
internal val AccentRed = Color(0xFFF85149)
internal val TextPrimary = Color(0xFFE6EDF3)
internal val TextMuted = Color(0xFF8B949E)

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val progressState = uiState.setupProgressState()
    var showWarningDialog by remember { mutableStateOf(false) }

    SetupPermissionRefreshEffect(context = context, viewModel = viewModel)
    SetupAutoCompleteEffect(uiState = uiState, onSetupComplete = onSetupComplete)

    SetupScreenContent(
        modifier = modifier,
        state = SetupScreenContentState(
            uiState = uiState,
            totalSteps = progressState.totalSteps,
            displayDoneCount = progressState.displayDoneCount
        ),
        setupActions = SetupScreenActions(
            onRefresh = { viewModel.refreshPermissions(context) },
            onContinue = onSetupComplete,
            onContinueAnyway = { showWarningDialog = true }
        ),
        permissionActions = SetupPermissionActions(
            onOpenNotificationAccess = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            },
            onOpenBatterySettings = { openBatterySettings(context) },
            onAcknowledgeAutostart = { viewModel.acknowledgeAutostart(it) }
        )
    )

    if (showWarningDialog) {
        SetupWarningDialog(
            onDismiss = { showWarningDialog = false },
            onConfirm = {
                showWarningDialog = false
                onSetupComplete()
            }
        )
    }
}

@Composable
private fun SetupPermissionRefreshEffect(
    context: Context,
    viewModel: SetupViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.refreshPermissions(context) }
}

@Composable
private fun SetupAutoCompleteEffect(
    uiState: SetupUiState,
    onSetupComplete: () -> Unit
) {
    LaunchedEffect(uiState) {
        if (uiState.reliabilitySetupComplete) {
            onSetupComplete()
        }
    }
}

private data class SetupProgressState(
    val totalSteps: Int,
    val displayDoneCount: Int
)

private fun SetupUiState.setupProgressState(): SetupProgressState {
    val totalSteps = if (isXiaomiLikeDevice) 3 else 2
    val displayDoneCount = listOf(
        notificationAccessEnabled,
        batteryOptimizationIgnored,
        xiaomiAutostartAcknowledged
    ).take(totalSteps).count { it }
    return SetupProgressState(totalSteps = totalSteps, displayDoneCount = displayDoneCount)
}

private fun openBatterySettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    } catch (e: Exception) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }
}
