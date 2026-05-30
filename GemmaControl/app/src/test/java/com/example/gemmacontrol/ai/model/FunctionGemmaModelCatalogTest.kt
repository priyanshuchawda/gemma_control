package com.example.gemmacontrol.ai.model

import com.example.gemmacontrol.ai.runtime.GemmaBackend
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionGemmaModelCatalogTest {

    @Test
    fun resolvesMissingMobileActionsModel() {
        val filesDir = Files.createTempDirectory("gemma-files").toFile()
        val cacheDir = Files.createTempDirectory("gemma-cache").toFile()
        val resolver = FunctionGemmaModelResolver(filesDir = filesDir, cacheDir = cacheDir)

        val result = resolver.resolveMobileActionsModel()

        assertTrue(result is InstalledFunctionGemmaModel.Missing)
        assertEquals(
            File(filesDir, "models/mobile_actions_q8_ekv1024.litertlm").absolutePath,
            (result as InstalledFunctionGemmaModel.Missing).expectedPath
        )
    }

    @Test
    fun resolvesInstalledMobileActionsModelWithGalleryDefaults() {
        val filesDir = Files.createTempDirectory("gemma-files").toFile()
        val cacheDir = Files.createTempDirectory("gemma-cache").toFile()
        val modelDirectory = File(filesDir, ModelDownloadFiles.MODEL_DIRECTORY).apply { mkdirs() }
        val modelFile = File(modelDirectory, FunctionGemmaModelCatalog.MobileActions.fileName)
        modelFile.writeBytes(byteArrayOf(1, 2, 3))
        val resolver = FunctionGemmaModelResolver(filesDir = filesDir, cacheDir = cacheDir)

        val result = resolver.resolveMobileActionsModel()

        assertTrue(result is InstalledFunctionGemmaModel.Ready)
        val config = (result as InstalledFunctionGemmaModel.Ready).config
        assertEquals(modelFile.absolutePath, config.modelPath)
        assertEquals(GemmaBackend.CPU, config.backend)
        assertEquals(1024, config.maxTokens)
        assertEquals(64, config.topK)
        assertEquals(0.95f, config.topP)
        assertEquals(0.0f, config.temperature)
        assertEquals(File(cacheDir, "litertlm").absolutePath, config.cacheDirectoryPath)
    }

    @Test
    fun mobileActionsModelExposesGalleryStyleDownloadUrl() {
        val model = FunctionGemmaModelCatalog.MobileActions

        assertEquals(
            "https://huggingface.co/litert-community/functiongemma-270m-ft-mobile-actions/resolve/38942192c9b723af836d489074823ff33d4a3e7a/mobile_actions_q8_ekv1024.litertlm?download=true",
            model.downloadUrl
        )
    }
}
