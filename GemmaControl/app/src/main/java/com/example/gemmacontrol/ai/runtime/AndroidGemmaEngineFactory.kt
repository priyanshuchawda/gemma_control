package com.example.gemmacontrol.ai.runtime

import android.content.Context
import java.io.File

class AndroidGemmaEngineFactory(context: Context) : () -> GemmaEngine {
    private val cacheDirectory = File(context.applicationContext.cacheDir, "litertlm")

    override fun invoke(): GemmaEngine {
        cacheDirectory.mkdirs()
        return LiteRtGemmaEngine(fallbackCacheDirectoryPath = cacheDirectory.absolutePath)
    }
}
