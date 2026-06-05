package com.example.gemmacontrol.ai.media

import com.example.gemmacontrol.notifications.WhatsAppContentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaUnderstandingPolicyTest {

    @Test
    fun notificationMediaPlaceholdersNeverAllowByteAnalysis() {
        val mediaKinds = listOf(
            WhatsAppContentKind.PHOTO,
            WhatsAppContentKind.VIDEO,
            WhatsAppContentKind.STICKER,
            WhatsAppContentKind.AUDIO,
            WhatsAppContentKind.DOCUMENT
        )

        mediaKinds.forEach { kind ->
            val decision = MediaUnderstandingPolicy.fromNotificationContent(kind, rawText = kind.name)

            assertEquals(MediaAccessState.NotificationPlaceholderOnly, decision.accessState)
            assertFalse(decision.canInspectMediaBytes)
            assertFalse(decision.hasUserGrantedMediaAccess)
            assertEquals(MediaUnderstandingModelPath.None, decision.modelPath)
            assertTrue(decision.reason.contains("placeholder only"))
            assertNoInventedMediaDescription(decision.userFacingText)
            assertNoInventedMediaDescription(decision.promptText)
        }
    }

    @Test
    fun hiddenNotificationContentIsNotAnalyzableMedia() {
        val decision = MediaUnderstandingPolicy.fromNotificationContent(
            WhatsAppContentKind.HIDDEN,
            rawText = null
        )

        assertEquals(MediaAccessState.Unsupported, decision.accessState)
        assertFalse(decision.canInspectMediaBytes)
        assertEquals(MediaUnderstandingModelPath.None, decision.modelPath)
        assertEquals("Content hidden or unavailable", decision.userFacingText)
    }

    @Test
    fun userSelectedVisualMediaCanEnterFutureAnalysisPathAfterConfirmation() {
        val decision = MediaUnderstandingPolicy.forUserSelectedFile(
            displayName = "test-photo.jpg",
            mimeType = "image/jpeg"
        )

        assertEquals(MediaAccessState.UserSelectedFile, decision.accessState)
        assertTrue(decision.canInspectMediaBytes)
        assertTrue(decision.hasUserGrantedMediaAccess)
        assertTrue(decision.requiresUserConfirmationBeforeAnalysis)
        assertEquals(MediaUnderstandingModelPath.FutureVisionUnderstanding, decision.modelPath)
        assertTrue(decision.promptText.contains("Actual user-selected media bytes"))
    }

    @Test
    fun localUriGrantCanEnterFutureAnalysisPathButStillRequiresConfirmation() {
        val decision = MediaUnderstandingPolicy.forLocalUriWithGrant(
            displayName = "test-video.mp4",
            mimeType = "video/mp4"
        )

        assertEquals(MediaAccessState.LocalUriWithGrant, decision.accessState)
        assertTrue(decision.canInspectMediaBytes)
        assertTrue(decision.hasUserGrantedMediaAccess)
        assertTrue(decision.requiresUserConfirmationBeforeAnalysis)
        assertEquals(MediaUnderstandingModelPath.FutureVisionUnderstanding, decision.modelPath)
    }

    @Test
    fun unsupportedSelectedFileDoesNotEnterVisionPath() {
        val decision = MediaUnderstandingPolicy.forUserSelectedFile(
            displayName = "test-document.pdf",
            mimeType = "application/pdf"
        )

        assertEquals(MediaAccessState.Unsupported, decision.accessState)
        assertFalse(decision.canInspectMediaBytes)
        assertTrue(decision.hasUserGrantedMediaAccess)
        assertFalse(decision.requiresUserConfirmationBeforeAnalysis)
        assertEquals(MediaUnderstandingModelPath.None, decision.modelPath)
    }

    private fun assertNoInventedMediaDescription(text: String) {
        val lower = text.lowercase()
        assertFalse(lower.contains("person"))
        assertFalse(lower.contains("face"))
        assertFalse(lower.contains("scene"))
        assertFalse(lower.contains("object"))
        assertFalse(lower.contains("screenshot says"))
        assertTrue(
            lower.contains("not inspected") ||
                lower.contains("cannot inspect") ||
                lower.contains("cannot transcribe") ||
                lower.contains("not transcribed")
        )
    }
}
