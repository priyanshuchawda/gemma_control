package com.example.gemmacontrol.ai.benchmark

import com.example.gemmacontrol.ai.model.FunctionGemmaModelResolver
import com.example.gemmacontrol.ai.model.InstalledFunctionGemmaModel
import com.example.gemmacontrol.ai.runtime.GemmaEngineResult
import com.example.gemmacontrol.ai.runtime.GemmaModelManager
import com.example.gemmacontrol.ai.tools.GemmaPromptBuilder
import com.example.gemmacontrol.ai.tools.WhatsAppToolRegistry
import java.io.File
import kotlin.math.max

class ModelRuntimeBenchmarkRunner(
    private val modelResolver: FunctionGemmaModelResolver,
    private val modelManager: GemmaModelManager,
    private val snapshotProvider: suspend () -> RuntimeDeviceSnapshot,
    private val registry: WhatsAppToolRegistry = WhatsAppToolRegistry.default(),
    private val promptBuilder: GemmaPromptBuilder = GemmaPromptBuilder(registry),
    private val scenarios: List<ModelRuntimeBenchmarkScenario> = ModelRuntimeBenchmarkScenarios.default,
    private val nanoTime: () -> Long = System::nanoTime
) {
    suspend fun run(): ModelRuntimeBenchmarkReport {
        val before = snapshotProvider()
        val resolvedModel = modelResolver.resolveMobileActionsModel()
        if (resolvedModel is InstalledFunctionGemmaModel.Missing) {
            val after = snapshotProvider()
            return ModelRuntimeBenchmarkReport(
                status = ModelRuntimeBenchmarkStatus.Blocked,
                summary = "FunctionGemma model file is missing; benchmark skipped.",
                modelFileStatus = "Missing at ${resolvedModel.expectedPath}",
                before = before,
                after = after,
                steps = emptyList(),
                canRunSensitiveActions = false
            )
        }

        val readyModel = resolvedModel as InstalledFunctionGemmaModel.Ready
        val steps = mutableListOf<ModelRuntimeBenchmarkStep>()
        try {
            val cold = measureMs {
                modelManager.release()
                modelManager.initialize(readyModel.config)
            }
            steps += ModelRuntimeBenchmarkStep(
                id = "cold_initialize",
                label = "Cold model initialize",
                elapsedMs = cold.elapsedMs,
                resultLabel = cold.value.safeResultLabel()
            )

            val warm = measureMs {
                modelManager.initialize(readyModel.config)
            }
            steps += ModelRuntimeBenchmarkStep(
                id = "warm_initialize",
                label = "Warm initialize reuse",
                elapsedMs = warm.elapsedMs,
                resultLabel = warm.value.safeResultLabel()
            )

            for (scenario in scenarios) {
                val prompt = promptBuilder.buildForUserCommand(
                    userCommand = scenario.command,
                    messages = emptyList()
                )
                val measured = measureMs {
                    modelManager.generateToolProposal(prompt, registry)
                }
                steps += ModelRuntimeBenchmarkStep(
                    id = scenario.id,
                    label = scenario.label,
                    elapsedMs = measured.elapsedMs,
                    resultLabel = measured.value.safeResultLabel()
                )
            }

            val after = snapshotProvider()
            return ModelRuntimeBenchmarkReport(
                status = ModelRuntimeBenchmarkStatus.Completed,
                summary = "Captured cold/warm load and ${scenarios.size} synthetic routes.",
                modelFileStatus = "Installed at ${readyModel.config.modelPath} (${formatBytes(File(readyModel.config.modelPath).length())})",
                before = before,
                after = after,
                steps = steps,
                canRunSensitiveActions = false
            )
        } catch (e: Exception) {
            val after = snapshotProvider()
            return ModelRuntimeBenchmarkReport(
                status = ModelRuntimeBenchmarkStatus.Failed,
                summary = "Benchmark failed safely: ${e.message ?: "unknown runtime error"}",
                modelFileStatus = "Installed at ${readyModel.config.modelPath} (${formatBytes(File(readyModel.config.modelPath).length())})",
                before = before,
                after = after,
                steps = steps,
                canRunSensitiveActions = false
            )
        }
    }

    private suspend fun <T> measureMs(block: suspend () -> T): Measured<T> {
        val start = nanoTime()
        val value = block()
        val end = nanoTime()
        return Measured(value = value, elapsedMs = max(0L, (end - start) / 1_000_000L))
    }
}

private data class Measured<T>(
    val value: T,
    val elapsedMs: Long
)

private fun GemmaEngineResult.safeResultLabel(): String {
    return when (this) {
        GemmaEngineResult.Ready -> "ready"
        is GemmaEngineResult.ProposalText -> "proposal_text"
        is GemmaEngineResult.NativeToolAction -> "native_tool_action:${action::class.simpleName ?: "action"}"
        is GemmaEngineResult.Blocked -> "blocked"
        is GemmaEngineResult.Failure -> "failed"
    }
}
