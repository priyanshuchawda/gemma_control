package com.example.gemmacontrol.ai.model

data class ModelDownloadProgress(
    val receivedBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
    val remainingMs: Long?,
    val fraction: Float?
) {
    companion object {
        fun calculate(
            receivedBytes: Long,
            totalBytes: Long,
            startedAtMs: Long,
            nowMs: Long
        ): ModelDownloadProgress {
            val safeReceivedBytes = receivedBytes.coerceAtLeast(0L)
            val safeTotalBytes = totalBytes.coerceAtLeast(0L)
            val elapsedMs = (nowMs - startedAtMs).coerceAtLeast(1L)
            val bytesPerSecond = (safeReceivedBytes * 1000L / elapsedMs).coerceAtLeast(0L)
            val hasKnownTotal = safeTotalBytes > 0L
            val fraction = if (hasKnownTotal) {
                (safeReceivedBytes.toFloat() / safeTotalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
            val remainingMs = if (hasKnownTotal && bytesPerSecond > 0L) {
                ((safeTotalBytes - safeReceivedBytes).coerceAtLeast(0L) * 1000L) / bytesPerSecond
            } else {
                null
            }

            return ModelDownloadProgress(
                receivedBytes = safeReceivedBytes,
                totalBytes = safeTotalBytes,
                bytesPerSecond = bytesPerSecond,
                remainingMs = remainingMs,
                fraction = fraction
            )
        }
    }
}

object ModelDownloadFiles {
    const val TEMP_EXTENSION = ".gallerytmp"
    const val MODEL_DIRECTORY = "models"
    private val SAFE_MODEL_FILE_NAME = Regex("[A-Za-z0-9._-]+\\.litertlm")

    fun temporaryFileName(fileName: String): String = "$fileName$TEMP_EXTENSION"

    fun requireSafeFileName(fileName: String): String {
        require(fileName.isNotBlank()) { "Model file name is required." }
        require(SAFE_MODEL_FILE_NAME.matches(fileName)) {
            "Model file name must use only letters, numbers, dots, underscores, or hyphens and end with .litertlm."
        }
        return fileName
    }
}
