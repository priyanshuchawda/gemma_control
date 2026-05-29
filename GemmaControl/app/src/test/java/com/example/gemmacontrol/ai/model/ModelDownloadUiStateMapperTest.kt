package com.example.gemmacontrol.ai.model

import androidx.work.WorkInfo
import androidx.work.workDataOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelDownloadUiStateMapperTest {

    @Test
    fun mapsRunningProgressWithFractionRateAndEta() {
        val state = ModelDownloadUiStateMapper.map(
            workState = WorkInfo.State.RUNNING,
            progress = workDataOf(
                ModelDownloadContract.KEY_MODEL_DOWNLOAD_RECEIVED_BYTES to 50L,
                ModelDownloadContract.KEY_MODEL_TOTAL_BYTES to 100L,
                ModelDownloadContract.KEY_MODEL_DOWNLOAD_RATE to 10L,
                ModelDownloadContract.KEY_MODEL_DOWNLOAD_REMAINING_MS to 5_000L
            ),
            output = workDataOf()
        )

        assertEquals(ModelDownloadStatus.Running, state.status)
        assertEquals(0.5f, state.fraction)
        assertEquals(50L, state.receivedBytes)
        assertEquals(100L, state.totalBytes)
        assertEquals(10L, state.bytesPerSecond)
        assertEquals(5_000L, state.remainingMs)
    }

    @Test
    fun mapsSucceededOutputPath() {
        val state = ModelDownloadUiStateMapper.map(
            workState = WorkInfo.State.SUCCEEDED,
            progress = workDataOf(),
            output = workDataOf(ModelDownloadContract.KEY_MODEL_PATH to "/models/mobile.litertlm")
        )

        assertEquals(ModelDownloadStatus.Succeeded, state.status)
        assertEquals("/models/mobile.litertlm", state.outputPath)
    }

    @Test
    fun mapsFailureErrorAndUnknownProgress() {
        val state = ModelDownloadUiStateMapper.map(
            workState = WorkInfo.State.FAILED,
            progress = workDataOf(ModelDownloadContract.KEY_MODEL_DOWNLOAD_REMAINING_MS to -1L),
            output = workDataOf(ModelDownloadContract.KEY_MODEL_ERROR to "Invalid model URL.")
        )

        assertEquals(ModelDownloadStatus.Failed, state.status)
        assertEquals("Invalid model URL.", state.errorMessage)
        assertNull(state.remainingMs)
        assertNull(state.fraction)
    }
}
