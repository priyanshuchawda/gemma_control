package com.example.gemmacontrol.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gemmacontrol.ServiceLocator
import com.example.gemmacontrol.ai.model.FunctionGemmaModelCatalog
import com.example.gemmacontrol.ai.model.ModelDownloadManager
import com.example.gemmacontrol.ai.model.ModelDownloadRequest
import com.example.gemmacontrol.ai.model.ModelDownloadUiStateMapper
import com.example.gemmacontrol.ai.model.ModelDownloadFiles
import com.example.gemmacontrol.data.preferences.VoiceInputMode
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val preferencesRepository = remember(context.applicationContext) {
        ServiceLocator.getPreferencesRepository(context.applicationContext)
    }
    val modelDownloadManager = remember { ModelDownloadManager() }
    val mobileActionsModel = FunctionGemmaModelCatalog.MobileActions
    val modelWorkInfoFlow = remember(context.applicationContext, mobileActionsModel.fileName) {
        modelDownloadManager.observe(context.applicationContext, mobileActionsModel.fileName)
    }
    val modelWorkInfos = modelWorkInfoFlow
        .collectAsStateWithLifecycle(initialValue = emptyList())
        .value
    val modelDownloadState = ModelDownloadUiStateMapper.map(modelWorkInfos.firstOrNull())
    val expectedModelFile = remember(context.applicationContext) {
        File(
            File(context.applicationContext.filesDir, ModelDownloadFiles.MODEL_DIRECTORY),
            mobileActionsModel.fileName
        )
    }
    val isModelInstalled = remember { mutableStateOf(expectedModelFile.isFile && expectedModelFile.length() > 0L) }
    LaunchedEffect(modelDownloadState.status) {
        isModelInstalled.value = expectedModelFile.isFile && expectedModelFile.length() > 0L
    }
    val voiceInputMode = preferencesRepository.voiceInputModeFlow.collectAsStateWithLifecycle(
        initialValue = VoiceInputMode.TapToggle
    ).value
    val coroutineScope = rememberCoroutineScope()
    val reminderNotificationsGranted = remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        reminderNotificationsGranted.value = granted
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            if (Build.VERSION.SDK_INT >= 33) {
                PermissionLinkCard(
                    emoji = "⏰",
                    title = "Reminder Notifications",
                    description = if (reminderNotificationsGranted.value) {
                        "Enabled for local reminder alerts. Reminder notes stay encrypted in the local database until the reminder worker runs."
                    } else {
                        "Required only for user-confirmed local reminders. WhatsApp capture and model prompts do not use this permission."
                    },
                    buttonLabel = if (reminderNotificationsGranted.value) {
                        "Reminder Notifications Enabled"
                    } else {
                        "Grant Reminder Notifications →"
                    },
                    enabled = !reminderNotificationsGranted.value,
                    onAction = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            SectionHeader("Voice Assistant")

            VoiceInputModeCard(
                voiceInputMode = voiceInputMode,
                onVoiceInputModeChange = { mode ->
                    coroutineScope.launch {
                        preferencesRepository.setVoiceInputMode(mode)
                    }
                }
            )

            SectionHeader("FunctionGemma Model")

            FunctionGemmaModelDownloadCard(
                model = mobileActionsModel,
                expectedPath = expectedModelFile.absolutePath,
                installed = isModelInstalled.value,
                downloadState = modelDownloadState,
                actions = FunctionGemmaModelDownloadActions(
                    onDownload = { url, sha256 ->
                        ModelDownloadRequest(
                            url = url,
                            fileName = mobileActionsModel.fileName,
                            sha256 = sha256
                        ).also { request ->
                            modelDownloadManager.enqueue(context.applicationContext, request)
                        }
                    },
                    onCancel = {
                        modelDownloadManager.cancel(context.applicationContext, mobileActionsModel.fileName)
                    }
                )
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
