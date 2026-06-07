package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.ai.tools.WhatsAppToolAction
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

private val storedReadPhrases = listOf(
    "read my latest stored messages",
    "read my stored messages",
    "read latest stored messages",
    "read stored messages",
    "show stored messages",
    "show my stored messages",
    "show my latest stored whatsapp messages",
    "show me my latest stored whatsapp messages"
)

private val readPhrases = listOf(
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
    "summarize message",
    "summarize messages",
    "summarize my message",
    "summarize my messages",
    "summarize whatsapp messages",
    "summarize my whatsapp messages",
    "summarize notifications",
    "summary of whatsapp",
    "summary of whatsapp message",
    "summary of whatsapp messages",
    "catch me up on whatsapp",
    "what happened in whatsapp"
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
    data object StoredLatest : VoiceReadAloudRequest
    data object Continue : VoiceReadAloudRequest
    data object Summarize : VoiceReadAloudRequest
    data object ImportantOnly : VoiceReadAloudRequest
    data class Conversation(val conversationName: String) : VoiceReadAloudRequest
    data class ConversationSummary(val conversationName: String) : VoiceReadAloudRequest
}

sealed interface VoiceCommand {
    data object ReadLatestMessages : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.Latest
    }

    data object ReadStoredMessages : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.StoredLatest
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

    data class SummarizeMessagesFromConversation(
        val conversationName: String
    ) : VoiceReadCommand {
        override val readRequest: VoiceReadAloudRequest = VoiceReadAloudRequest.ConversationSummary(conversationName)
    }

    data class ReplyToLatestActiveMessage(
        val replyText: String,
        val explicitLatest: Boolean = false
    ) : VoiceCommand

    data class ReplyToConversation(
        val conversationName: String,
        val replyText: String
    ) : VoiceCommand

    data class LocalToolAction(
        val action: WhatsAppToolAction
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
    data class ClarificationRequired(val prompt: String) : VoiceAssistantState
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
            ?: localWriteWorkflowCommand(trimmed)
            ?: pendingFollowUpsCommand(lower)
            ?: actionableInboxCommand(lower)
            ?: searchCapturedMessagesCommand(trimmed, lower)
            ?: continueReadingMessagesCommand(lower)
            ?: summarizeMessagesFromConversationCommand(trimmed)
            ?: summarizeMessagesCommand(lower)
            ?: importantMessagesCommand(lower)
            ?: readMessagesFromConversationCommand(trimmed)
            ?: readLatestMessagesCommand(lower)
            ?: replyToNamedConversationCommand(trimmed)
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

    private fun searchCapturedMessagesCommand(
        trimmed: String,
        lower: String
    ): VoiceCommand.LocalToolAction? {
        if (!lower.startsWith("search") && !lower.startsWith("find")) {
            return null
        }

        senderSearchRegex.find(trimmed.trim())?.let { match ->
            val conversationName = match.groupValues[1].trim()
            val query = match.groupValues[2].trim().trimEnd('.', '?')
            if (conversationName.isNotBlank() && query.isNotBlank()) {
                return VoiceCommand.LocalToolAction(
                    WhatsAppToolAction.SearchMessages(
                        query = query,
                        conversationName = conversationName
                    )
                )
            }
        }

        recentSearchRegex.find(trimmed.trim())?.let { match ->
            val sinceMinutes = match.groupValues[1].toIntOrNull()
            val query = match.groupValues[2].trim().trimEnd('.', '?')
            if (sinceMinutes != null && sinceMinutes > 0 && query.isNotBlank()) {
                return VoiceCommand.LocalToolAction(
                    WhatsAppToolAction.SearchMessages(
                        query = query,
                        conversationName = null,
                        sinceMinutes = sinceMinutes
                    )
                )
            }
        }

        genericSearchRegex.find(trimmed.trim())?.let { match ->
            val query = match.groupValues[1].trim().trimEnd('.', '?')
            if (query.isNotBlank()) {
                return VoiceCommand.LocalToolAction(
                    WhatsAppToolAction.SearchMessages(
                        query = query,
                        conversationName = null,
                        priority = priorityFromText(lower)
                    )
                )
            }
        }

        return null
    }

    private fun pendingFollowUpsCommand(lower: String): VoiceCommand.LocalToolAction? {
        val normalized = lower.removeSuffix(".").removeSuffix("?").trim()
        val mentionsFollowUp = Regex("""\bfollow[- ]?ups?\b""").containsMatchIn(normalized)
        if (!mentionsFollowUp) {
            return null
        }
        val asksToList = listOf("show", "list", "read", "tell").any { normalized.startsWith(it) } ||
            normalized.contains("pending")
        if (!asksToList) {
            return null
        }
        return VoiceCommand.LocalToolAction(
            WhatsAppToolAction.ListPendingFollowUps(
                limit = 10,
                priority = priorityFromText(normalized)
            )
        )
    }

    private fun actionableInboxCommand(lower: String): VoiceCommand.LocalToolAction? {
        val normalized = lower.removeSuffix(".").removeSuffix("?").trim()
        val asksActionableInbox = normalized.contains("actionable inbox") ||
            normalized.contains("pending important items") ||
            normalized.contains("pending priority items")
        if (!asksActionableInbox) {
            return null
        }
        val status = when {
            normalized.contains("completed") -> "COMPLETED"
            normalized.contains("pending") || normalized.contains("actionable") -> "PENDING"
            else -> null
        }
        return VoiceCommand.LocalToolAction(
            WhatsAppToolAction.GetActionableInbox(
                status = status,
                priority = priorityFromText(normalized),
                limit = 10
            )
        )
    }

    private fun localWriteWorkflowCommand(trimmed: String): VoiceCommand.LocalToolAction? {
        val clean = trimmed.trim()
        createFollowUpRegex.find(clean)?.let { match ->
            val messageEventId = match.groupValues[1].trim()
            val title = match.groupValues[2].trim().trimEnd('.', '?')
            if (messageEventId.isNotBlank() && title.isNotBlank()) {
                return VoiceCommand.LocalToolAction(
                    WhatsAppToolAction.CreateFollowUp(
                        messageEventId = messageEventId,
                        followUpTitle = title,
                        priority = priorityFromText(title.lowercase(Locale.US))
                    )
                )
            }
        }

        reminderRegex.find(clean)?.let { match ->
            val messageEventId = match.groupValues[1].trim()
            val remindAt = match.groupValues[2].trim().trimEnd('.', '?')
            if (messageEventId.isNotBlank() && remindAt.isNotBlank()) {
                return VoiceCommand.LocalToolAction(
                    WhatsAppToolAction.ScheduleReminder(
                        messageEventId = messageEventId,
                        remindAt = remindAt
                    )
                )
            }
        }

        markPriorityRegex.find(clean)?.let { match ->
            val messageEventId = match.groupValues[1].trim()
            val priorityPhrase = match.groupValues[2].lowercase(Locale.US)
            val priority = when {
                priorityPhrase.contains("normal") || priorityPhrase.contains("not important") -> "NORMAL"
                else -> "HIGH"
            }
            if (messageEventId.isNotBlank()) {
                return VoiceCommand.LocalToolAction(
                    WhatsAppToolAction.MarkMessagePriority(
                        messageEventId = messageEventId,
                        priority = priority
                    )
                )
            }
        }

        return null
    }

    private fun priorityFromText(lower: String): String? {
        return when {
            lower.contains("normal priority") ||
                lower.contains("not important") -> "NORMAL"
            lower.contains("high priority") ||
                lower.contains("important") ||
                lower.contains("urgent") -> "HIGH"
            else -> null
        }
    }

    private fun readLatestMessagesCommand(lower: String): VoiceReadCommand? {
        val normalizedRead = lower.removeSuffix(".").removeSuffix("?").trim()
        return when {
            normalizedRead in storedReadPhrases || normalizedRead.isStoredReadIntent() ->
                VoiceCommand.ReadStoredMessages
            normalizedRead in readPhrases || normalizedRead.isRecentWhatsAppReadIntent() ->
                VoiceCommand.ReadLatestMessages
            else -> null
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
        val normalized = lower
            .removeSuffix(".")
            .removeSuffix("?")
            .trim()
            .replace("summarise", "summarize")
        val naturalSummaryIntent = SummaryMessagesRegex.matches(normalized)
        return if (
            normalized in summarizeReadPhrases ||
            normalized.startsWith("summarize whatsapp ") ||
            naturalSummaryIntent
        ) {
            VoiceCommand.SummarizeWhatsAppMessages
        } else {
            null
        }
    }

    private fun summarizeMessagesFromConversationCommand(trimmed: String): VoiceCommand.SummarizeMessagesFromConversation? {
        val match = SummaryFromConversationRegex.find(trimmed.trim()) ?: return null
        val conversationName = match.groupValues[1].trim()
            .removePrefix(":")
            .trim()
        return conversationName
            .takeIf { it.isNotBlank() }
            ?.let { VoiceCommand.SummarizeMessagesFromConversation(it) }
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

    private fun String.isStoredReadIntent(): Boolean {
        val hasReadVerb = readIntentVerbs.any { containsWord(it) }
        val hasStored = containsWord("stored") || containsWord("saved")
        val hasTarget = readIntentTargetWords.any { containsWord(it) }
        return hasReadVerb && hasStored && hasTarget
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
            else -> VoiceCommand.ReplyToLatestActiveMessage(
                replyText = cleaned,
                explicitLatest = prefix.contains("latest")
            )
        }
    }

    private fun replyToNamedConversationCommand(trimmed: String): VoiceCommand? {
        val match = NamedReplyRegex.find(trimmed.trim()) ?: return null
        val conversationName = match.groupValues[1].trim()
        val replyText = match.groupValues[2].trim()
        if (conversationName.isLatestReplyTarget()) {
            return null
        }
        return when {
            conversationName.isEmpty() -> VoiceCommand.Unsupported("Reply target chat cannot be empty.")
            replyText.isEmpty() -> VoiceCommand.Unsupported("Reply text cannot be empty.")
            replyText.length > MaxVoiceReplyCharacters -> VoiceCommand.Unsupported(
                "Reply text is too long (maximum $MaxVoiceReplyCharacters characters)."
            )
            else -> VoiceCommand.ReplyToConversation(
                conversationName = conversationName,
                replyText = replyText
            )
        }
    }

    private fun defaultUnsupportedCommand(): VoiceCommand.Unsupported {
        return VoiceCommand.Unsupported("I can currently read or search captured messages, manage local follow-ups, or reply to the latest active WhatsApp notification.")
    }

    private val NamedReplyRegex = Regex(
        pattern = """^(?:reply to|tell|draft reply to)\s+(.+?)(?:\s+that|:)\s+(.+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val SummaryFromConversationRegex = Regex(
        pattern = """^(?:summarize|summary\s+of|catch\s+me\s+up\s+on)(?:\s+(?:whatsapp\s+)?(?:messages?|notifications?))?\s+from\s+(.+?)[?.]?$""",
        option = RegexOption.IGNORE_CASE
    )

    private val SummaryMessagesRegex = Regex(
        pattern = """^summarize\s+(?:the\s+|my\s+)?(?:whatsapp\s+)?(?:messages?|notifications?)(?:\s+is)?$"""
    )

    private val senderSearchRegex = Regex(
        pattern = """^(?:search|find)(?:\s+(?:whatsapp\s+)?messages?)?\s+from\s+(.+?)\s+(?:for|about)\s+(.+?)[?.]?$""",
        option = RegexOption.IGNORE_CASE
    )

    private val recentSearchRegex = Regex(
        pattern = """^(?:search|find)(?:\s+(?:whatsapp|whatsapp\s+messages|messages))?\s+last\s+(\d+)\s+minutes?\s+(?:for|about)\s+(.+?)[?.]?$""",
        option = RegexOption.IGNORE_CASE
    )

    private val genericSearchRegex = Regex(
        pattern = """^(?:search|find)(?:\s+(?:whatsapp|whatsapp\s+messages|messages))?\s+(?:for|about)\s+(.+?)[?.]?$""",
        option = RegexOption.IGNORE_CASE
    )

    private val createFollowUpRegex = Regex(
        pattern = """^(?:create|add|save)\s+follow[- ]?up\s+for\s+(\S+)\s*:\s*(.+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val reminderRegex = Regex(
        pattern = """^remind\s+me\s+about\s+(\S+)\s+at\s+(.+)$""",
        option = RegexOption.IGNORE_CASE
    )

    private val markPriorityRegex = Regex(
        pattern = """^(?:mark|set)\s+(\S+)\s+(?:as\s+)?(important|urgent|high\s+priority|normal\s+priority|normal|not\s+important)[?.]?$""",
        option = RegexOption.IGNORE_CASE
    )

    private fun String.isLatestReplyTarget(): Boolean {
        val normalized = lowercase(Locale.US).trim()
        return normalized == "it" ||
            normalized == "them" ||
            normalized == "latest" ||
            normalized == "the latest" ||
            normalized == "latest message" ||
            normalized == "the latest message" ||
            normalized == "latest one" ||
            normalized == "the latest one"
    }
}
