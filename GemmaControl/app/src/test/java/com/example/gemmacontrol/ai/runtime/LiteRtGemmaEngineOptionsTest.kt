package com.example.gemmacontrol.ai.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class LiteRtGemmaEngineOptionsTest {

    @Test
    fun mapsGemmaEngineConfigToGalleryDefaultLiteRtOptions() {
        val options = GemmaEngineConfig(modelPath = "/models/functiongemma.litertlm")
            .toLiteRtGemmaEngineOptions()

        assertEquals("/models/functiongemma.litertlm", options.modelPath)
        assertEquals("GPU", options.backend)
        assertEquals(DEFAULT_MAX_TOKENS, options.maxTokens)
        assertEquals(DEFAULT_TOP_K, options.topK)
        assertEquals(DEFAULT_TOP_P, options.topP)
        assertEquals(DEFAULT_TEMPERATURE, options.temperature)
        assertTrue(options.automaticToolCalling)
        assertNull(options.cacheDirectoryPath)
    }

    @Test
    fun preservesExplicitSamplingAndCacheOptions() {
        val options = GemmaEngineConfig(
            modelPath = "/models/functiongemma.litertlm",
            backend = GemmaBackend.CPU,
            maxTokens = 256,
            topK = 32,
            topP = 0.8f,
            temperature = 0.7f,
            cacheDirectoryPath = "/cache"
        ).toLiteRtGemmaEngineOptions()

        assertEquals("CPU", options.backend)
        assertEquals(256, options.maxTokens)
        assertEquals(32, options.topK)
        assertEquals(0.8f, options.topP)
        assertEquals(0.7f, options.temperature)
        assertEquals("/cache", options.cacheDirectoryPath)
    }
}
