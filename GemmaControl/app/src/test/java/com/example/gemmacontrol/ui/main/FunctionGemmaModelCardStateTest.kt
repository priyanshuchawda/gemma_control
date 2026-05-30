package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.model.FunctionGemmaModelCatalog
import com.example.gemmacontrol.ai.model.ModelDownloadStatus
import com.example.gemmacontrol.ai.model.ModelDownloadUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionGemmaModelCardStateTest {
    private val model = FunctionGemmaModelCatalog.MobileActions

    @Test
    fun missingModelShowsVerifiedDownloadActionWithoutOpeningAdvancedFields() {
        val state = functionGemmaModelCardState(
            model = model,
            expectedPath = "/files/models/mobile_actions_q8_ekv1024.litertlm",
            installed = false,
            downloadState = ModelDownloadUiState(ModelDownloadStatus.Idle)
        )

        assertEquals("Missing", state.statusLabel)
        assertEquals("Install MobileActions-270M before FunctionGemma proposals can run.", state.statusDescription)
        assertEquals("Download verified model", state.primaryActionLabel)
        assertEquals(model.downloadUrl, state.verifiedDownloadUrl)
        assertFalse(state.advancedExpandedByDefault)
        assertFalse(state.showCancelAction)
        assertNull(state.progressText)
    }

    @Test
    fun runningDownloadShowsBytesRateEtaAndCancelAction() {
        val state = functionGemmaModelCardState(
            model = model,
            expectedPath = "/files/models/mobile_actions_q8_ekv1024.litertlm",
            installed = false,
            downloadState = ModelDownloadUiState(
                status = ModelDownloadStatus.Running,
                receivedBytes = 50L * 1024L * 1024L,
                totalBytes = 100L * 1024L * 1024L,
                bytesPerSecond = 5L * 1024L * 1024L,
                remainingMs = 10_000L,
                fraction = 0.5f
            )
        )

        assertEquals("Downloading", state.statusLabel)
        assertEquals("Downloading and verifying the local LiteRT-LM file.", state.statusDescription)
        assertEquals("Downloading...", state.primaryActionLabel)
        assertEquals("50.0 MB / 100.0 MB - 5.0 MB/s - 10s left", state.progressText)
        assertTrue(state.showCancelAction)
    }

    @Test
    fun failedDownloadShowsRetryWithoutExpandingManualFields() {
        val state = functionGemmaModelCardState(
            model = model,
            expectedPath = "/files/models/mobile_actions_q8_ekv1024.litertlm",
            installed = false,
            downloadState = ModelDownloadUiState(
                status = ModelDownloadStatus.Failed,
                errorMessage = "SHA-256 mismatch."
            )
        )

        assertEquals("Failed", state.statusLabel)
        assertEquals("SHA-256 mismatch.", state.statusDescription)
        assertEquals("Retry verified model", state.primaryActionLabel)
        assertFalse(state.advancedExpandedByDefault)
        assertFalse(state.showCancelAction)
    }

    @Test
    fun installedModelShowsReadyStateAndLocalPath() {
        val state = functionGemmaModelCardState(
            model = model,
            expectedPath = "/files/models/mobile_actions_q8_ekv1024.litertlm",
            installed = true,
            downloadState = ModelDownloadUiState(ModelDownloadStatus.Succeeded)
        )

        assertEquals("Installed", state.statusLabel)
        assertEquals("MobileActions-270M is installed locally and ready for voice proposals.", state.statusDescription)
        assertEquals("Model ready", state.primaryActionLabel)
        assertEquals("/files/models/mobile_actions_q8_ekv1024.litertlm", state.localPath)
        assertFalse(state.primaryActionEnabled)
    }
}
