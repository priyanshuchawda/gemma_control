package com.example.gemmacontrol.ai.benchmark

import com.example.gemmacontrol.ai.model.FunctionGemmaModelCatalog
import com.example.gemmacontrol.ai.model.FunctionGemmaModelResolver
import com.example.gemmacontrol.ai.model.ModelDownloadFiles
import com.example.gemmacontrol.ai.runtime.GemmaEngine
import com.example.gemmacontrol.ai.runtime.GemmaEngineConfig
import com.example.gemmacontrol.ai.runtime.GemmaEngineResult
import com.example.gemmacontrol.ai.runtime.GemmaModelManager
import com.example.gemmacontrol.ai.tools.ToolCallParseResult
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRuntimeBenchmarkRunnerTest {

    @Test
    fun missingModelProducesBlockedReportWithoutDownloadInstruction() = runTest {
        val filesDir = Files.createTempDirectory("gemma-files").toFile()
        val cacheDir = Files.createTempDirectory("gemma-cache").toFile()
        val runner = ModelRuntimeBenchmarkRunner(
            modelResolver = FunctionGemmaModelResolver(filesDir = filesDir, cacheDir = cacheDir),
            modelManager = GemmaModelManager { RecordingEngine() },
            snapshotProvider = { testSnapshot(availableMemoryBytes = 100L) },
            nanoTime = incrementingClock()
        )

        val report = runner.run()

        assertEquals(ModelRuntimeBenchmarkStatus.Blocked, report.status)
        assertEquals(0, report.steps.size)
        assertTrue(report.summary.contains("missing"))
        assertFalse(report.summary.contains("download", ignoreCase = true))
        assertFalse(report.canRunSensitiveActions)
    }

    @Test
    fun installedModelMeasuresColdWarmAndSyntheticRoutes() = runTest {
        val filesDir = Files.createTempDirectory("gemma-files").toFile()
        val cacheDir = Files.createTempDirectory("gemma-cache").toFile()
        val modelDirectory = File(filesDir, ModelDownloadFiles.MODEL_DIRECTORY).apply { mkdirs() }
        File(modelDirectory, FunctionGemmaModelCatalog.MobileActions.fileName).writeBytes(byteArrayOf(1, 2, 3))
        val engine = RecordingEngine()
        val runner = ModelRuntimeBenchmarkRunner(
            modelResolver = FunctionGemmaModelResolver(filesDir = filesDir, cacheDir = cacheDir),
            modelManager = GemmaModelManager { engine },
            snapshotProvider = {
                if (engine.generatedPrompts.isEmpty()) {
                    testSnapshot(availableMemoryBytes = 200L)
                } else {
                    testSnapshot(availableMemoryBytes = 150L)
                }
            },
            nanoTime = incrementingClock(stepNanos = 2_000_000L)
        )

        val report = runner.run()

        assertEquals(ModelRuntimeBenchmarkStatus.Completed, report.status)
        assertEquals(2 + ModelRuntimeBenchmarkScenarios.default.size, report.steps.size)
        assertEquals("Cold model initialize", report.steps[0].label)
        assertEquals(2L, report.steps[0].elapsedMs)
        assertEquals("Warm initialize reuse", report.steps[1].label)
        assertEquals(2L, report.steps[1].elapsedMs)
        assertEquals(ModelRuntimeBenchmarkScenarios.default.size, engine.generatedPrompts.size)
        assertTrue(report.summary.contains("6 synthetic routes"))
        assertTrue(report.modelFileStatus.contains("3 B"))
        assertEquals(200L, report.before.availableMemoryBytes)
        assertEquals(150L, report.after.availableMemoryBytes)
        assertFalse(engine.generatedPrompts.any { it.contains("Peter Parker") })
    }

    private fun testSnapshot(availableMemoryBytes: Long): RuntimeDeviceSnapshot {
        return RuntimeDeviceSnapshot(
            deviceLabel = "Xiaomi 2406ERN9CI Android 16 API 36",
            totalMemoryBytes = 5_531_208L * 1024L,
            availableMemoryBytes = availableMemoryBytes,
            appPssKb = 124_915L,
            appRssKb = 138_260L,
            batteryPercent = 49,
            charging = true,
            batteryTemperatureC = 32.0f,
            thermalStatus = 0,
            notificationListenerEnabled = true,
            postNotificationsGranted = true,
            recordAudioGranted = true
        )
    }

    private fun incrementingClock(stepNanos: Long = 1_000_000L): () -> Long {
        var now = 0L
        return {
            val current = now
            now += stepNanos
            current
        }
    }
}

private class RecordingEngine : GemmaEngine {
    val generatedPrompts = mutableListOf<String>()
    override val isReady: Boolean
        get() = initialized
    private var initialized = false

    override suspend fun initialize(config: GemmaEngineConfig): GemmaEngineResult {
        initialized = true
        return GemmaEngineResult.Ready
    }

    override suspend fun generateToolProposal(
        prompt: String,
        registry: WhatsAppToolRegistry,
        onPartialText: (String) -> Unit
    ): GemmaEngineResult {
        generatedPrompts += prompt
        return GemmaEngineResult.ProposalText(
            rawText = "{}",
            parseResult = ToolCallParseResult.Invalid("synthetic benchmark result")
        )
    }

    override fun close() {
        initialized = false
    }
}
