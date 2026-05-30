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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.ai.model.FunctionGemmaModelDefinition
import com.example.gemmacontrol.ai.model.ModelDownloadStatus
import com.example.gemmacontrol.ai.model.ModelDownloadUiState
import java.util.Locale

data class FunctionGemmaModelCardUiState(
    val statusLabel: String,
    val statusDescription: String,
    val primaryActionLabel: String,
    val verifiedDownloadUrl: String,
    val localPath: String,
    val progressText: String?,
    val advancedExpandedByDefault: Boolean,
    val showCancelAction: Boolean,
    val primaryActionEnabled: Boolean
)

fun functionGemmaModelCardState(
    model: FunctionGemmaModelDefinition,
    expectedPath: String,
    installed: Boolean,
    downloadState: ModelDownloadUiState
): FunctionGemmaModelCardUiState {
    val downloading = downloadState.status == ModelDownloadStatus.Enqueued ||
        downloadState.status == ModelDownloadStatus.Running
    val failed = downloadState.status == ModelDownloadStatus.Failed
    val installedOrSucceeded = installed || downloadState.status == ModelDownloadStatus.Succeeded

    return FunctionGemmaModelCardUiState(
        statusLabel = when {
            installedOrSucceeded -> "Installed"
            downloading -> "Downloading"
            failed -> "Failed"
            downloadState.status == ModelDownloadStatus.Canceled -> "Canceled"
            else -> "Missing"
        },
        statusDescription = when {
            installedOrSucceeded -> "${model.name} is installed locally and ready for voice proposals."
            downloading -> "Downloading and verifying the local LiteRT-LM file."
            failed -> downloadState.errorMessage ?: "Model download failed."
            downloadState.status == ModelDownloadStatus.Canceled -> "Download canceled."
            else -> "Install ${model.name} before FunctionGemma proposals can run."
        },
        primaryActionLabel = when {
            installedOrSucceeded -> "Model ready"
            downloading -> "Downloading..."
            failed -> "Retry verified model"
            else -> "Download verified model"
        },
        verifiedDownloadUrl = model.downloadUrl,
        localPath = expectedPath,
        progressText = modelDownloadProgressText(downloadState),
        advancedExpandedByDefault = false,
        showCancelAction = downloading,
        primaryActionEnabled = !installedOrSucceeded && !downloading
    )
}

@Composable
fun FunctionGemmaModelDownloadCard(
    model: FunctionGemmaModelDefinition,
    expectedPath: String,
    installed: Boolean,
    downloadState: ModelDownloadUiState,
    onDownload: (url: String, sha256: String) -> Unit,
    onCancel: () -> Unit
) {
    val state = functionGemmaModelCardState(
        model = model,
        expectedPath = expectedPath,
        installed = installed,
        downloadState = downloadState
    )
    val advancedExpanded = remember(model.fileName) {
        mutableStateOf(state.advancedExpandedByDefault)
    }
    val modelUrl = remember(model.downloadUrl) { mutableStateOf(model.downloadUrl) }
    val modelSha256 = remember(model.fileName) { mutableStateOf("") }
    val validationError = remember { mutableStateOf<String?>(null) }

    fun download(url: String, sha256: String) {
        try {
            onDownload(url.trim(), sha256.trim())
            validationError.value = null
        } catch (e: IllegalArgumentException) {
            validationError.value = e.message ?: "Invalid model download request."
        }
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
            FunctionGemmaModelHeader(
                model = model,
                state = state
            )
            ModelDownloadProgressBlock(
                downloadState = downloadState,
                progressText = state.progressText
            )
            FunctionGemmaModelActions(
                state = state,
                sha256 = modelSha256.value,
                onPrimaryAction = {
                    if (modelSha256.value.isBlank()) {
                        advancedExpanded.value = true
                        validationError.value = "Add a SHA-256 checksum before downloading the verified model."
                    } else {
                        download(state.verifiedDownloadUrl, modelSha256.value)
                    }
                },
                onCancel = onCancel
            )
            Text(
                "Local path: ${state.localPath}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FunctionGemmaAdvancedModelFields(
                expanded = advancedExpanded.value,
                modelUrl = modelUrl.value,
                modelSha256 = modelSha256.value,
                validationError = validationError.value,
                downloading = state.showCancelAction,
                onToggleExpanded = { advancedExpanded.value = !advancedExpanded.value },
                onModelUrlChange = { modelUrl.value = it },
                onModelSha256Change = { value ->
                    if (value.length <= 64) modelSha256.value = value.trim()
                },
                onDownload = { download(modelUrl.value, modelSha256.value) }
            )
        }
    }
}

@Composable
private fun FunctionGemmaModelHeader(
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
private fun FunctionGemmaModelActions(
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
private fun FunctionGemmaAdvancedModelFields(
    expanded: Boolean,
    modelUrl: String,
    modelSha256: String,
    validationError: String?,
    downloading: Boolean,
    onToggleExpanded: () -> Unit,
    onModelUrlChange: (String) -> Unit,
    onModelSha256Change: (String) -> Unit,
    onDownload: () -> Unit
) {
    TextButton(onClick = onToggleExpanded) {
        Text(if (expanded) "Hide manual download settings" else "Manual download settings")
    }
    if (!expanded) {
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = modelUrl,
            onValueChange = onModelUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Manual model HTTPS URL") },
            singleLine = true,
            enabled = !downloading
        )
        OutlinedTextField(
            value = modelSha256,
            onValueChange = onModelSha256Change,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("SHA-256 checksum") },
            singleLine = true,
            enabled = !downloading,
            supportingText = { Text("${modelSha256.length} / 64") }
        )
        validationError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        OutlinedButton(
            onClick = onDownload,
            enabled = !downloading && modelUrl.isNotBlank() && modelSha256.isNotBlank(),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download manual source")
        }
    }
}

@Composable
private fun ModelDownloadProgressBlock(
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

fun modelDownloadProgressText(downloadState: ModelDownloadUiState): String? {
    if (downloadState.status != ModelDownloadStatus.Running) return null

    return listOfNotNull(
        "${formatModelBytes(downloadState.receivedBytes)} / ${formatModelBytes(downloadState.totalBytes)}",
        downloadState.bytesPerSecond.takeIf { it > 0L }?.let { "${formatModelBytes(it)}/s" },
        downloadState.remainingMs?.let { "${formatModelDuration(it)} left" }
    ).joinToString(" - ")
}

fun formatModelBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return if (unitIndex == 0) {
        "${bytes} B"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

fun formatModelDuration(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0L)
    return when {
        seconds < 60L -> "${seconds}s"
        seconds < 3600L -> "${seconds / 60L}m ${seconds % 60L}s"
        else -> "${seconds / 3600L}h ${(seconds % 3600L) / 60L}m"
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
