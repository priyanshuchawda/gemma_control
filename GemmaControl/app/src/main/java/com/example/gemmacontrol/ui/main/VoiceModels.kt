package com.example.gemmacontrol.ui.main

import java.util.Locale

sealed interface VoiceCommand {
    data object ReadLatestMessages : VoiceCommand

    data class ReplyToLatestActiveMessage(
        val replyText: String
    ) : VoiceCommand

    data class Unsupported(
        val reason: String
    ) : VoiceCommand
}

data class PendingVoiceReply(
    val notificationKey: String,
    val replyText: String,
    val conversationTitle: String
)

sealed interface VoiceAssistantState {
    data object Idle : VoiceAssistantState
    data object RequestingMicrophonePermission : VoiceAssistantState
    data object Listening : VoiceAssistantState
    data class TranscriptReady(val transcript: String) : VoiceAssistantState
    data class CommandReady(val command: VoiceCommand) : VoiceAssistantState
    data class ConfirmationRequired(val draft: PendingVoiceReply) : VoiceAssistantState
    data class SpeakingMessages(val count: Int) : VoiceAssistantState
    data class Failure(val safeReason: String) : VoiceAssistantState
}

object VoiceCommandParser {
    fun parse(text: String): VoiceCommand {
        val trimmed = text.trim()
        val lower = trimmed.lowercase(Locale.US)

        // Check for audio/voice messages first
        if (lower.contains("send a voice message") ||
            lower.contains("send an audio message") ||
            lower.contains("send a voice note")
        ) {
            return VoiceCommand.Unsupported("Sending WhatsApp audio voice notes is not supported yet. You can dictate a text reply instead.")
        }

        // Check for new message to a contact
        if (lower.contains("send a message to") ||
            lower.contains("send message to")
        ) {
            return VoiceCommand.Unsupported("Starting a new WhatsApp conversation by voice is not supported yet. I can reply to an active notification.")
        }

        // Check for read messages
        val normalizedRead = lower.removeSuffix(".").removeSuffix("?").trim()
        if (normalizedRead in listOf(
                "read my latest messages",
                "read latest messages",
                "read my notifications",
                "read current messages"
            )
        ) {
            return VoiceCommand.ReadLatestMessages
        }

        // Check for reply to latest message with broad support for variations
        val prefixes = listOf(
            "reply to the latest message:",
            "reply to the latest message",
            "reply to latest message:",
            "reply to latest message",
            "reply to the latest one:",
            "reply to the latest one",
            "reply latest message:",
            "reply latest message",
            "send reply to latest message:",
            "send reply to latest message",
            "send reply:",
            "send reply",
            "reply to it:",
            "reply to it",
            "reply:",
            "reply",
            "send message that:",
            "send message that",
            "send message:",
            "send message",
            "send a message that:",
            "send a message that",
            "send a message:",
            "send a message"
        )
        
        var matchedReplyPrefix = false
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
                matchedReplyPrefix = true
                val rawText = trimmed.substring(prefix.length).trim()
                val cleaned = rawText.removePrefix(":").removePrefix("that").trim().removePrefix(":").trim()
                if (cleaned.isEmpty()) {
                    return VoiceCommand.Unsupported("Reply text cannot be empty.")
                }
                if (cleaned.length > 1000) {
                    return VoiceCommand.Unsupported("Reply text is too long (maximum 1000 characters).")
                }
                return VoiceCommand.ReplyToLatestActiveMessage(cleaned)
            }
        }

        return VoiceCommand.Unsupported("I can currently read recent captured messages or reply to the latest active message.")
    }
}
