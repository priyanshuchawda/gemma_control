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
    "read my latest stored messages",
    "read my stored messages",
    "read latest stored messages",
    "show my latest stored whatsapp messages",
    "show me my latest stored whatsapp messages",
    "read my latest messages",
    "show my latest whatsapp messages",
    "show me my latest whatsapp messages",
    "show me latest whatsapp messages",
    "show my latest whatsapp message",
    "show me my latest whatsapp message",
    "show me latest whatsapp message",
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

private val continueReadPhrases = listOf(
    "continue",
    "read more",
    "keep reading",
    "next messages",
    "read next messages",
    "more messages"
)

private val summarizeReadPhrases = listOf(
    "summarize whatsapp",
    "summarize my whatsapp",
    "summarize whatsapp messages",
    "summarize my whatsapp messages",
    "summarize messages",
    "summarize notifications"
)

private val importantReadPhrases = listOf(
    "only important",
    "read important",
    "read important messages",
    "read important whatsapp messages",
    "read only important",
    "read only important whatsapp messages",
    "show important whatsapp messages",
    "tell me important whatsapp messages"
)

private val readIntentVerbs = listOf("read", "show", "tell")
private val readIntentRecencyWords = listOf("latest", "recent", "current")
private val readIntentTargetWords = listOf("message", "messages", "notification", "notifications")

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

sealed interface VoiceReadAloudRequest {
    data object Latest : VoiceReadAloudRequest
    data object Continue : VoiceReadAloudRequest
    data object Summarize : VoiceReadAloudRequest
    data object ImportantOnly : VoiceReadAloudRequest
    data class Conversation(val conversationName: String) : VoiceReadAloudRequest
}

sealed interface VoiceCommand {
    data object ReadLatestMessages : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.Latest
    }

    data object ContinueReadingMessages : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.Continue
    }

    data object SummarizeWhatsAppMessages : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.Summarize
    }

    data object ReadImportantMessages : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.ImportantOnly
    }

    data class ReadMessagesFromConversation(
        val conversationName: String
    ) : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.Conversation(conversationName)
    }

    data class ReplyToLatestActiveMessage(
        val replyText: String
    ) : VoiceCommand

    data class Unsupported(
        val reason: String
    ) : VoiceCommand
}

sealed interface VoiceReadCommand : VoiceCommand {
    val readRequest: VoiceReadAloudRequest
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
            ?: continueReadingMessagesCommand(lower)
            ?: summarizeMessagesCommand(lower)
            ?: importantMessagesCommand(lower)
            ?: readMessagesFromConversationCommand(trimmed)
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
        return if (normalizedRead in readPhrases || normalizedRead.isRecentWhatsAppReadIntent()) {
            VoiceCommand.ReadLatestMessages
        } else {
            null
        }
    }

    private fun continueReadingMessagesCommand(lower: String): VoiceCommand.ContinueReadingMessages? {
        val normalized = lower.removeSuffix(".").removeSuffix("?").trim()
        return if (normalized in continueReadPhrases) {
            VoiceCommand.ContinueReadingMessages
        } else {
            null
        }
    }

    private fun summarizeMessagesCommand(lower: String): VoiceCommand.SummarizeWhatsAppMessages? {
        val normalized = lower.removeSuffix(".").removeSuffix("?").trim()
        return if (normalized in summarizeReadPhrases || normalized.startsWith("summarize whatsapp ")) {
            VoiceCommand.SummarizeWhatsAppMessages
        } else {
            null
        }
    }

    private fun importantMessagesCommand(lower: String): VoiceCommand.ReadImportantMessages? {
        val normalized = lower.removeSuffix(".").removeSuffix("?").trim()
        return if (normalized in importantReadPhrases || (normalized.contains("important") && normalized.contains("whatsapp"))) {
            VoiceCommand.ReadImportantMessages
        } else {
            null
        }
    }

    private fun readMessagesFromConversationCommand(trimmed: String): VoiceCommand.ReadMessagesFromConversation? {
        val match = Regex(
            pattern = """^(?:read|show|tell)(?:\s+me)?\s+(?:whatsapp\s+)?(?:messages?|notifications?)\s+from\s+(.+?)[?.]?$""",
            option = RegexOption.IGNORE_CASE
        ).find(trimmed.trim()) ?: return null
        val conversationName = match.groupValues[1].trim()
            .removePrefix(":")
            .trim()
        return conversationName
            .takeIf { it.isNotBlank() }
            ?.let { VoiceCommand.ReadMessagesFromConversation(it) }
    }

    private fun String.isRecentWhatsAppReadIntent(): Boolean {
        val hasReadVerb = readIntentVerbs.any { containsWord(it) }
        val hasRecency = readIntentRecencyWords.any { containsWord(it) }
        val hasWhatsApp = contains("whatsapp")
        val hasTarget = readIntentTargetWords.any { containsWord(it) }
        return hasReadVerb && hasRecency && hasWhatsApp && hasTarget
    }

    private fun String.containsWord(word: String): Boolean {
        return Regex("""\b${Regex.escape(word)}\b""").containsMatchIn(this)
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
