package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.ToolExecutionDecision
import com.example.gemmacontrol.ai.tools.ToolProposal
import java.util.Locale

private const val MaxVoiceReplyCharacters = 10 * 10 * 10

private val unsupportedVoiceMessagePhrases = listOf(
    "send a voice message",
    "send an audio message",
    "send a voice note"
)

private val newConversationPhrases = listOf(
    "send a message to",
    "send message to"
)

private val readPhrases = listOf(
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

private val replyPrefixes = listOf(
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

        return unsupportedVoiceMessage(lower)
            ?: unsupportedNewConversation(lower)
            ?: readLatestMessagesCommand(lower)
            ?: replyToLatestMessageCommand(trimmed, lower)
            ?: defaultUnsupportedCommand()
    }

    private fun unsupportedVoiceMessage(lower: String): VoiceCommand.Unsupported? {
        return if (unsupportedVoiceMessagePhrases.any(lower::contains)) {
            VoiceCommand.Unsupported("Sending WhatsApp audio voice notes is not supported yet. You can dictate a text reply instead.")
        } else {
            null
        }
    }

    private fun unsupportedNewConversation(lower: String): VoiceCommand.Unsupported? {
        return if (newConversationPhrases.any(lower::contains)) {
            VoiceCommand.Unsupported("Starting a new WhatsApp conversation needs FunctionGemma and a verified E.164 phone number. I can reply to an active notification.")
        } else {
            null
        }
    }

    private fun readLatestMessagesCommand(lower: String): VoiceCommand.ReadLatestMessages? {
        val normalizedRead = lower.removeSuffix(".").removeSuffix("?").trim()
        return if (normalizedRead in readPhrases) {
            VoiceCommand.ReadLatestMessages
        } else {
            null
        }
    }

    private fun replyToLatestMessageCommand(
        trimmed: String,
        lower: String
    ): VoiceCommand? {
        val prefix = replyPrefixes.firstOrNull(lower::startsWith) ?: return null
        val cleaned = trimmed.substring(prefix.length)
            .trim()
            .removePrefix(":")
            .removePrefix("that")
            .trim()
            .removePrefix(":")
            .trim()

        return when {
            cleaned.isEmpty() -> VoiceCommand.Unsupported("Reply text cannot be empty.")
            cleaned.length > MaxVoiceReplyCharacters -> VoiceCommand.Unsupported(
                "Reply text is too long (maximum $MaxVoiceReplyCharacters characters)."
            )
            else -> VoiceCommand.ReplyToLatestActiveMessage(cleaned)
        }
    }

    private fun defaultUnsupportedCommand(): VoiceCommand.Unsupported {
        return VoiceCommand.Unsupported("I can currently read captured messages or reply to the latest active WhatsApp notification.")
    }
}
