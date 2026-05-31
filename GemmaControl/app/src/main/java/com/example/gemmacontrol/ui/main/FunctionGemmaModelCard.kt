package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.ai.model.FunctionGemmaModelDefinition
import com.example.gemmacontrol.ai.model.ModelDownloadUiState

data class FunctionGemmaModelDownloadActions(
    val onDownload: (url: String, sha256: String) -> Unit,
    val onCancel: () -> Unit
)

@Composable
fun FunctionGemmaModelDownloadCard(
    model: FunctionGemmaModelDefinition,
    expectedPath: String,
    installed: Boolean,
    downloadState: ModelDownloadUiState,
    actions: FunctionGemmaModelDownloadActions
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
    val requestDownload: (String, String) -> Unit = { url, sha256 ->
        try {
            actions.onDownload(url.trim(), sha256.trim())
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
            FunctionGemmaModelHeader(model = model, state = state)
            ModelDownloadProgressBlock(downloadState = downloadState, progressText = state.progressText)
            FunctionGemmaModelActions(
                state = state,
                sha256 = modelSha256.value,
                onPrimaryAction = {
                    if (modelSha256.value.isBlank()) {
                        advancedExpanded.value = true
                        validationError.value = "Add a SHA-256 checksum before downloading the verified model."
                    } else {
                        requestDownload(state.verifiedDownloadUrl, modelSha256.value)
                    }
                },
                onCancel = actions.onCancel
            )
            Text(
                "Local path: ${state.localPath}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FunctionGemmaAdvancedModelFields(
                state = FunctionGemmaAdvancedModelFieldsState(
                    expanded = advancedExpanded.value,
                    modelUrl = modelUrl.value,
                    modelSha256 = modelSha256.value,
                    validationError = validationError.value,
                    downloading = state.showCancelAction
                ),
                actions = FunctionGemmaAdvancedModelFieldActions(
                    onToggleExpanded = { advancedExpanded.value = !advancedExpanded.value },
                    onModelUrlChange = { modelUrl.value = it },
                    onModelSha256Change = { value ->
                        if (value.length <= ModelSha256HexLength) modelSha256.value = value.trim()
                    },
                    onDownload = { requestDownload(modelUrl.value, modelSha256.value) }
                )
            )
        }
    }
}
