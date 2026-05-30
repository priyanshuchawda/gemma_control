package com.example.gemmacontrol.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.gemmacontrol.StoredInbox
import com.example.gemmacontrol.VoiceAssistant
import com.example.gemmacontrol.AppSettings
import androidx.compose.material.icons.filled.Settings
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent

// ─── Color palette ────────────────────────────────────────────────────────────
internal val Green800  = Color(0xFF2E7D32)
internal val Blue800   = Color(0xFF1565C0)
internal val Red800    = Color(0xFFC62828)
internal val Teal700   = Color(0xFF00796B)
internal val Purple700 = Color(0xFF5E35B1)
internal val Orange800 = Color(0xFFE65100)
internal val Grey600   = Color(0xFF757575)

internal val GreenBg  = Color(0xFFE8F5E9)
internal val BlueBg   = Color(0xFFE3F2FD)
internal val RedBg    = Color(0xFFFFEBEE)
internal val TealBg   = Color(0xFFE0F2F1)
internal val PurpleBg = Color(0xFFEDE7F6)
internal val OrangeBg = Color(0xFFFFF3E0)

// ─── Screen root ──────────────────────────────────────────────────────────────
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
            MainScreenUiState.Loading -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            is MainScreenUiState.Success -> {
                MainScreenContent(
                    modifier = Modifier.padding(innerPadding),
                    notifications = currentState.notifications,
                    isPermissionGranted = currentState.isPermissionGranted,
                    onNavigateToVoice = { onItemClick(VoiceAssistant) },
                    onRequestPermission = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onClearNotifications = { viewModel.clearNotifications() },
                    onCheckPermission = { viewModel.checkPermission(context) }
                )
            }
        }
    }
}

// ─── Top App Bar ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemmaTopBar(
    onNavigateToInbox: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "GemmaControl",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "POC",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onNavigateToVoice) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Assistant",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onNavigateToInbox) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Stored Inbox",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopAppBarDefaults.topAppBarColors().containerColor // fallback, using system default
        )
    )
}

// ─── Main content (all in one LazyColumn — fully scrollable) ──────────────────
@Composable
internal fun MainScreenContent(
    modifier: Modifier = Modifier,
    notifications: List<ParsedWhatsAppNotificationEvent>,
    isPermissionGranted: Boolean,
    onNavigateToVoice: () -> Unit,
    onRequestPermission: () -> Unit,
    onClearNotifications: () -> Unit,
    onCheckPermission: () -> Unit,
) {
    val formatter = rememberFormatter()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Permission card
        item(key = "permission") {
            PermissionCard(
                isGranted = isPermissionGranted,
                onRequestPermission = onRequestPermission,
                onCheckPermission = onCheckPermission
            )
        }

        // Voice Assistant Card
        item(key = "voice_assistant") {
            VoiceAssistantHomeCard(onNavigateToVoice = onNavigateToVoice)
        }

        // Privacy notice (collapsed, less dominant)
        item(key = "notice") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "On-device only · No history · Notification content only",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Section header
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Captured Events",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (notifications.isNotEmpty()) {
                        Text(
                            text = "${notifications.size} notification${if (notifications.size == 1) "" else "s"} intercepted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (notifications.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = onClearNotifications,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Empty state
        if (notifications.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Waiting for WhatsApp notifications",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Send a message to this device. Events will appear here in real time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(
                items = notifications,
                key = { "${it.notificationKey}_${it.observedAt}" }
            ) { event ->
                NotificationEventCard(event = event, formatter = formatter)
            }
        }
    }
}

// ─── Permission Card ──────────────────────────────────────────────────────────
@Composable
fun PermissionCard(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    val bg    = if (isGranted) GreenBg   else RedBg
    val fg    = if (isGranted) Green800  else Red800
    val icon  = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning
    val title = if (isGranted) "Listener Active" else "Permission Required"
    val desc  = if (isGranted)
        "Notification access is granted. Incoming WhatsApp events are being captured on-device."
    else
        "Tap Grant to enable Notification Access so GemmaControl can intercept WhatsApp events."

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(28.dp).padding(top = 2.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = fg)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.85f))
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isGranted) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = Red800),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) { Text("Grant", style = MaterialTheme.typography.labelMedium) }
                    }
                    OutlinedButton(
                        onClick = onCheckPermission,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Refresh", style = MaterialTheme.typography.labelMedium, color = fg) }
                }
            }
        }
    }
}

@Composable
fun VoiceAssistantHomeCard(
    onNavigateToVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Voice Assistant",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Speak commands for your recent WhatsApp notifications.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onNavigateToVoice,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Tap to speak", color = Color.White)
            }
        }
    }
}
