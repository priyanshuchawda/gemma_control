package com.example.gemmacontrol.ui.main

enum class AssistantInputSource {
    Voice,
    Typed
}

sealed interface AssistantPlan {
    data class ReadCommand(val command: VoiceReadCommand) : AssistantPlan
    data class ReplyCommand(val command: VoiceCommand.ReplyToLatestActiveMessage) : AssistantPlan
    data class NamedReplyCommand(val command: VoiceCommand.ReplyToConversation) : AssistantPlan
    data class RequestModelProposal(
        val transcript: String,
        val fallbackState: VoiceAssistantState
    ) : AssistantPlan
    data class AskClarification(val prompt: String) : AssistantPlan
}

class AssistantPlanner(
    private val parser: (String) -> VoiceCommand = VoiceCommandParser::parse
) {
    fun plan(
        text: String,
        source: AssistantInputSource = AssistantInputSource.Voice
    ): AssistantPlan {
        val transcript = text.trim()
        if (transcript.isBlank()) {
            return AssistantPlan.AskClarification("Type or say a WhatsApp command to continue.")
        }

        return when (val command = parser(transcript)) {
            is VoiceReadCommand -> AssistantPlan.ReadCommand(command)
            is VoiceCommand.ReplyToLatestActiveMessage -> AssistantPlan.ReplyCommand(command)
            is VoiceCommand.ReplyToConversation -> AssistantPlan.NamedReplyCommand(command)
            is VoiceCommand.Unsupported -> unsupportedPlan(transcript, command)
        }
    }

    private fun unsupportedPlan(
        transcript: String,
        command: VoiceCommand.Unsupported
    ): AssistantPlan {
        return when (command.reason) {
            "Reply text cannot be empty.",
            "Sending WhatsApp audio voice notes is not supported yet. You can dictate a text reply instead." ->
                AssistantPlan.AskClarification(command.reason)
            else -> AssistantPlan.RequestModelProposal(
                transcript = transcript,
                fallbackState = VoiceAssistantState.ClarificationRequired(command.fallbackClarification())
            )
        }
    }

    private fun VoiceCommand.Unsupported.fallbackClarification(): String {
        return when (reason) {
            "I can currently read captured messages or reply to the latest active WhatsApp notification." ->
                GenericWhatsAppClarification
            else -> reason
        }
    }

    private companion object {
        const val GenericWhatsAppClarification =
            "I can help with locally stored WhatsApp messages and active notification replies. " +
                "Try: read my latest stored messages, summarize WhatsApp, read messages from a chat, or reply to the latest message."
    }
}
