package com.example.gemmacontrol.ai.tools

class WhatsAppToolRegistry private constructor(
    val tools: List<ToolDefinition>
) {
    private val toolsByName = tools.associateBy { it.name.value }

    fun find(toolName: String): ToolDefinition? = toolsByName[toolName]

    fun require(toolName: String): ToolDefinition {
        return find(toolName) ?: error("Unsupported tool: $toolName")
    }

    fun buildSystemPrompt(currentDateTimeIso: String, dayOfWeek: String): String {
        return """
            You are GemmaControl, a private on-device WhatsApp assistant.
            Time: $currentDateTimeIso ($dayOfWeek).
            Use native tools for supported WhatsApp actions.
            Read/search/summarize are safe. Replies, drafts, local writes, capture changes, and deletes need Kotlin confirmation.
            Never claim an action ran until Kotlin reports success.
            If unsure, ask one short clarification.
            Keep reply text concise.
        """.trimIndent()
    }

    companion object {
        fun default(): WhatsAppToolRegistry = WhatsAppToolRegistry(
            listOf(
                ToolDefinition(
                    name = WhatsAppToolName.ListRecentWhatsAppMessages,
                    description = "Return recently captured WhatsApp notification messages from local storage. Use for latest messages, what did I miss, summarize recent WhatsApp notifications, continue reading, or unread chat checks.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(
                        stringParam("conversation_name", required = false, "Optional direct sender or group name filter."),
                        intParam("limit", required = true, "Maximum number of messages to return."),
                        intParam("since_minutes", required = false, "Optional recent-time filter in minutes."),
                        stringParam("read_mode", required = false, "Optional local read mode: latest, unread, chat, summarize, or continue.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.SearchWhatsAppMessages,
                    description = "Search locally stored captured WhatsApp messages by keyword, topic, sender, or time. Use for find requests such as payment, meeting, address, dinner, or other remembered message topics.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(
                        stringParam("query", required = true, "Keyword to search for."),
                        stringParam("conversation_name", required = false, "Optional direct sender or group name filter."),
                        intParam("since_minutes", required = false, "Optional recent-time filter in minutes."),
                        stringParam("priority", required = false, "Optional HIGH or NORMAL priority filter."),
                        stringParam("from_timestamp", required = false, "Optional ISO start boundary."),
                        stringParam("to_timestamp", required = false, "Optional ISO end boundary.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.GetWhatsAppMessageDetails,
                    description = "Retrieve decrypted body and metadata for one stored message event.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(stringParam("message_event_id", required = true, "Stored message row id."))
                ),
                ToolDefinition(
                    name = WhatsAppToolName.GetActionableInbox,
                    description = "Show unresolved or prioritized local inbox items. Use for anything urgent, important messages, pending follow-ups, or messages needing action.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(
                        stringParam("status", required = false, "Optional PENDING or COMPLETED status filter."),
                        stringParam("priority", required = false, "Optional HIGH or NORMAL priority filter."),
                        intParam("limit", required = true, "Maximum number of items.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.CreateFollowUpFromMessage,
                    description = "Create a local follow-up task from a stored WhatsApp message. Use for follow up, todo, task, or remember-to-act requests tied to a captured message.",
                    safetyLevel = ToolSafetyLevel.LocalWrite,
                    parameters = listOf(
                        stringParam("message_event_id", required = true, "Stored message row id."),
                        stringParam("follow_up_title", required = true, "Short task title."),
                        stringParam("due_at", required = false, "Optional ISO deadline."),
                        stringParam("priority", required = false, "Optional HIGH, NORMAL, or LOW priority.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.ListPendingFollowUps,
                    description = "List unresolved local follow-up tasks.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(
                        intParam("limit", required = true, "Maximum number of tasks."),
                        stringParam("priority", required = false, "Optional priority filter.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.MarkFollowUpCompleted,
                    description = "Mark a local follow-up task completed.",
                    safetyLevel = ToolSafetyLevel.LocalWrite,
                    parameters = listOf(stringParam("follow_up_id", required = true, "Follow-up row id."))
                ),
                ToolDefinition(
                    name = WhatsAppToolName.ScheduleReminderForMessage,
                    description = "Schedule a local reminder notification for a stored message. Use for remind me, remind me later, notify me later, or time-based callback requests.",
                    safetyLevel = ToolSafetyLevel.LocalWrite,
                    parameters = listOf(
                        stringParam("message_event_id", required = true, "Stored message row id."),
                        stringParam("remind_at", required = true, "ISO reminder time."),
                        stringParam("reminder_note", required = false, "Optional reminder note.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.MarkMessagePriority,
                    description = "Pin or unpin a local inbox message by priority. Use for urgent, important, high priority, not important, or normal priority requests.",
                    safetyLevel = ToolSafetyLevel.LocalWrite,
                    parameters = listOf(
                        stringParam("message_event_id", required = true, "Stored message row id."),
                        stringParam("priority", required = true, "HIGH or NORMAL.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.DraftWhatsAppReply,
                    description = "Prepare a WhatsApp reply draft for review without sending. Use for reply to named chat, tell Mom, draft a reply, or prepare text for a known conversation.",
                    safetyLevel = ToolSafetyLevel.ConfirmationRequired,
                    parameters = listOf(
                        stringParam("message_event_id", required = false, "Optional stored message row id."),
                        stringParam("conversation_name", required = true, "Target conversation display name."),
                        stringParam("message_text", required = true, "Draft reply text.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.OpenWhatsAppShareDraft,
                    description = "Open WhatsApp share flow with prepared text after user confirmation.",
                    safetyLevel = ToolSafetyLevel.OpenExternalApp,
                    parameters = listOf(stringParam("message_text", required = true, "Draft text to share."))
                ),
                ToolDefinition(
                    name = WhatsAppToolName.OpenWhatsAppClickToChat,
                    description = "Open a verified E.164 WhatsApp click-to-chat draft after user confirmation.",
                    safetyLevel = ToolSafetyLevel.OpenExternalApp,
                    parameters = listOf(
                        stringParam("phone_number_e164", required = true, "Verified E.164 phone number."),
                        stringParam("message_text", required = true, "Draft message text.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.SendReplyToActiveWhatsAppNotification,
                    description = "Send a reply through a live WhatsApp notification after strict manual confirmation.",
                    safetyLevel = ToolSafetyLevel.SendMessage,
                    parameters = listOf(
                        stringParam("notification_key", required = true, "Active Android notification key."),
                        stringParam("message_text", required = true, "Reply text.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.PauseWhatsAppCapture,
                    description = "Turn WhatsApp notification capture off.",
                    safetyLevel = ToolSafetyLevel.ConfirmationRequired
                ),
                ToolDefinition(
                    name = WhatsAppToolName.ResumeWhatsAppCapture,
                    description = "Turn WhatsApp notification capture on.",
                    safetyLevel = ToolSafetyLevel.ConfirmationRequired
                ),
                ToolDefinition(
                    name = WhatsAppToolName.DeleteLocalWhatsAppData,
                    description = "Delete local WhatsApp data after user confirmation.",
                    safetyLevel = ToolSafetyLevel.DeleteData,
                    parameters = listOf(
                        boolParam("delete_all", required = true, "Whether to delete all local data."),
                        stringParam("conversation_name", required = false, "Optional conversation filter.")
                    )
                )
            )
        )

        private fun stringParam(name: String, required: Boolean, description: String) =
            ToolParameterDefinition(name, ToolParameterType.String, required, description)

        private fun intParam(name: String, required: Boolean, description: String) =
            ToolParameterDefinition(name, ToolParameterType.Integer, required, description)

        private fun boolParam(name: String, required: Boolean, description: String) =
            ToolParameterDefinition(name, ToolParameterType.Boolean, required, description)
    }
}
