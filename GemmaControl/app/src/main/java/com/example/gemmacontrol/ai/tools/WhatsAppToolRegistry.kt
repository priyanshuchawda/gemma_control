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
        val toolLines = tools.joinToString(separator = "\n") { tool ->
            val params = if (tool.parameters.isEmpty()) {
                "no parameters"
            } else {
                tool.parameters.joinToString { param ->
                    val marker = if (param.required) "required" else "optional"
                    "${param.name}:${param.type.name.lowercase()}:$marker"
                }
            }
            "- ${tool.name.value}: ${tool.description} Parameters: $params. Safety: ${tool.safetyLevel}."
        }

        return """
            You are GemmaControl, a private on-device WhatsApp notification assistant.
            Current date and time: $currentDateTimeIso. Day of week: $dayOfWeek.
            Use the native LiteRT-LM WhatsApp tools when a supported action is needed.
            Never execute tools yourself. Never claim that a reply was sent, data was deleted, capture was changed, or WhatsApp was opened unless Kotlin reports success after user confirmation.
            Use English only. Keep message_text under 1000 characters.
            Tool registry:
            $toolLines
        """.trimIndent()
    }

    companion object {
        fun default(): WhatsAppToolRegistry = WhatsAppToolRegistry(
            listOf(
                ToolDefinition(
                    name = WhatsAppToolName.ListRecentWhatsAppMessages,
                    description = "Return recently captured WhatsApp notification messages from local storage.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(
                        stringParam("conversation_name", required = false, "Optional direct sender or group name filter."),
                        intParam("limit", required = true, "Maximum number of messages to return."),
                        intParam("since_minutes", required = false, "Optional recent-time filter in minutes.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.SearchWhatsAppMessages,
                    description = "Search locally stored captured WhatsApp messages by keyword.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(
                        stringParam("query", required = true, "Keyword to search for."),
                        stringParam("conversation_name", required = false, "Optional direct sender or group name filter."),
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
                    description = "Show unresolved or prioritized local inbox items.",
                    safetyLevel = ToolSafetyLevel.ReadOnly,
                    parameters = listOf(
                        stringParam("status", required = false, "Optional PENDING or COMPLETED status filter."),
                        stringParam("priority", required = false, "Optional HIGH or NORMAL priority filter."),
                        intParam("limit", required = true, "Maximum number of items.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.CreateFollowUpFromMessage,
                    description = "Create a local follow-up task from a stored WhatsApp message.",
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
                    description = "Schedule a local reminder notification for a stored message.",
                    safetyLevel = ToolSafetyLevel.LocalWrite,
                    parameters = listOf(
                        stringParam("message_event_id", required = true, "Stored message row id."),
                        stringParam("remind_at", required = true, "ISO reminder time."),
                        stringParam("reminder_note", required = false, "Optional reminder note.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.MarkMessagePriority,
                    description = "Pin or unpin a local inbox message by priority.",
                    safetyLevel = ToolSafetyLevel.LocalWrite,
                    parameters = listOf(
                        stringParam("message_event_id", required = true, "Stored message row id."),
                        stringParam("priority", required = true, "HIGH or NORMAL.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.DraftWhatsAppReply,
                    description = "Prepare a WhatsApp reply draft for review without sending.",
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
                    safetyLevel = ToolSafetyLevel.ConfirmationRequired,
                    parameters = listOf(stringParam("message_text", required = true, "Draft text to share."))
                ),
                ToolDefinition(
                    name = WhatsAppToolName.OpenWhatsAppClickToChat,
                    description = "Open a verified E.164 WhatsApp click-to-chat draft after user confirmation.",
                    safetyLevel = ToolSafetyLevel.ConfirmationRequired,
                    parameters = listOf(
                        stringParam("phone_number_e164", required = true, "Verified E.164 phone number."),
                        stringParam("message_text", required = true, "Draft message text.")
                    )
                ),
                ToolDefinition(
                    name = WhatsAppToolName.SendReplyToActiveWhatsAppNotification,
                    description = "Send a reply through a live WhatsApp notification after strict manual confirmation.",
                    safetyLevel = ToolSafetyLevel.StrictManualConfirmation,
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
                    safetyLevel = ToolSafetyLevel.ConfirmationRequired,
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
