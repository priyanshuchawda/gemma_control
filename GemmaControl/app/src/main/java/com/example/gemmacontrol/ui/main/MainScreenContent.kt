package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.text.SimpleDateFormat

private fun LazyListScope.mainOverviewItems(
    isPermissionGranted: Boolean,
    onNavigateToVoice: () -> Unit,
    onRequestPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    item(key = "permission") {
        PermissionCard(
            isGranted = isPermissionGranted,
            onRequestPermission = onRequestPermission,
            onCheckPermission = onCheckPermission
        )
    }
    item(key = "voice_assistant") {
        VoiceAssistantHomeCard(onNavigateToVoice = onNavigateToVoice)
    }
    item(key = "notice") {
        PrivacyNotice()
    }
}

private fun LazyListScope.mainNotificationItems(
    notifications: List<ParsedWhatsAppNotificationEvent>,
    formatter: SimpleDateFormat,
    onClearNotifications: () -> Unit
) {
    item(key = "header") {
        EventsHeader(
            notificationCount = notifications.size,
            onClearNotifications = onClearNotifications
        )
    }

    if (notifications.isEmpty()) {
        item(key = "empty") {
            EmptyNotificationsState()
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
        mainOverviewItems(
            isPermissionGranted = isPermissionGranted,
            onNavigateToVoice = onNavigateToVoice,
            onRequestPermission = onRequestPermission,
            onCheckPermission = onCheckPermission
        )
        mainNotificationItems(
            notifications = notifications,
            formatter = formatter,
            onClearNotifications = onClearNotifications
        )
    }
}

@Composable
private fun PrivacyNotice() {
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

@Composable
private fun EventsHeader(
    notificationCount: Int,
    onClearNotifications: () -> Unit
) {
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
            if (notificationCount > 0) {
                Text(
                    text = "$notificationCount notification${if (notificationCount == 1) "" else "s"} intercepted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (notificationCount > 0) {
            ClearNotificationsButton(onClearNotifications)
        }
    }
}

@Composable
private fun ClearNotificationsButton(onClearNotifications: () -> Unit) {
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

@Composable
private fun EmptyNotificationsState() {
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

@Composable
private fun PermissionCard(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    val presentation = permissionCardPresentation(isGranted)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = presentation.backgroundColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                presentation.icon,
                contentDescription = null,
                tint = presentation.foregroundColor,
                modifier = Modifier.size(28.dp).padding(top = 2.dp)
            )
            PermissionCardText(
                presentation = presentation,
                onRequestPermission = onRequestPermission,
                onCheckPermission = onCheckPermission
            )
        }
    }
}

@Composable
private fun PermissionCardText(
    presentation: PermissionCardPresentation,
    onRequestPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            presentation.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = presentation.foregroundColor
        )
        Text(
            presentation.description,
            style = MaterialTheme.typography.bodySmall,
            color = presentation.foregroundColor.copy(alpha = 0.85f)
        )
        Spacer(Modifier.height(4.dp))
        PermissionCardActions(
            presentation = presentation,
            onRequestPermission = onRequestPermission,
            onCheckPermission = onCheckPermission
        )
    }
}

@Composable
private fun PermissionCardActions(
    presentation: PermissionCardPresentation,
    onRequestPermission: () -> Unit,
    onCheckPermission: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (presentation.showGrantAction) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Red800),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Grant", style = MaterialTheme.typography.labelMedium)
            }
        }
        OutlinedButton(
            onClick = onCheckPermission,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                "Refresh",
                style = MaterialTheme.typography.labelMedium,
                color = presentation.foregroundColor
            )
        }
    }
}

private data class PermissionCardPresentation(
    val backgroundColor: Color,
    val foregroundColor: Color,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val showGrantAction: Boolean
)

private fun permissionCardPresentation(isGranted: Boolean): PermissionCardPresentation {
    return if (isGranted) {
        PermissionCardPresentation(
            backgroundColor = GreenBg,
            foregroundColor = Green800,
            icon = Icons.Default.CheckCircle,
            title = "Listener Active",
            description = "Notification access is granted. Incoming WhatsApp events are being captured on-device.",
            showGrantAction = false
        )
    } else {
        PermissionCardPresentation(
            backgroundColor = RedBg,
            foregroundColor = Red800,
            icon = Icons.Default.Warning,
            title = "Permission Required",
            description = "Tap Grant to enable Notification Access so GemmaControl can intercept WhatsApp events.",
            showGrantAction = true
        )
    }
}

@Composable
private fun VoiceAssistantHomeCard(
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
                text = "Speak or type commands for your recent WhatsApp notifications.",
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
                Text("Open Assistant", color = Color.White)
            }
        }
    }
}
