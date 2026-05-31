package com.example.gemmacontrol.ui.main

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.text.SimpleDateFormat

private fun LazyListScope.homeDashboardOverviewItems(
    state: HomeDashboardReadyState,
    onPrimaryAction: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefreshDashboard: () -> Unit
) {
    item(key = "hero") {
        HomeHeroCard(summary = state.summary, onPrimaryAction = onPrimaryAction)
    }
    item(key = "stats") {
        HomeDashboardMetricsRow(
            summary = state.summary,
            storedMessageCount = state.storedMessageCount,
            actionableItemCount = state.actionableItemCount
        )
    }
    item(key = "status") {
        ReadinessStatusCard(
            state = state,
            onRefreshDashboard = onRefreshDashboard,
            onNavigateToSettings = onNavigateToSettings
        )
    }
    item(key = "quick_actions") {
        HomeDashboardQuickActionsRow(
            onNavigateToVoice = onNavigateToVoice,
            onNavigateToInbox = onNavigateToInbox
        )
    }
}

private fun LazyListScope.homeDashboardRecentContextItems(
    summary: HomeDashboardSummary,
    notifications: List<ParsedWhatsAppNotificationEvent>,
    formatter: SimpleDateFormat,
    onClearNotifications: () -> Unit
) {
    item(key = "events_header") {
        RecentContextHeader(
            summary = summary,
            hasNotifications = notifications.isNotEmpty(),
            onClearNotifications = onClearNotifications
        )
    }

    if (notifications.isEmpty()) {
        item(key = "empty") {
            EmptyDashboardState(
                title = summary.emptyTitle,
                subtitle = summary.emptySubtitle
            )
        }
    } else {
        items(
            items = notifications.take(RecentNotificationLimit),
            key = { "${it.notificationKey}_${it.observedAt}" }
        ) { event ->
            NotificationEventCard(event = event, formatter = formatter)
        }
    }
}

@Composable
internal fun HomeDashboardContent(
    state: HomeDashboardReadyState,
    onPrimaryAction: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onClearNotifications: () -> Unit,
    onRefreshDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = rememberFormatter()
    val summary = state.summary
    val notifications = state.notifications

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        homeDashboardOverviewItems(
            state = state,
            onPrimaryAction = onPrimaryAction,
            onNavigateToVoice = onNavigateToVoice,
            onNavigateToInbox = onNavigateToInbox,
            onNavigateToSettings = onNavigateToSettings,
            onRefreshDashboard = onRefreshDashboard
        )
        homeDashboardRecentContextItems(
            summary = summary,
            notifications = notifications,
            formatter = formatter,
            onClearNotifications = onClearNotifications
        )
    }
}

@Composable
private fun HomeHeroCard(
    summary: HomeDashboardSummary,
    onPrimaryAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = summary.statusTitle,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = summary.statusSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
            )
            HomeHeroPrimaryButton(summary, onPrimaryAction)
        }
    }
}

@Composable
private fun HomeHeroPrimaryButton(
    summary: HomeDashboardSummary,
    onPrimaryAction: () -> Unit
) {
    Button(
        onClick = onPrimaryAction,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(
            imageVector = if (summary.heroAction == HomeHeroAction.OpenVoiceAssistant) {
                Icons.Default.Mic
            } else {
                Icons.Default.Warning
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(summary.heroActionLabel)
    }
}

@Composable
private fun HomeDashboardMetricsRow(
    summary: HomeDashboardSummary,
    storedMessageCount: Int,
    actionableItemCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DashboardMetricCard(
            title = "Captured",
            value = summary.capturedEventsCount.toString(),
            icon = Icons.Default.Notifications,
            modifier = Modifier.weight(1f)
        )
        DashboardMetricCard(
            title = "Stored",
            value = storedMessageCount.toString(),
            icon = Icons.Default.CheckCircle,
            modifier = Modifier.weight(1f)
        )
        DashboardMetricCard(
            title = "Actionable",
            value = actionableItemCount.toString(),
            icon = Icons.Default.Lock,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HomeDashboardQuickActionsRow(
    onNavigateToVoice: () -> Unit,
    onNavigateToInbox: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            onClick = onNavigateToVoice,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Voice")
        }
        FilledTonalButton(
            onClick = onNavigateToInbox,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Inbox")
        }
    }
}

@Composable
private fun RecentContextHeader(
    summary: HomeDashboardSummary,
    hasNotifications: Boolean,
    onClearNotifications: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Recent WhatsApp context",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = summary.latestConversationTitle ?: "No conversations captured yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (hasNotifications) {
            OutlinedButton(onClick = onClearNotifications) {
                Text("Clear")
            }
        }
    }
}

@Composable
private fun EmptyDashboardState(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private const val RecentNotificationLimit = 5
