package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceAssistantScaffold(
    screenState: VoiceAssistantScreenState,
    actions: VoiceAssistantScreenActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { VoiceAssistantTopBar(actions.onBack) }
    ) { innerPadding ->
        VoiceAssistantBody(
            screenState = screenState,
            actions = actions,
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
    screenState: VoiceAssistantScreenState,
    actions: VoiceAssistantScreenActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(VoiceScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        VoiceAssistantHeader(screenState)
        VoiceAssistantMicButton(
            screenState = screenState,
            actions = actions
        )
        VoiceTypedCommandInput(
            state = screenState.state,
            onSubmit = actions.onTypedCommandSubmit,
            modifier = Modifier.fillMaxWidth()
        )
        VoiceAssistantActionPanel(
            state = screenState.state,
            actions = actions,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
        )
    }
}
