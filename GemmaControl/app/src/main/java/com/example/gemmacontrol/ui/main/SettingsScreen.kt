package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val miuiAutostartIntent = remember {
        android.content.Intent().apply {
            setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
    }

    val miuiBatterySaverIntent = remember {
        android.content.Intent().apply {
            setClassName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HardwareStartManagerActivity"
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & Permissions",
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
        // ── Scrollable column ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Troubleshoot banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "📢 Notifications stopped after reinstall?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "1. Open Notification Access (button below).\n" +
                        "2. Find GemmaControl, toggle it OFF, then ON again.\n" +
                        "This resets the listener service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                }
            }

            SectionHeader("Core Permissions")

            // 1. Notification Listener
            PermissionLinkCard(
                emoji = "🔔",
                title = "Notification Listener Access",
                description = "Required for GemmaControl to read WhatsApp notifications. Toggle OFF & ON to refresh after reinstall.",
                buttonLabel = "Open Notification Access →",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            // 2. Microphone permission (direct App Info where RECORD_AUDIO is visible)
            PermissionLinkCard(
                emoji = "🎙️",
                title = "Microphone Permission",
                description = "Required for the Voice Assistant. Grant via App Info → Permissions → Microphone.",
                buttonLabel = "Open App Permissions →",
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
                    }
                }
            )

            SectionHeader("Xiaomi / HyperOS Background Settings")

            // 3. Autostart
            PermissionLinkCard(
                emoji = "🚀",
                title = "Autostart",
                description = "Allow GemmaControl to start in background on boot. Required on Xiaomi / HyperOS / MIUI devices for reliable capture.",
                buttonLabel = "Open Autostart Settings →",
                onAction = {
                    try {
                        context.startActivity(miuiAutostartIntent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                }
            )

            // 4. Battery unrestricted – try MIUI-specific then standard fallback
            PermissionLinkCard(
                emoji = "🔋",
                title = "Battery – No Restrictions",
                description = "Set GemmaControl to 'No restrictions' in battery settings so it is not killed in the background by HyperOS.",
                buttonLabel = "Open Battery Settings →",
                onAction = {
                    try {
                        // Try MIUI-specific battery power saving page first
                        context.startActivity(miuiBatterySaverIntent)
                    } catch (e1: Exception) {
                        try {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (e2: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            )

            SectionHeader("Other System Settings")

            // 5. App Info (general)
            PermissionLinkCard(
                emoji = "ℹ️",
                title = "App Info",
                description = "View all permissions, storage, usage, and other app details for GemmaControl.",
                buttonLabel = "Open App Info →",
                onAction = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )

            // 6. Speech recognition / voice data
            PermissionLinkCard(
                emoji = "🗣️",
                title = "Speech Recognition Language",
                description = "If on-device voice recognition fails (error 13), open Gboard / Google app → Voice Search → Offline speech and download English.",
                buttonLabel = "Open Language & Input Settings →",
                onAction = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            )

            // Bottom padding for scroll breathing room
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PermissionLinkCard(
    emoji: String,
    title: String,
    description: String,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
