package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeDashboardScreen(
    onNavigateToVoice: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as android.app.Application
    val viewModel: HomeDashboardViewModel = viewModel { HomeDashboardViewModel(app) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshDashboard(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDashboard(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (val currentState = state) {
        HomeDashboardScreenState.Loading -> LoadingHomeDashboard(modifier)
        is HomeDashboardReadyState -> {
            HomeDashboardScaffold(
                modifier = modifier,
                state = currentState,
                onPrimaryAction = {
                    when (currentState.summary.heroAction) {
                        HomeHeroAction.OpenVoiceAssistant -> onNavigateToVoice()
                        HomeHeroAction.RequestNotificationAccess -> {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    }
                },
                onNavigateToVoice = onNavigateToVoice,
                onNavigateToInbox = onNavigateToInbox,
                onNavigateToSettings = onNavigateToSettings,
                onClearNotifications = viewModel::clearNotifications,
                onRefreshDashboard = { viewModel.refreshDashboard(context) }
            )
        }
    }
}
