package com.example.gemmacontrol.ai.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelDownloadProgressTest {
    private val validSha256 = "a".repeat(64)

    @Test
    fun calculatesFractionRateAndRemainingTime() {
        val progress = ModelDownloadProgress.calculate(
            receivedBytes = 50L * 1024L * 1024L,
            totalBytes = 100L * 1024L * 1024L,
            startedAtMs = 1_000L,
            nowMs = 11_000L
        )

        assertEquals(0.5f, progress.fraction)
        assertEquals(5L * 1024L * 1024L, progress.bytesPerSecond)
        assertEquals(10_000L, progress.remainingMs)
    }

    @Test
    fun unknownOrZeroTotalDoesNotReportFractionOrEta() {
        val progress = ModelDownloadProgress.calculate(
            receivedBytes = 10L,
            totalBytes = 0L,
            startedAtMs = 1_000L,
            nowMs = 2_000L
        )

        assertNull(progress.fraction)
        assertNull(progress.remainingMs)
        assertEquals(10L, progress.bytesPerSecond)
    }

    @Test
    fun buildsGalleryStyleTemporaryFileName() {
        assertEquals(
            "mobile_actions_q8_ekv1024.litertlm.gallerytmp",
            ModelDownloadFiles.temporaryFileName("mobile_actions_q8_ekv1024.litertlm")
        )
    }

    @Test
    fun rejectsUnsafeModelFileNames() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelDownloadFiles.requireSafeFileName("../mobile_actions_q8_ekv1024.litertlm")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ModelDownloadFiles.requireSafeFileName("mobile_actions_q8_ekv1024.tflite")
        }
    }

    @Test
    fun requestRequiresHttpsAndValidChecksum() {
        assertThrows(IllegalArgumentException::class.java) {
            ModelDownloadRequest(
                url = "http://example.com/mobile_actions_q8_ekv1024.litertlm",
                fileName = "mobile_actions_q8_ekv1024.litertlm",
                sha256 = validSha256
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ModelDownloadRequest(
                url = "https://example.com/mobile_actions_q8_ekv1024.litertlm",
                fileName = "mobile_actions_q8_ekv1024.litertlm",
                sha256 = "not-a-sha"
            )
        }
    }
}
