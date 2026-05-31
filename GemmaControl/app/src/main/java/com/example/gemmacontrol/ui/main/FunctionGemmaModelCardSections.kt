package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.ai.model.FunctionGemmaModelDefinition
import com.example.gemmacontrol.ai.model.ModelDownloadStatus
import com.example.gemmacontrol.ai.model.ModelDownloadUiState

internal data class FunctionGemmaAdvancedModelFieldsState(
    val expanded: Boolean,
    val modelUrl: String,
    val modelSha256: String,
    val validationError: String?,
    val downloading: Boolean
)

internal data class FunctionGemmaAdvancedModelFieldActions(
    val onToggleExpanded: () -> Unit,
    val onModelUrlChange: (String) -> Unit,
    val onModelSha256Change: (String) -> Unit,
    val onDownload: () -> Unit
)

@Composable
internal fun FunctionGemmaModelHeader(
    model: FunctionGemmaModelDefinition,
    state: FunctionGemmaModelCardUiState
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = state.statusIcon(),
            contentDescription = state.statusLabel,
            tint = state.statusColor(),
            modifier = Modifier.size(28.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "FunctionGemma Mobile Actions",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                model.modelId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(state)
                Text(
                    formatModelBytes(model.sizeInBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                state.statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun FunctionGemmaModelActions(
    state: FunctionGemmaModelCardUiState,
    sha256: String,
    onPrimaryAction: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onPrimaryAction,
            enabled = state.primaryActionEnabled,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(state.primaryActionLabel)
        }
        OutlinedButton(
            onClick = onCancel,
            enabled = state.showCancelAction,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancel")
        }
    }
    if (state.primaryActionEnabled && sha256.isBlank()) {
        Text(
            "Checksum required for verified download.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun FunctionGemmaAdvancedModelFields(
    state: FunctionGemmaAdvancedModelFieldsState,
    actions: FunctionGemmaAdvancedModelFieldActions
) {
    TextButton(onClick = actions.onToggleExpanded) {
        Text(if (state.expanded) "Hide manual download settings" else "Manual download settings")
    }
    if (!state.expanded) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = state.modelUrl,
            onValueChange = actions.onModelUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Manual model HTTPS URL") },
            singleLine = true,
            enabled = !state.downloading
        )
        OutlinedTextField(
            value = state.modelSha256,
            onValueChange = actions.onModelSha256Change,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SHA-256 checksum") },
            singleLine = true,
            enabled = !state.downloading,
            supportingText = { Text("${state.modelSha256.length} / $ModelSha256HexLength") }
        )
        state.validationError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        OutlinedButton(
            onClick = actions.onDownload,
            enabled = !state.downloading && state.modelUrl.isNotBlank() && state.modelSha256.isNotBlank(),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download manual source")
        }
    }
}

@Composable
internal fun ModelDownloadProgressBlock(
    downloadState: ModelDownloadUiState,
    progressText: String?
) {
    when (downloadState.status) {
        ModelDownloadStatus.Idle -> Unit
        ModelDownloadStatus.Enqueued -> {
            Text(
                "Queued for download",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        ModelDownloadStatus.Running -> {
            if (downloadState.fraction != null) {
                LinearProgressIndicator(
                    progress = { downloadState.fraction },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            progressText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ModelDownloadStatus.Succeeded -> Text(
            "Download verified and installed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        ModelDownloadStatus.Failed -> Text(
            downloadState.errorMessage ?: "Model download failed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        ModelDownloadStatus.Canceled -> Text(
            "Download canceled.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusBadge(state: FunctionGemmaModelCardUiState) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = state.statusColor().copy(alpha = 0.12f)
    ) {
        Text(
            state.statusLabel,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = state.statusColor(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FunctionGemmaModelCardUiState.statusIcon(): ImageVector {
    return when (statusLabel) {
        "Installed" -> Icons.Default.CheckCircle
        "Failed" -> Icons.Default.Warning
        else -> Icons.Default.Info
    }
}

@Composable
private fun FunctionGemmaModelCardUiState.statusColor() = when (statusLabel) {
    "Installed" -> MaterialTheme.colorScheme.primary
    "Failed" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.secondary
}
