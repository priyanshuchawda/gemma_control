package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.NotificationParseSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceReadAloudBuilderTest {

    private val builder = VoiceReadAloudBuilder()

    @Test
    fun build_returnsEmptyMessageWhenNoCapturedMessagesExist() {
        val plan = builder.build(
            messages = emptyList(),
            request = VoiceReadAloudRequest.StoredLatest
        )

        assertEquals("There are no locally stored captured WhatsApp messages to read.", plan.spokenText)
        assertEquals(0, plan.spokenMessageCount)
        assertEquals(0, plan.nextOffset)
    }

    @Test
    fun build_readsSingleMessageDirectly() {
        val plan = builder.build(
            messages = listOf(message(id = "1", conversation = "Mom", text = "Dinner at 7")),
            request = VoiceReadAloudRequest.StoredLatest
        )

        assertEquals(1, plan.spokenMessageCount)
        assertEquals(1, plan.nextOffset)
        assertTrue(plan.spokenText.contains("You have 1 locally stored captured WhatsApp message."))
        assertTrue(plan.spokenText.contains("First message: From Mom. Dinner at 7."))
    }

    @Test
    fun build_readsThreeMessagesDirectly() {
        val plan = builder.build(
            messages = listOf(
                message(id = "1", conversation = "Mom", text = "Dinner at 7", postedAt = 3L),
                message(id = "2", conversation = "Peter", text = "On my way", postedAt = 2L),
                message(id = "3", conversation = "Work", sender = "Aunt May", text = "Call back", postedAt = 1L)
            ),
            request = VoiceReadAloudRequest.StoredLatest
        )

        assertEquals(3, plan.spokenMessageCount)
        assertEquals(3, plan.nextOffset)
        assertTrue(plan.spokenText.contains("You have 3 locally stored captured WhatsApp messages."))
        assertTrue(plan.spokenText.contains("Third message: From Aunt May in Work. Call back."))
    }

    @Test
    fun build_summarizesFourOrMoreMessagesByChatAndOffersFollowUps() {
        val plan = builder.build(
            messages = listOf(
                message(id = "1", conversation = "Mom", text = "Dinner", postedAt = 5L),
                message(id = "2", conversation = "Mom", text = "Bring milk", postedAt = 4L),
                message(id = "3", conversation = "Work", sender = "Peter", text = "Standup", postedAt = 3L),
                message(id = "4", conversation = "Peter", text = "On my way", postedAt = 2L)
            ),
            request = VoiceReadAloudRequest.StoredLatest
        )

        assertEquals(0, plan.spokenMessageCount)
        assertEquals(0, plan.nextOffset)
        assertTrue(plan.spokenText.contains("You have 4 locally stored captured WhatsApp messages across 3 chats."))
        assertTrue(plan.spokenText.contains("Mom has 2 messages: Dinner; Bring milk."))
        assertTrue(plan.spokenText.contains("Work has 1 message: Standup."))
        assertTrue(plan.spokenText.contains("Say continue to hear messages"))
    }

    @Test
    fun build_activeLatestReadsOnlyActiveNotificationMessages() {
        val plan = builder.build(
            messages = listOf(
                message(id = "old", conversation = "Mom", text = "Already seen", postedAt = 1L),
                message(id = "active", conversation = "Office", text = "Join now", postedAt = 2L)
            ),
            request = VoiceReadAloudRequest.Latest,
            activeNotificationKeys = setOf("key-active")
        )

        assertEquals(1, plan.spokenMessageCount)
        assertTrue(plan.spokenText.contains("You have 1 active WhatsApp notification message."))
        assertTrue(plan.spokenText.contains("Join now."))
        assertTrue(!plan.spokenText.contains("Already seen."))
    }

    @Test
    fun build_activeLatestDoesNotFallBackToStoredHistoryWhenNoActiveNotificationsExist() {
        val plan = builder.build(
            messages = listOf(message(id = "old", conversation = "Mom", text = "Already seen")),
            request = VoiceReadAloudRequest.Latest,
            activeNotificationKeys = emptySet()
        )

        assertEquals(0, plan.spokenMessageCount)
        assertEquals(0, plan.nextOffset)
        assertEquals(
            "There are no active WhatsApp notifications to read. Say read stored messages to hear local captured history.",
            plan.spokenText
        )
    }

    @Test
    fun build_continueReadsNextWindowAndReportsRemainingCount() {
        val messages = (1..5).map { index ->
            message(
                id = index.toString(),
                conversation = "Chat $index",
                text = "Message $index",
                postedAt = (10 - index).toLong()
            )
        }

        val plan = builder.build(
            messages = messages,
            request = VoiceReadAloudRequest.Continue,
            continueOffset = 0
        )

        assertEquals(3, plan.spokenMessageCount)
        assertEquals(3, plan.nextOffset)
        assertTrue(plan.spokenText.contains("There are 2 more. Say continue to hear more."))
    }

    @Test
    fun build_filtersMessagesFromSpecificChat() {
        val plan = builder.build(
            messages = listOf(
                message(id = "1", conversation = "Mom", text = "Dinner", postedAt = 3L),
                message(id = "2", conversation = "Peter", text = "Project", postedAt = 2L)
            ),
            request = VoiceReadAloudRequest.Conversation("Mom")
        )

        assertEquals(1, plan.spokenMessageCount)
        assertTrue(plan.spokenText.contains("You have 1 locally stored captured WhatsApp message from Mom."))
        assertTrue(plan.spokenText.contains("Dinner."))
    }

    @Test
    fun build_summarizeModeAlwaysSummarizesMultiChatSet() {
        val plan = builder.build(
            messages = listOf(
                message(id = "1", conversation = "Mom", text = "Dinner", postedAt = 3L),
                message(id = "2", conversation = "Work", text = "Standup", postedAt = 2L),
                message(id = "3", conversation = "Peter", text = "Project", postedAt = 1L)
            ),
            request = VoiceReadAloudRequest.Summarize
        )

        assertEquals(0, plan.spokenMessageCount)
        assertTrue(plan.spokenText.contains("You have 3 locally stored captured WhatsApp messages across 3 chats."))
    }

    @Test
    fun build_chatScopedSummarizeModeSummarizesSpecificChatEvenForSmallSets() {
        val plan = builder.build(
            messages = listOf(
                message(id = "1", conversation = "Mom", text = "Dinner", postedAt = 3L),
                message(id = "2", conversation = "Mom", text = "Bring medicine", postedAt = 2L),
                message(id = "3", conversation = "Work", text = "Standup", postedAt = 1L)
            ),
            request = VoiceReadAloudRequest.ConversationSummary("Mom")
        )

        assertEquals(0, plan.spokenMessageCount)
        assertTrue(plan.spokenText.contains("You have 2 locally stored captured WhatsApp messages from Mom."))
        assertTrue(plan.spokenText.contains("Mom has 2 messages: Dinner; Bring medicine."))
        assertTrue(!plan.spokenText.contains("First message:"))
    }

    @Test
    fun build_importantModeOnlyReadsHighPriorityMessages() {
        val plan = builder.build(
            messages = listOf(
                message(id = "1", conversation = "Mom", text = "Urgent", priority = "HIGH", postedAt = 3L),
                message(id = "2", conversation = "Peter", text = "Normal", priority = "NORMAL", postedAt = 2L)
            ),
            request = VoiceReadAloudRequest.ImportantOnly
        )

        assertEquals(1, plan.spokenMessageCount)
        assertTrue(plan.spokenText.contains("You have 1 important locally stored captured WhatsApp message."))
        assertTrue(plan.spokenText.contains("Urgent."))
        assertTrue(!plan.spokenText.contains("Normal."))
    }

    @Test
    fun build_removesEmojiOnlySpeechNoise() {
        val plan = builder.build(
            messages = listOf(message(id = "1", conversation = "Mom", text = "😂😂 ok")),
            request = VoiceReadAloudRequest.StoredLatest
        )

        assertTrue(plan.spokenText.contains("ok."))
        assertTrue(!plan.spokenText.contains("😂"))
    }

    private fun message(
        id: String,
        conversation: String,
        text: String,
        sender: String? = conversation,
        postedAt: Long = 1L,
        priority: String = "NORMAL"
    ): StoredInboxRepository.DecryptedMessage {
        return StoredInboxRepository.DecryptedMessage(
            id = id,
            conversationId = conversation,
            senderName = sender,
            decryptedText = text,
            postedAt = postedAt,
            notificationKey = "key-$id",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            isContentUnavailable = false,
            createdAt = postedAt,
            priority = priority
        )
    }
}
