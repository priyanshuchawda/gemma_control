package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.gemmacontrol.AppSettings
import com.example.gemmacontrol.StoredInbox
import com.example.gemmacontrol.VoiceAssistant

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel() },
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.checkPermission(context) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkPermission(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GemmaTopBar(
                onNavigateToInbox = { onItemClick(StoredInbox) },
                onNavigateToVoice = { onItemClick(VoiceAssistant) },
                onNavigateToSettings = { onItemClick(AppSettings) }
            )
        }
    ) { innerPadding ->
        when (val currentState = state) {
            MainScreenUiState.Loading -> MainScreenLoading(Modifier.padding(innerPadding))
            is MainScreenUiState.Success -> MainScreenContent(
                modifier = Modifier.padding(innerPadding),
                notifications = currentState.notifications,
                isPermissionGranted = currentState.isPermissionGranted,
                onNavigateToVoice = { onItemClick(VoiceAssistant) },
                onRequestPermission = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                onClearNotifications = viewModel::clearNotifications,
                onCheckPermission = { viewModel.checkPermission(context) }
            )
        }
    }
}

@Composable
private fun MainScreenLoading(modifier: Modifier) {
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
