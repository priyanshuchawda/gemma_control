package com.example.gemmacontrol.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gemmacontrol.ai.benchmark.ModelRuntimeBenchmarkReport
import com.example.gemmacontrol.ai.benchmark.ModelRuntimeBenchmarkStatus
import com.example.gemmacontrol.ai.benchmark.RuntimeDeviceSnapshot
import com.example.gemmacontrol.ai.benchmark.formatBytes

@Composable
fun ModelRuntimeBenchmarkCard(
    report: ModelRuntimeBenchmarkReport?,
    running: Boolean,
    onRunBenchmark: () -> Unit
) {
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Runtime benchmark",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "Captures cold/warm FunctionGemma timings, synthetic routing latency, and device memory/battery/thermal state. No model download is started.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onRunBenchmark,
                enabled = !running,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (running) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(18.dp)
                    )
                    Text("Capturing...")
                } else {
                    Text("Capture baseline")
                }
            }

            report?.let { currentReport ->
                BenchmarkStatusBadge(currentReport.status)
                Text(
                    currentReport.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    currentReport.modelFileStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BenchmarkSnapshotBlock("Before", currentReport.before)
                BenchmarkSnapshotBlock("After", currentReport.after)
                currentReport.steps.forEach { step ->
                    BenchmarkMetricRow(
                        label = step.label,
                        value = "${step.elapsedMs} ms",
                        detail = step.resultLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchmarkStatusBadge(status: ModelRuntimeBenchmarkStatus) {
    val label = when (status) {
        ModelRuntimeBenchmarkStatus.Completed -> "Completed"
        ModelRuntimeBenchmarkStatus.Blocked -> "Blocked"
        ModelRuntimeBenchmarkStatus.Failed -> "Failed"
    }
    val color = when (status) {
        ModelRuntimeBenchmarkStatus.Completed -> MaterialTheme.colorScheme.primary
        ModelRuntimeBenchmarkStatus.Blocked -> MaterialTheme.colorScheme.secondary
        ModelRuntimeBenchmarkStatus.Failed -> MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BenchmarkSnapshotBlock(label: String, snapshot: RuntimeDeviceSnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        BenchmarkMetricRow("Device", snapshot.deviceLabel)
        BenchmarkMetricRow("Available memory", formatBytes(snapshot.availableMemoryBytes))
        BenchmarkMetricRow("App PSS", snapshot.appPssKb?.let { "$it KB" } ?: "unknown")
        BenchmarkMetricRow("Battery", snapshot.batteryPercent?.let { "$it%" } ?: "unknown")
        BenchmarkMetricRow("Battery temp", snapshot.batteryTemperatureC?.let { "%.1f C".format(it) } ?: "unknown")
        BenchmarkMetricRow("Charging", snapshot.charging?.toString() ?: "unknown")
        BenchmarkMetricRow("Thermal status", snapshot.thermalStatus?.toString() ?: "unknown")
        BenchmarkMetricRow("Notification listener", snapshot.notificationListenerEnabled.readyLabel())
        BenchmarkMetricRow("Microphone", snapshot.recordAudioGranted.readyLabel())
    }
}

@Composable
private fun BenchmarkMetricRow(
    label: String,
    value: String,
    detail: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.9f)
        )
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1.1f)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Boolean.readyLabel(): String = if (this) "ready" else "missing"
