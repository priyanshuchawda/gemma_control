package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal val voiceQuickCommandExamples = listOf(
    "Read my latest WhatsApp messages",
    "Read my latest stored messages",
    "Search WhatsApp for payment",
    "Show pending follow ups",
    "Summarize WhatsApp",
    "Read messages from Mom",
    "Reply to the latest message: I am in a meeting"
)

@Composable
internal fun VoiceCommandExamplesCard(
    onCommandSelected: (String) -> Unit
) {
    Card(
        shape = VoiceCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Quick commands",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                voiceQuickCommandExamples.forEach { command ->
                    CommandExample(
                        command = command,
                        onClick = { onCommandSelected(command) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandExample(
    command: String,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(command) },
        modifier = Modifier.fillMaxWidth()
    )
}
