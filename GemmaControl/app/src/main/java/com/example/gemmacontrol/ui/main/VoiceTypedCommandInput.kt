package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
internal fun VoiceTypedCommandInput(
    state: VoiceAssistantState,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    val canSubmit = canSubmitTypedVoiceCommand(text, state)
    val submit = {
        if (canSubmit) {
            onSubmit(text)
            text = ""
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            enabled = typedVoiceCommandInputEnabled(state),
            singleLine = true,
            label = { Text("Type command") },
            placeholder = { Text("show my latest WhatsApp messages") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
            modifier = Modifier.weight(1f),
            shape = VoiceCardShape
        )
        IconButton(
            onClick = submit,
            enabled = canSubmit
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Run typed command",
                tint = if (canSubmit) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

internal fun canSubmitTypedVoiceCommand(
    text: String,
    state: VoiceAssistantState
): Boolean = text.isNotBlank() && typedVoiceCommandInputEnabled(state)

internal fun typedVoiceCommandInputEnabled(state: VoiceAssistantState): Boolean {
    return when (state) {
        VoiceAssistantState.Idle,
        is VoiceAssistantState.Failure,
        is VoiceAssistantState.LocalToolSucceeded -> true
        else -> false
    }
}
