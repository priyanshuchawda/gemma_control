package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun LoadingHomeDashboard(modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeDashboardScaffold(
    state: HomeDashboardReadyState,
    onPrimaryAction: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onClearNotifications: () -> Unit,
    onRefreshDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { HomeDashboardTopBar(onNavigateToSettings) }
    ) { innerPadding ->
        HomeDashboardContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onPrimaryAction = onPrimaryAction,
            onNavigateToVoice = onNavigateToVoice,
            onNavigateToInbox = onNavigateToInbox,
            onNavigateToSettings = onNavigateToSettings,
            onClearNotifications = onClearNotifications,
            onRefreshDashboard = onRefreshDashboard
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeDashboardTopBar(onNavigateToSettings: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "GemmaControl",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    text = "Private WhatsApp actions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
