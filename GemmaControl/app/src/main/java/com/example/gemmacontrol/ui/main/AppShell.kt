package com.example.gemmacontrol.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gemmacontrol.AppDestination

@Composable
fun AppShell(
    modifier: Modifier = Modifier,
    initialDestination: AppDestination = AppDestination.Home
) {
    var selectedDestination by rememberSaveable { mutableStateOf(initialDestination) }

    BackHandler(enabled = selectedDestination != AppDestination.Home) {
        selectedDestination = AppDestination.Home
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            GemmaBottomNavigationBar(
                selectedDestination = selectedDestination,
                onDestinationSelected = { selectedDestination = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedDestination) {
                AppDestination.Home -> HomeDashboardScreen(
                    onNavigateToVoice = { selectedDestination = AppDestination.Voice },
                    onNavigateToInbox = { selectedDestination = AppDestination.Inbox },
                    onNavigateToSettings = { selectedDestination = AppDestination.Settings }
                )
                AppDestination.Voice -> VoiceAssistantScreen(
                    onBack = { selectedDestination = AppDestination.Home }
                )
                AppDestination.Inbox -> StoredInboxScreen(
                    onBack = { selectedDestination = AppDestination.Home }
                )
                AppDestination.Settings -> SettingsScreen(
                    onBack = { selectedDestination = AppDestination.Home }
                )
            }
        }
    }
}

@Composable
private fun GemmaBottomNavigationBar(
    selectedDestination: AppDestination,
    onDestinationSelected: (AppDestination) -> Unit
) {
    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        AppDestination.bottomBarDestinations.forEach { destination ->
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = { onDestinationSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.contentDescription
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

private val AppDestination.icon: ImageVector
    get() = when (this) {
        AppDestination.Home -> Icons.Default.Home
        AppDestination.Voice -> Icons.Default.Mic
        AppDestination.Inbox -> Icons.Default.Lock
        AppDestination.Settings -> Icons.Default.Settings
    }
