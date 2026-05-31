package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ReadinessStatusCard(
    state: HomeDashboardReadyState,
    onRefreshDashboard: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val isHealthy = state.summary.heroAction == HomeHeroAction.OpenVoiceAssistant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isHealthy) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        }
    ) {
        Column {
            NotificationListenerStatusRow(
                isHealthy = isHealthy,
                onRefreshDashboard = onRefreshDashboard
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            FunctionGemmaReadinessRow(
                state = state,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun NotificationListenerStatusRow(
    isHealthy: Boolean,
    onRefreshDashboard: () -> Unit
) {
    ReadinessStatusRow(
        icon = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
        iconTint = if (isHealthy) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        title = if (isHealthy) "Notification listener active" else "Notification access required",
        subtitle = if (isHealthy) {
            "Capture, storage, and voice actions can use the latest local WhatsApp context."
        } else {
            "Grant access from the hero action, then refresh this card."
        },
        trailing = {
            OutlinedButton(onClick = onRefreshDashboard) {
                Text("Refresh")
            }
        }
    )
}

@Composable
private fun FunctionGemmaReadinessRow(
    state: HomeDashboardReadyState,
    onNavigateToSettings: () -> Unit
) {
    ReadinessStatusRow(
        icon = Icons.Default.Mic,
        iconTint = if (state.modelReadiness == HomeModelReadiness.Ready) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
        title = "FunctionGemma model: ${state.modelReadinessLabel}",
        subtitle = state.modelReadiness.description,
        trailing = {
            if (state.modelReadiness == HomeModelReadiness.Missing) {
                TextButton(onClick = onNavigateToSettings) {
                    Text("Settings")
                }
            }
        }
    )
}

@Composable
private fun ReadinessStatusRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailing()
    }
}
