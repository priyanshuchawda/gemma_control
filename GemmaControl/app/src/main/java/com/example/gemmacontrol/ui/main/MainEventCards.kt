package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationEventCard(event: ParsedWhatsAppNotificationEvent, formatter: SimpleDateFormat) {
    val presentation = notificationEventCardPresentation(event, formatter)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            NotificationEventHeader(event = event, presentation = presentation)
            NotificationEventTitle(conversationTitle = event.conversationTitle)
            NotificationEventMessagePreview(event = event)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            NotificationEventFooter(event = event, presentation = presentation)
        }
    }
}

@Composable
fun rememberFormatter(): SimpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
