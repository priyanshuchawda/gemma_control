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
    val batteryExempt by viewModel.batteryOptExempt.collectAsStateWithLifecycle()
    val listenerEnabled by viewModel.notificationListenerEnabled.collectAsStateWithLifecycle()

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

    // Auto-proceed once both permissions are granted
    LaunchedEffect(batteryExempt, listenerEnabled) {
        if (batteryExempt && listenerEnabled) onSetupComplete()
    }

    val allDone = batteryExempt && listenerEnabled
    val doneCount = listOf(batteryExempt, listenerEnabled).count { it }

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
                        "Grant two permissions so the notification listener can run reliably in the background.",
                        fontSize = 14.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            // Progress indicator
            ProgressBadge(done = doneCount, total = 2)

            // Step 1 – Battery Optimization
            SetupStepCard(
                step = 1,
                icon = Icons.Default.Battery5Bar,
                title = "Disable Battery Optimization",
                description = "Android limits background services to save battery. Allow GemmaControl to run unrestricted so it never misses a notification.",
                isGranted = batteryExempt,
                buttonLabel = "Disable Restriction",
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }
            )

            // Step 2 – Notification Listener
            SetupStepCard(
                step = 2,
                icon = Icons.Default.Notifications,
                title = "Grant Notification Access",
                description = "Allows GemmaControl to read WhatsApp notifications on-device. Nothing is sent off your phone.",
                isGranted = listenerEnabled,
                buttonLabel = "Open Notification Access",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            // Step 3 – MIUI Autostart (informational only — can't request programmatically)
            MiuiAutostartCard()

            // Refresh + Continue button
            Spacer(Modifier.height(4.dp))
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
                    enabled = listenerEnabled, // at minimum need the listener
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allDone) AccentGreen else AccentBlue,
                        disabledContainerColor = CardBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (allDone) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (allDone) "All Set!" else "Continue",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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
            "$done / $total permissions granted",
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
                        "✓  Permission granted",
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
private fun MiuiAutostartCard() {
    // Only show on MIUI / HyperOS devices (detected by manufacturer string)
    val isMiui = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                 Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
                 Build.MANUFACTURER.equals("POCO", ignoreCase = true)
    val context = LocalContext.current

    // Try to detect a running MIUI security app component for the autostart shortcut
    val miuiAutostartIntent = remember {
        val intent = android.content.Intent()
        intent.setClassName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        intent
    }

    if (!isMiui) return  // Samsung/Pixel users don't need this

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentOrange.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AccentOrange.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MI", color = AccentOrange, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "XIAOMI / MIUI",
                        fontSize = 11.sp,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text("Enable Autostart", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(24.dp))
            }

            Text(
                "MIUI aggressively kills background services. You must whitelist GemmaControl in Security Settings → Autostart, otherwise the notification listener will stop after the app is swiped away.",
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
                        Text("›", color = AccentOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        }
    }
}
