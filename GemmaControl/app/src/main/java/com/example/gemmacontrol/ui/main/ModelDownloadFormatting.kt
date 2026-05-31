package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.model.ModelDownloadStatus
import com.example.gemmacontrol.ai.model.ModelDownloadUiState
import java.util.Locale

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
    while (value >= BytesPerDisplayUnit && unitIndex < units.lastIndex) {
        value /= BytesPerDisplayUnit
        unitIndex += 1
    }
    return if (unitIndex == 0) {
        "${bytes} B"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

fun formatModelDuration(ms: Long): String {
    val seconds = (ms / MillisPerSecond).coerceAtLeast(0L)
    return when {
        seconds < SecondsPerMinute -> "${seconds}s"
        seconds < SecondsPerHour -> "${seconds / SecondsPerMinute}m ${seconds % SecondsPerMinute}s"
        else -> "${seconds / SecondsPerHour}h ${(seconds % SecondsPerHour) / SecondsPerMinute}m"
    }
}

private const val BytesPerDisplayUnit = 1024.0
private const val MillisPerSecond = 1000L
private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 3600L
