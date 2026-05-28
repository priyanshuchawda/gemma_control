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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.gemmacontrol.StoredInbox
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.text.SimpleDateFormat
import java.util.*

// ─── Color palette ────────────────────────────────────────────────────────────
private val Green800  = Color(0xFF2E7D32)
private val Blue800   = Color(0xFF1565C0)
private val Red800    = Color(0xFFC62828)
private val Teal700   = Color(0xFF00796B)
private val Purple700 = Color(0xFF5E35B1)
private val Orange800 = Color(0xFFE65100)
private val Grey600   = Color(0xFF757575)

private val GreenBg  = Color(0xFFE8F5E9)
private val BlueBg   = Color(0xFFE3F2FD)
private val RedBg    = Color(0xFFFFEBEE)
private val TealBg   = Color(0xFFE0F2F1)
private val PurpleBg = Color(0xFFEDE7F6)
private val OrangeBg = Color(0xFFFFF3E0)

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
        topBar = { GemmaTopBar(onNavigateToInbox = { onItemClick(StoredInbox) }) }
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
fun GemmaTopBar(onNavigateToInbox: () -> Unit) {
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
            IconButton(onClick = onNavigateToInbox) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Stored Inbox",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

// ─── Main content (all in one LazyColumn — fully scrollable) ──────────────────
@Composable
internal fun MainScreenContent(
    modifier: Modifier = Modifier,
    notifications: List<ParsedWhatsAppNotificationEvent>,
    isPermissionGranted: Boolean,
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

// ─── Event Card ───────────────────────────────────────────────────────────────
@Composable
fun NotificationEventCard(event: ParsedWhatsAppNotificationEvent, formatter: SimpleDateFormat) {
    val observedTime = formatter.format(Date(event.observedAt))
    val safeKey = if (event.notificationKey.length > 8) "…${event.notificationKey.takeLast(8)}" else event.notificationKey

    // Event type styling
    val (evBg, evFg) = when (event.eventType) {
        NotificationEventType.POSTED  -> Pair(GreenBg,  Green800)
        NotificationEventType.UPDATED -> Pair(BlueBg,   Blue800)
        NotificationEventType.REMOVED -> Pair(RedBg,    Red800)
    }

    // Conversation type styling
    val (cvBg, cvFg) = when (event.conversationType) {
        ConversationType.DIRECT  -> Pair(TealBg,   Teal700)
        ConversationType.GROUP   -> Pair(PurpleBg, Purple700)
        ConversationType.UNKNOWN -> Pair(OrangeBg, Orange800)
    }

    val isActive = event.isCurrentlyActive

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ── Row 1: badges + status dot ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge(bg = evBg, fg = evFg, text = event.eventType.name)
                    Badge(bg = cvBg, fg = cvFg, text = event.conversationType.name)
                }
                // Dot + label
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isActive) Green800 else Grey600)
                    )
                    Text(
                        text = if (isActive) "Active" else "Expired",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isActive) Green800 else Grey600
                    )
                }
            }

            // ── Row 2: Chat title ────────────────────────────────────────────
            Text(
                text = event.conversationTitle ?: "[No Title]",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // ── Row 3: Message bubble ────────────────────────────────────────
            if (!event.isContentUnavailable) {
                val latestMsg = event.messages.lastOrNull()
                if (latestMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (latestMsg.senderName != null) {
                                Text(
                                    text = latestMsg.senderName,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(
                                text = latestMsg.messageText ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Content unavailable",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // ── Row 4: Metadata strip ────────────────────────────────────────
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: source + counts
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Chip(label = event.parseSource.name)
                    Text(
                        text = "${event.currentMessageCount} msg · ${event.historicMessageCount} hist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                // Right: times + key
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = observedTime,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = safeKey,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ─── Small reusable composables ───────────────────────────────────────────────
@Composable
private fun Badge(bg: Color, fg: Color, text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}

@Composable
private fun Chip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun rememberFormatter(): SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
