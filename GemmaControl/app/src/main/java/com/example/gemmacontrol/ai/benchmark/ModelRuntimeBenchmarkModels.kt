package com.example.gemmacontrol.ai.benchmark

data class RuntimeDeviceSnapshot(
    val deviceLabel: String,
    val totalMemoryBytes: Long?,
    val availableMemoryBytes: Long?,
    val appPssKb: Long?,
    val appRssKb: Long?,
    val batteryPercent: Int?,
    val charging: Boolean?,
    val batteryTemperatureC: Float?,
    val thermalStatus: Int?,
    val notificationListenerEnabled: Boolean,
    val postNotificationsGranted: Boolean,
    val recordAudioGranted: Boolean
)

enum class ModelRuntimeBenchmarkStatus {
    Completed,
    Blocked,
    Failed
}

data class ModelRuntimeBenchmarkScenario(
    val id: String,
    val label: String,
    val command: String
)

object ModelRuntimeBenchmarkScenarios {
    val default: List<ModelRuntimeBenchmarkScenario> = listOf(
        ModelRuntimeBenchmarkScenario(
            id = "typed_route",
            label = "Typed command routing",
            command = "Show my latest WhatsApp messages."
        ),
        ModelRuntimeBenchmarkScenario(
            id = "read_intent",
            label = "Read intent",
            command = "Read unread WhatsApp messages from the last 30 minutes."
        ),
        ModelRuntimeBenchmarkScenario(
            id = "reply_intent",
            label = "Reply intent",
            command = "Reply to the latest WhatsApp notification saying I am in a meeting."
        ),
        ModelRuntimeBenchmarkScenario(
            id = "search_intent",
            label = "Search intent",
            command = "Search WhatsApp messages for project update."
        ),
        ModelRuntimeBenchmarkScenario(
            id = "summarize_intent",
            label = "Summarize intent",
            command = "Summarize recent WhatsApp notifications without sending anything."
        ),
        ModelRuntimeBenchmarkScenario(
            id = "safe_delete_refusal",
            label = "Sensitive action routing",
            command = "Delete local WhatsApp data for one conversation."
        )
    )
}

data class ModelRuntimeBenchmarkStep(
    val id: String,
    val label: String,
    val elapsedMs: Long,
    val resultLabel: String
)

data class ModelRuntimeBenchmarkReport(
    val status: ModelRuntimeBenchmarkStatus,
    val summary: String,
    val modelFileStatus: String,
    val before: RuntimeDeviceSnapshot,
    val after: RuntimeDeviceSnapshot,
    val steps: List<ModelRuntimeBenchmarkStep>,
    val canRunSensitiveActions: Boolean
)

internal fun formatBytes(bytes: Long?): String {
    if (bytes == null) return "unknown"
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return "%.1f MB".format(mb)
    return "%.1f GB".format(mb / 1024.0)
}
