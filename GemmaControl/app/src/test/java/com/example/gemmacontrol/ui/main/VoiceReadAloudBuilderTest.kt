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
            request = VoiceReadAloudRequest.Latest
        )

        assertEquals("There are no captured WhatsApp messages to read.", plan.spokenText)
        assertEquals(0, plan.spokenMessageCount)
        assertEquals(0, plan.nextOffset)
    }

    @Test
    fun build_readsSingleMessageDirectly() {
        val plan = builder.build(
            messages = listOf(message(id = "1", conversation = "Mom", text = "Dinner at 7")),
            request = VoiceReadAloudRequest.Latest
        )

        assertEquals(1, plan.spokenMessageCount)
        assertEquals(1, plan.nextOffset)
        assertTrue(plan.spokenText.contains("You have 1 recent WhatsApp message."))
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
            request = VoiceReadAloudRequest.Latest
        )

        assertEquals(3, plan.spokenMessageCount)
        assertEquals(3, plan.nextOffset)
        assertTrue(plan.spokenText.contains("You have 3 recent WhatsApp messages."))
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
            request = VoiceReadAloudRequest.Latest
        )

        assertEquals(0, plan.spokenMessageCount)
        assertEquals(0, plan.nextOffset)
        assertTrue(plan.spokenText.contains("You have 4 recent WhatsApp messages across 3 chats."))
        assertTrue(plan.spokenText.contains("Mom has 2 messages."))
        assertTrue(plan.spokenText.contains("Say continue to hear messages"))
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
        assertTrue(plan.spokenText.contains("You have 1 recent WhatsApp message from Mom."))
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
        assertTrue(plan.spokenText.contains("You have 3 recent WhatsApp messages across 3 chats."))
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
        assertTrue(plan.spokenText.contains("You have 1 important WhatsApp message."))
        assertTrue(plan.spokenText.contains("Urgent."))
        assertTrue(!plan.spokenText.contains("Normal."))
    }

    @Test
    fun build_removesEmojiOnlySpeechNoise() {
        val plan = builder.build(
            messages = listOf(message(id = "1", conversation = "Mom", text = "😂😂 ok")),
            request = VoiceReadAloudRequest.Latest
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
