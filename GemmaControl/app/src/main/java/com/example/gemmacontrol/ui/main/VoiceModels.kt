package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolProposal
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

data class PendingLocalToolAction(
    val title: String,
    val description: String,
    val confirmText: String,
    val proposal: ToolProposal,
    val decision: ToolExecutionDecision
)

sealed interface VoiceAssistantState {
    data object Idle : VoiceAssistantState
    data object RequestingMicrophonePermission : VoiceAssistantState
    data object Listening : VoiceAssistantState
    data class TranscriptReady(val transcript: String) : VoiceAssistantState
    data class CommandReady(val command: VoiceCommand) : VoiceAssistantState
    data class Streaming(val partialText: String) : VoiceAssistantState
    data class ConfirmationRequired(val draft: PendingVoiceReply) : VoiceAssistantState
    data class LocalToolConfirmationRequired(val action: PendingLocalToolAction) : VoiceAssistantState
    data class LocalToolSucceeded(val message: String) : VoiceAssistantState
    data class SpeakingMessages(val count: Int) : VoiceAssistantState
    data class Failure(val safeReason: String) : VoiceAssistantState
    data object LanguagePackMissingError : VoiceAssistantState
    data object ConfirmSystemRecognitionConsent : VoiceAssistantState
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
        val readPhrases = listOf(
            "read my latest messages",
            "read latest messages",
            "read my notifications",
            "read current messages",
            "read recent messages",
            "read messages",
            "read my messages",
            "read notifications",
            "read latest",
            "read current",
            "read recent",
            "read"
        )
        if (normalizedRead in readPhrases) {
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
            "reply that:",
            "reply that",
            "reply:",
            "reply",
            "send message that:",
            "send message that",
            "send message:",
            "send message",
            "send a message that:",
            "send a message that",
            "send a message:",
            "send a message",
            "say that:",
            "say that",
            "say:",
            "say",
            "tell them that:",
            "tell them that",
            "tell them:",
            "tell them"
        )
        
        for (prefix in prefixes) {
            if (lower.startsWith(prefix)) {
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

        return VoiceCommand.Unsupported("I can currently read captured messages or reply to the latest active WhatsApp notification.")
    }
}
