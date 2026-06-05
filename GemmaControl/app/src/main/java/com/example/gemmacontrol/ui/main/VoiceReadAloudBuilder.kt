package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.WhatsAppContentKind
import com.example.gemmacontrol.notifications.spokenSummaryText
import java.util.Locale

internal data class VoiceReadAloudPlan(
    val spokenText: String,
    val spokenMessageCount: Int,
    val nextOffset: Int
)

internal class VoiceReadAloudBuilder(
    private val directWindowSize: Int = 3,
    private val maxSummaryChats: Int = 4
) {
    fun build(
        messages: List<StoredInboxRepository.DecryptedMessage>,
        request: VoiceReadAloudRequest,
        continueOffset: Int = 0,
        forceDirect: Boolean = request == VoiceReadAloudRequest.Continue
    ): VoiceReadAloudPlan {
        val sortedMessages = messages
            .asSequence()
            .filterByRequest(request)
            .sortedByDescending { it.postedAt }
            .toList()

        if (sortedMessages.isEmpty()) {
            return VoiceReadAloudPlan(
                spokenText = emptyStateText(request),
                spokenMessageCount = 0,
                nextOffset = 0
            )
        }

        if (request == VoiceReadAloudRequest.Summarize) {
            return summarize(sortedMessages, request, continueOffset = 0)
        }

        return if (forceDirect || sortedMessages.size <= directWindowSize) {
            readDirect(sortedMessages, request, continueOffset.coerceAtLeast(0))
        } else {
            summarize(sortedMessages, request, continueOffset = 0)
        }
    }

    private fun Sequence<StoredInboxRepository.DecryptedMessage>.filterByRequest(
        request: VoiceReadAloudRequest
    ): Sequence<StoredInboxRepository.DecryptedMessage> {
        return when (request) {
            VoiceReadAloudRequest.Latest,
            VoiceReadAloudRequest.Continue,
            VoiceReadAloudRequest.Summarize -> this
            VoiceReadAloudRequest.ImportantOnly -> filter {
                it.priority.equals("HIGH", ignoreCase = true)
            }
            is VoiceReadAloudRequest.Conversation -> {
                val target = normalizeName(request.conversationName)
                filter { message ->
                    normalizeName(message.conversationId) == target ||
                        normalizeName(message.senderName.orEmpty()) == target
                }
            }
        }
    }

    private fun readDirect(
        messages: List<StoredInboxRepository.DecryptedMessage>,
        request: VoiceReadAloudRequest,
        continueOffset: Int
    ): VoiceReadAloudPlan {
        val boundedOffset = continueOffset.coerceIn(0, messages.size)
        val selected = messages.drop(boundedOffset).take(directWindowSize)
        if (selected.isEmpty()) {
            return VoiceReadAloudPlan(
                spokenText = "There are no more locally stored captured WhatsApp messages to read.",
                spokenMessageCount = 0,
                nextOffset = boundedOffset
            )
        }

        val intro = directIntro(
            totalCount = messages.size,
            selectedCount = selected.size,
            request = request,
            isContinuation = boundedOffset > 0 || request == VoiceReadAloudRequest.Continue
        )
        val spokenMessages = selected.mapIndexed { index, message ->
            messageLine(position = boundedOffset + index, message = message)
        }
        val nextOffset = (boundedOffset + selected.size).coerceAtMost(messages.size)
        val remaining = messages.size - nextOffset
        val continuationHint = if (remaining > 0) {
            " There ${if (remaining == 1) "is" else "are"} $remaining more. Say continue to hear more."
        } else {
            ""
        }

        return VoiceReadAloudPlan(
            spokenText = (listOf(intro) + spokenMessages).joinToString(separator = " ") + continuationHint,
            spokenMessageCount = selected.size,
            nextOffset = nextOffset
        )
    }

    private fun summarize(
        messages: List<StoredInboxRepository.DecryptedMessage>,
        request: VoiceReadAloudRequest,
        continueOffset: Int
    ): VoiceReadAloudPlan {
        val chats = messages
            .groupBy { it.conversationId.ifBlank { "WhatsApp Chat" } }
            .toList()
            .sortedByDescending { (_, chatMessages) -> chatMessages.maxOf { it.postedAt } }

        val intro = summaryIntro(messages.size, chats.size, request)
        val chatSummary = chats.take(maxSummaryChats).joinToString(separator = " ") { (chat, chatMessages) ->
            "$chat has ${chatMessages.size} ${messageWord(chatMessages.size)}."
        }
        val hiddenChats = chats.size - maxSummaryChats
        val hiddenSummary = if (hiddenChats > 0) {
            " Plus $hiddenChats more ${chatWord(hiddenChats)}."
        } else {
            ""
        }
        val nextActions = " Say continue to hear messages, read messages from a chat, summarize WhatsApp, or only important."

        return VoiceReadAloudPlan(
            spokenText = intro + " " + chatSummary + hiddenSummary + nextActions,
            spokenMessageCount = 0,
            nextOffset = continueOffset
        )
    }

    private fun directIntro(
        totalCount: Int,
        selectedCount: Int,
        request: VoiceReadAloudRequest,
        isContinuation: Boolean
    ): String {
        if (isContinuation && selectedCount < totalCount) {
            return "Reading $selectedCount of $totalCount ${scopedMessageLabel(totalCount, request)}."
        }
        return "You have $totalCount ${scopedMessageLabel(totalCount, request)}."
    }

    private fun summaryIntro(
        messageCount: Int,
        chatCount: Int,
        request: VoiceReadAloudRequest
    ): String {
        val scopedLabel = scopedMessageLabel(messageCount, request)
        return when (request) {
            is VoiceReadAloudRequest.Conversation -> "You have $messageCount $scopedLabel."
            else -> "You have $messageCount $scopedLabel across $chatCount ${chatWord(chatCount)}."
        }
    }

    private fun scopedMessageLabel(count: Int, request: VoiceReadAloudRequest): String {
        val base = when (request) {
            VoiceReadAloudRequest.ImportantOnly -> "important locally stored captured WhatsApp ${messageWord(count)}"
            is VoiceReadAloudRequest.Conversation ->
                "locally stored captured WhatsApp ${messageWord(count)} from ${request.conversationName}"
            else -> "locally stored captured WhatsApp ${messageWord(count)}"
        }
        return base
    }

    private fun messageLine(
        position: Int,
        message: StoredInboxRepository.DecryptedMessage
    ): String {
        val ordinal = when (position) {
            0 -> "First message"
            1 -> "Second message"
            2 -> "Third message"
            3 -> "Fourth message"
            4 -> "Fifth message"
            else -> "Next message"
        }
        val source = messageSource(message)
        val spokenText = message.contentKind.spokenSummaryText(message.decryptedText)
            .safeSpeechText(message.contentKind)
        return "$ordinal: $source. $spokenText."
    }

    private fun messageSource(message: StoredInboxRepository.DecryptedMessage): String {
        val conversation = message.conversationId.ifBlank { "WhatsApp Chat" }
        val sender = message.senderName?.takeIf { it.isNotBlank() }
        return if (sender != null && !sender.equals(conversation, ignoreCase = true)) {
            "From $sender in $conversation"
        } else {
            "From ${sender ?: conversation}"
        }
    }

    private fun emptyStateText(request: VoiceReadAloudRequest): String {
        return when (request) {
            VoiceReadAloudRequest.ImportantOnly -> "There are no important locally stored captured WhatsApp messages to read."
            is VoiceReadAloudRequest.Conversation ->
                "There are no locally stored captured WhatsApp messages from ${request.conversationName}."
            else -> "There are no locally stored captured WhatsApp messages to read."
        }
    }

    private fun String.safeSpeechText(contentKind: WhatsAppContentKind): String {
        if (contentKind != WhatsAppContentKind.TEXT) {
            return this
        }
        val cleaned = replace(Regex("[\\p{So}\\p{Sk}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifBlank { "Emoji-only message" }
    }

    private fun normalizeName(value: String): String {
        return value.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    private fun messageWord(count: Int): String = if (count == 1) "message" else "messages"

    private fun chatWord(count: Int): String = if (count == 1) "chat" else "chats"
}
