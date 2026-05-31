package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.model.FunctionGemmaModelDefinition
import com.example.gemmacontrol.ai.model.ModelDownloadStatus
import com.example.gemmacontrol.ai.model.ModelDownloadUiState

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

internal const val ModelSha256HexLength = 64
