package com.example.gemmacontrol.ai.model

import com.example.gemmacontrol.ai.runtime.GemmaBackend
import com.example.gemmacontrol.ai.runtime.GemmaEngineConfig
import java.io.File

data class FunctionGemmaModelDefinition(
    val name: String,
    val modelId: String,
    val fileName: String,
    val sizeInBytes: Long,
    val commitHash: String,
    val backend: GemmaBackend,
    val maxTokens: Int,
    val topK: Int,
    val topP: Float,
    val temperature: Float
) {
    init {
        ModelDownloadFiles.requireSafeFileName(fileName)
    }

    fun toEngineConfig(modelPath: String, cacheDirectoryPath: String): GemmaEngineConfig {
        return GemmaEngineConfig(
            modelPath = modelPath,
            backend = backend,
            maxTokens = maxTokens,
            topK = topK,
            topP = topP,
            temperature = temperature,
            cacheDirectoryPath = cacheDirectoryPath
        )
    }

    val downloadUrl: String
        get() = "https://huggingface.co/$modelId/resolve/$commitHash/$fileName?download=true"
}

object FunctionGemmaModelCatalog {
    val MobileActions = FunctionGemmaModelDefinition(
        name = "MobileActions-270M",
        modelId = "litert-community/functiongemma-270m-ft-mobile-actions",
        fileName = "mobile_actions_q8_ekv1024.litertlm",
        sizeInBytes = 288_964_608L,
        commitHash = "38942192c9b723af836d489074823ff33d4a3e7a",
        backend = GemmaBackend.CPU,
        maxTokens = 1024,
        topK = 64,
        topP = 0.95f,
        temperature = 0.0f
    )
}

sealed interface InstalledFunctionGemmaModel {
    data class Ready(
        val definition: FunctionGemmaModelDefinition,
        val config: GemmaEngineConfig
    ) : InstalledFunctionGemmaModel

    data class Missing(
        val definition: FunctionGemmaModelDefinition,
        val expectedPath: String
    ) : InstalledFunctionGemmaModel
}

class FunctionGemmaModelResolver(
    private val filesDir: File,
    private val cacheDir: File
) {
    fun resolveMobileActionsModel(): InstalledFunctionGemmaModel {
        return resolve(FunctionGemmaModelCatalog.MobileActions)
    }

    private fun resolve(definition: FunctionGemmaModelDefinition): InstalledFunctionGemmaModel {
        val modelFile = File(
            File(filesDir, ModelDownloadFiles.MODEL_DIRECTORY),
            definition.fileName
        )
        if (!modelFile.isFile || modelFile.length() <= 0L) {
            return InstalledFunctionGemmaModel.Missing(
                definition = definition,
                expectedPath = modelFile.absolutePath
            )
        }

        val liteRtCacheDir = File(cacheDir, "litertlm").apply { mkdirs() }
        return InstalledFunctionGemmaModel.Ready(
            definition = definition,
            config = definition.toEngineConfig(
                modelPath = modelFile.absolutePath,
                cacheDirectoryPath = liteRtCacheDir.absolutePath
            )
        )
    }
}
