package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationEventCard(event: ParsedWhatsAppNotificationEvent, formatter: SimpleDateFormat) {
    val observedTime = formatter.format(Date(event.observedAt))
    val safeKey = if (event.notificationKey.length > 8) "…${event.notificationKey.takeLast(8)}" else event.notificationKey

    val (evBg, evFg) = when (event.eventType) {
        NotificationEventType.POSTED -> Pair(GreenBg, Green800)
        NotificationEventType.UPDATED -> Pair(BlueBg, Blue800)
        NotificationEventType.REMOVED -> Pair(RedBg, Red800)
    }

    val (cvBg, cvFg) = when (event.conversationType) {
        ConversationType.DIRECT -> Pair(TealBg, Teal700)
        ConversationType.GROUP -> Pair(PurpleBg, Purple700)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Badge(bg = evBg, fg = evFg, text = event.eventType.name)
                    Badge(bg = cvBg, fg = cvFg, text = event.conversationType.name)
                }
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

            Text(
                text = event.conversationTitle ?: "[No Title]",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!event.isContentUnavailable) {
                event.messages.lastOrNull()?.let { latestMsg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            latestMsg.senderName?.let { senderName ->
                                Text(
                                    text = senderName,
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
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.outline
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Chip(label = event.parseSource.name)
                    Text(
                        text = "${event.currentMessageCount} msg · ${event.historicMessageCount} hist",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
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
