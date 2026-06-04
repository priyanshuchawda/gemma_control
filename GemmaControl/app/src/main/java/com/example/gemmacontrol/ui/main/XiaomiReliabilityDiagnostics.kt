package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class XiaomiReliabilityLevel {
    Ready,
    Warning,
    Blocked
}

data class XiaomiReliabilityCheck(
    val label: String,
    val ready: Boolean,
    val detail: String
)

data class XiaomiReliabilityDiagnosticState(
    val level: XiaomiReliabilityLevel,
    val title: String,
    val summary: String,
    val latestEventAgeMinutes: Long?,
    val checks: List<XiaomiReliabilityCheck>
)

fun buildXiaomiReliabilityDiagnosticState(
    notificationListenerEnabled: Boolean,
    postNotificationsGranted: Boolean,
    microphoneGranted: Boolean,
    latestEventObservedAt: Long?,
    nowMillis: Long,
    staleAfterMinutes: Long = 120L
): XiaomiReliabilityDiagnosticState {
    val latestEventAgeMinutes = latestEventObservedAt?.let { observedAt ->
        ((nowMillis - observedAt).coerceAtLeast(0L)) / 60_000L
    }
    val eventRecentlyObserved = latestEventAgeMinutes != null && latestEventAgeMinutes <= staleAfterMinutes

    val checks = listOf(
        XiaomiReliabilityCheck(
            label = "Notification Listener Access",
            ready = notificationListenerEnabled,
            detail = if (notificationListenerEnabled) {
                "Enabled"
            } else {
                "Required for WhatsApp capture and active notification replies"
            }
        ),
        XiaomiReliabilityCheck(
            label = "Recent listener event",
            ready = eventRecentlyObserved,
            detail = when (latestEventAgeMinutes) {
                null -> "No WhatsApp listener event captured in this app session"
                0L -> "Latest event arrived less than 1 minute ago"
                else -> "Latest event arrived $latestEventAgeMinutes minutes ago"
            }
        ),
        XiaomiReliabilityCheck(
            label = "Reminder Notifications",
            ready = postNotificationsGranted,
            detail = if (postNotificationsGranted) "Granted" else "Needed for local reminder alerts"
        ),
        XiaomiReliabilityCheck(
            label = "Microphone",
            ready = microphoneGranted,
            detail = if (microphoneGranted) "Granted" else "Needed for voice commands"
        )
    )

    return when {
        !notificationListenerEnabled -> XiaomiReliabilityDiagnosticState(
            level = XiaomiReliabilityLevel.Blocked,
            title = "Notification access disabled",
            summary = "Enable Notification Listener Access so GemmaControl can capture WhatsApp notifications after HyperOS background events.",
            latestEventAgeMinutes = latestEventAgeMinutes,
            checks = checks
        )
        latestEventAgeMinutes == null -> XiaomiReliabilityDiagnosticState(
            level = XiaomiReliabilityLevel.Warning,
            title = "No WhatsApp events captured yet",
            summary = "After enabling Xiaomi settings, send a test WhatsApp message and refresh this card to confirm the listener is alive.",
            latestEventAgeMinutes = latestEventAgeMinutes,
            checks = checks
        )
        latestEventAgeMinutes > staleAfterMinutes -> XiaomiReliabilityDiagnosticState(
            level = XiaomiReliabilityLevel.Warning,
            title = "No recent WhatsApp listener events",
            summary = "HyperOS may have killed or delayed background capture. Toggle notification access off/on, then verify Autostart and Battery No restrictions.",
            latestEventAgeMinutes = latestEventAgeMinutes,
            checks = checks
        )
        !postNotificationsGranted || !microphoneGranted -> XiaomiReliabilityDiagnosticState(
            level = XiaomiReliabilityLevel.Warning,
            title = "Capture ready, optional permissions need attention",
            summary = "WhatsApp capture has recent events, but voice commands or local reminder alerts still need permission setup.",
            latestEventAgeMinutes = latestEventAgeMinutes,
            checks = checks
        )
        else -> XiaomiReliabilityDiagnosticState(
            level = XiaomiReliabilityLevel.Ready,
            title = "Xiaomi background capture looks ready",
            summary = "Notification access is enabled and a recent WhatsApp listener event was observed.",
            latestEventAgeMinutes = latestEventAgeMinutes,
            checks = checks
        )
    }
}

@Composable
fun XiaomiReliabilityDiagnosticsCard(
    state: XiaomiReliabilityDiagnosticState,
    onRefresh: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onOpenAutostart: () -> Unit,
    onOpenBattery: () -> Unit
) {
    val color = when (state.level) {
        XiaomiReliabilityLevel.Ready -> MaterialTheme.colorScheme.primary
        XiaomiReliabilityLevel.Warning -> MaterialTheme.colorScheme.secondary
        XiaomiReliabilityLevel.Blocked -> MaterialTheme.colorScheme.error
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Text(
                    state.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            Text(
                state.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.checks.forEach { check ->
                    ReliabilityCheckRow(check)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh")
                }
                OutlinedButton(
                    onClick = onOpenNotificationAccess,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Notification Access")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenAutostart,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Autostart")
                }
                OutlinedButton(
                    onClick = onOpenBattery,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Battery")
                }
            }
        }
    }
}

@Composable
private fun ReliabilityCheckRow(check: XiaomiReliabilityCheck) {
    val icon = if (check.ready) Icons.Default.CheckCircle else Icons.Default.Warning
    val color = if (check.ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = color
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                check.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                check.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
