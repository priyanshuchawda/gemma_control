package com.example.gemmacontrol.ai.runtime

internal data class LiteRtGemmaEngineOptions(
    val modelPath: String,
    val backend: String,
    val maxTokens: Int,
    val topK: Int,
    val topP: Float,
    val temperature: Float,
    val automaticToolCalling: Boolean,
    val cacheDirectoryPath: String?
)

internal fun GemmaEngineConfig.toLiteRtGemmaEngineOptions(): LiteRtGemmaEngineOptions {
    return LiteRtGemmaEngineOptions(
        modelPath = modelPath,
        backend = "GPU",
        maxTokens = maxTokens,
        topK = topK,
        topP = topP,
        temperature = temperature,
        automaticToolCalling = false,
        cacheDirectoryPath = cacheDirectoryPath
    )
}
