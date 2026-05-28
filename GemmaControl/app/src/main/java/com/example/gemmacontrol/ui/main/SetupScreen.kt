package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val DarkBg       = Color(0xFF0D1117)
private val CardBg       = Color(0xFF161B22)
private val CardBorder   = Color(0xFF30363D)
private val AccentGreen  = Color(0xFF3FB950)
private val AccentBlue   = Color(0xFF58A6FF)
private val AccentOrange = Color(0xFFF0883E)
private val AccentRed    = Color(0xFFF85149)
private val TextPrimary  = Color(0xFFE6EDF3)
private val TextMuted    = Color(0xFF8B949E)

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showWarningDialog by remember { mutableStateOf(false) }

    // Refresh on every resume (user might have toggled settings and come back)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.refreshPermissions(context) }

    // Auto-proceed once reliability is complete
    LaunchedEffect(uiState) {
        if (uiState.reliabilitySetupComplete) {
            onSetupComplete()
        }
    }

    val totalSteps = if (uiState.isXiaomiLikeDevice) 3 else 2
    val displayDoneCount = listOf(
        uiState.notificationAccessEnabled,
        uiState.batteryOptimizationIgnored,
        uiState.xiaomiAutostartAcknowledged
    ).take(totalSteps).count { it }

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
            // Header
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

            // Progress indicator
            ProgressBadge(done = displayDoneCount, total = totalSteps)

            // Step 1 – Notification Listener (Core Requirement)
            SetupStepCard(
                step = 1,
                icon = Icons.Default.Notifications,
                title = "Grant Notification Access",
                description = "Allows GemmaControl to read WhatsApp notifications on-device. Nothing is sent off your phone.",
                isGranted = uiState.notificationAccessEnabled,
                buttonLabel = "Open Notification Access",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            // Step 2 – Battery Settings
            SetupStepCard(
                step = 2,
                icon = Icons.Default.Battery5Bar,
                title = "Configure Battery Settings",
                description = "On the tested Redmi 13 5G, notification capture resumed after allowing unrestricted battery usage and enabling Autostart. Xiaomi/HyperOS background controls may affect reliability.",
                isGranted = uiState.batteryOptimizationIgnored,
                buttonLabel = "Open Battery Settings",
                onAction = {
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
            )

            // Step 3 – MIUI Autostart (Xiaomi-specific)
            if (uiState.isXiaomiLikeDevice) {
                MiuiAutostartCard(
                    acknowledged = uiState.xiaomiAutostartAcknowledged,
                    onAcknowledge = { viewModel.acknowledgeAutostart(it) }
                )
            }

            // Status message if complete
            if (uiState.reliabilitySetupComplete) {
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

            // Refresh + Continue / Continue anyway buttons
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.refreshPermissions(context) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh Status", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onSetupComplete,
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
                        Text(
                            if (uiState.reliabilitySetupComplete) "All Set!" else "Continue",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // If Notification Access is enabled but reliability setup is incomplete, show "Continue anyway"
                if (uiState.notificationAccessEnabled && !uiState.reliabilitySetupComplete) {
                    TextButton(
                        onClick = { showWarningDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = AccentOrange)
                    ) {
                        Text("Continue anyway", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // Warning Dialog for Continuing anyway
    if (showWarningDialog) {
        AlertDialog(
            onDismissRequest = { showWarningDialog = false },
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
                    onClick = {
                        showWarningDialog = false
                        onSetupComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Continue Anyway", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarningDialog = false }) {
                    Text("Go Back", color = TextMuted)
                }
            },
            containerColor = CardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Progress badge ────────────────────────────────────────────────────────────
@Composable
private fun ProgressBadge(done: Int, total: Int) {
    val color = when (done) {
        total -> AccentGreen
        0 -> AccentRed
        else -> AccentOrange
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
    ) {
        Text(
            "$done / $total steps configured",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

// ── Generic setup step card ───────────────────────────────────────────────────
@Composable
private fun SetupStepCard(
    step: Int,
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonLabel: String,
    onAction: () -> Unit
) {
    val accentColor = if (isGranted) AccentGreen else AccentBlue
    val borderColor = if (isGranted) AccentGreen.copy(alpha = 0.4f) else CardBorder

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Step $step",
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                if (isGranted) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = AccentGreen, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = "Required", tint = AccentOrange, modifier = Modifier.size(24.dp))
                }
            }

            Text(description, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)

            if (!isGranted) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonLabel, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AccentGreen.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✓  Configured",
                        fontSize = 13.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── MIUI Autostart guidance card ──────────────────────────────────────────────
@Composable
private fun MiuiAutostartCard(
    acknowledged: Boolean,
    onAcknowledge: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val accentColor = if (acknowledged) AccentGreen else AccentOrange
    val borderColor = if (acknowledged) AccentGreen.copy(alpha = 0.4f) else AccentOrange.copy(alpha = 0.4f)

    // Try to detect a running MIUI security app component for the autostart shortcut
    val miuiAutostartIntent = remember {
        val intent = android.content.Intent()
        intent.setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        intent
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MI", color = accentColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Step 3 · XIAOMI / MIUI",
                        fontSize = 11.sp,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text("Enable Autostart", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                if (acknowledged) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Confirmed manually", tint = AccentGreen, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Warning, contentDescription = "Unacknowledged", tint = AccentOrange, modifier = Modifier.size(24.dp))
                }
            }

            Text(
                "MIUI aggressively kills background services. You must whitelist GemmaControl in Security Settings → Autostart, otherwise notification capture will fail in the background.",
                fontSize = 13.sp,
                color = TextMuted,
                lineHeight = 18.sp
            )

            // Step-by-step instructions
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "1. Open  Security  (or  Phone Manager)  app",
                    "2. Tap  Manage apps  →  Permissions",
                    "3. Tap  Autostart",
                    "4. Find  GemmaControl  and toggle it  ON",
                    "5. Also disable Battery Saver for GemmaControl"
                ).forEach { step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("›", color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(step, fontSize = 13.sp, color = TextMuted, lineHeight = 18.sp)
                    }
                }
            }

            Button(
                onClick = {
                    try {
                        context.startActivity(miuiAutostartIntent)
                    } catch (e: Exception) {
                        // Fallback: open App Info where user can navigate manually
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"))
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Autostart Settings", fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(color = CardBorder)

            Text(
                text = "Autostart status cannot be verified automatically. Confirm only after enabling it manually in Xiaomi settings.",
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 16.sp
            )

            if (!acknowledged) {
                Button(
                    onClick = { onAcknowledge(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I enabled Autostart for GemmaControl", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGreen.copy(alpha = 0.1f))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Confirmed manually",
                        fontSize = 13.sp,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { onAcknowledge(false) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text("Undo", color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
