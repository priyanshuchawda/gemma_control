package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceReplyTargetResolverTest {

    @Test
    fun genericReplyUsesSingleActiveTarget() {
        val resolution = VoiceReplyTargetResolver.resolveLatest(
            replyText = "On my way",
            explicitLatest = false,
            targets = listOf(target("key-mom", "Mom", postedAt = 10))
        )

        assertEquals(
            VoiceReplyTargetResolution.Active(
                PendingVoiceReply("key-mom", "On my way", "Mom")
            ),
            resolution
        )
    }

    @Test
    fun genericReplyAsksClarificationForMultipleTargets() {
        val resolution = VoiceReplyTargetResolver.resolveLatest(
            replyText = "On my way",
            explicitLatest = false,
            targets = listOf(
                target("key-office", "Office Group", postedAt = 20),
                target("key-mom", "Mom", postedAt = 10)
            )
        )

        assertEquals(
            VoiceReplyTargetResolution.Clarification(
                "Which chat should I reply to? Active reply targets: Office Group, Mom."
            ),
            resolution
        )
    }

    @Test
    fun explicitLatestUsesMostRecentActiveTarget() {
        val resolution = VoiceReplyTargetResolver.resolveLatest(
            replyText = "On my way",
            explicitLatest = true,
            targets = listOf(
                target("key-mom", "Mom", postedAt = 10),
                target("key-office", "Office Group", postedAt = 20)
            )
        )

        assertEquals(
            VoiceReplyTargetResolution.Active(
                PendingVoiceReply("key-office", "On my way", "Office Group")
            ),
            resolution
        )
    }

    @Test
    fun noActiveTargetReturnsActionableClarification() {
        val resolution = VoiceReplyTargetResolver.resolveLatest(
            replyText = "On my way",
            explicitLatest = false,
            targets = emptyList()
        )

        assertEquals(
            VoiceReplyTargetResolution.Clarification(
                "No active WhatsApp reply notification is available. To avoid sending to the wrong chat, say: draft reply to Mom: On my way."
            ),
            resolution
        )
    }

    @Test
    fun namedReplyUsesMatchingActiveTargetOrDraftFallback() {
        val active = VoiceReplyTargetResolver.resolveNamed(
            conversationName = "mom",
            replyText = "On my way",
            targets = listOf(target("key-mom", "Mom", postedAt = 10))
        )
        val draft = VoiceReplyTargetResolver.resolveNamed(
            conversationName = "Dad",
            replyText = "On my way",
            targets = listOf(target("key-mom", "Mom", postedAt = 10))
        )

        assertEquals(
            VoiceReplyTargetResolution.Active(
                PendingVoiceReply("key-mom", "On my way", "Mom")
            ),
            active
        )
        assertEquals(
            VoiceReplyTargetResolution.Draft(conversationName = "Dad", replyText = "On my way"),
            draft
        )
    }

    @Test
    fun clarificationPromptBoundsLongTargetLists() {
        val resolution = VoiceReplyTargetResolver.resolveLatest(
            replyText = "On my way",
            explicitLatest = false,
            targets = listOf(
                target("1", "A", 4),
                target("2", "B", 3),
                target("3", "C", 2),
                target("4", "D", 1)
            )
        )

        assertTrue(resolution is VoiceReplyTargetResolution.Clarification)
        assertEquals(
            "Which chat should I reply to? Active reply targets: A, B, C, plus 1 more.",
            (resolution as VoiceReplyTargetResolution.Clarification).prompt
        )
    }

    private fun target(
        key: String,
        conversationTitle: String,
        postedAt: Long
    ) = ActiveReplyTarget(
        notificationKey = key,
        conversationTitle = conversationTitle,
        postedAt = postedAt
    )
}
