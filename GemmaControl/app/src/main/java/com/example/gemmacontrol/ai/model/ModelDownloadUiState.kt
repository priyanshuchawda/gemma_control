package com.example.gemmacontrol.ai.model

import androidx.work.Data
import androidx.work.WorkInfo

enum class ModelDownloadStatus {
    Idle,
    Enqueued,
    Running,
    Succeeded,
    Failed,
    Canceled
}

data class ModelDownloadUiState(
    val status: ModelDownloadStatus,
    val receivedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val remainingMs: Long? = null,
    val fraction: Float? = null,
    val outputPath: String? = null,
    val errorMessage: String? = null
)

object ModelDownloadUiStateMapper {
    fun map(
        workState: WorkInfo.State?,
        progress: Data,
        output: Data
    ): ModelDownloadUiState {
        val receivedBytes = progress.getLong(ModelDownloadContract.KEY_MODEL_DOWNLOAD_RECEIVED_BYTES, 0L)
        val totalBytes = progress.getLong(ModelDownloadContract.KEY_MODEL_TOTAL_BYTES, 0L)
        val bytesPerSecond = progress.getLong(ModelDownloadContract.KEY_MODEL_DOWNLOAD_RATE, 0L)
        val remainingMs = progress.getLong(ModelDownloadContract.KEY_MODEL_DOWNLOAD_REMAINING_MS, -1L)
            .takeIf { it >= 0L }
        val fraction = if (totalBytes > 0L) {
            (receivedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }

        return ModelDownloadUiState(
            status = workState.toDownloadStatus(),
            receivedBytes = receivedBytes,
            totalBytes = totalBytes,
            bytesPerSecond = bytesPerSecond,
            remainingMs = remainingMs,
            fraction = fraction,
            outputPath = output.getString(ModelDownloadContract.KEY_MODEL_PATH),
            errorMessage = output.getString(ModelDownloadContract.KEY_MODEL_ERROR)
        )
    }

    fun map(workInfo: WorkInfo?): ModelDownloadUiState {
        return map(
            workState = workInfo?.state,
            progress = workInfo?.progress ?: Data.EMPTY,
            output = workInfo?.outputData ?: Data.EMPTY
        )
    }

    private fun WorkInfo.State?.toDownloadStatus(): ModelDownloadStatus {
        return when (this) {
            null -> ModelDownloadStatus.Idle
            WorkInfo.State.ENQUEUED -> ModelDownloadStatus.Enqueued
            WorkInfo.State.RUNNING -> ModelDownloadStatus.Running
            WorkInfo.State.SUCCEEDED -> ModelDownloadStatus.Succeeded
            WorkInfo.State.FAILED -> ModelDownloadStatus.Failed
            WorkInfo.State.BLOCKED -> ModelDownloadStatus.Enqueued
            WorkInfo.State.CANCELLED -> ModelDownloadStatus.Canceled
        }
    }
}
