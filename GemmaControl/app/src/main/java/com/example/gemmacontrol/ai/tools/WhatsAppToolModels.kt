package com.example.gemmacontrol.ai.tools

enum class ToolSafetyLevel {
    ReadOnly,
    LocalWrite,
    ConfirmationRequired,
    StrictManualConfirmation
}

enum class ToolParameterType {
    String,
    Integer,
    Boolean
}

enum class WhatsAppToolName(val value: String) {
    ListRecentWhatsAppMessages("list_recent_whatsapp_messages"),
    SearchWhatsAppMessages("search_whatsapp_messages"),
    GetWhatsAppMessageDetails("get_whatsapp_message_details"),
    GetActionableInbox("get_actionable_inbox"),
    CreateFollowUpFromMessage("create_follow_up_from_message"),
    ListPendingFollowUps("list_pending_follow_ups"),
    MarkFollowUpCompleted("mark_follow_up_completed"),
    ScheduleReminderForMessage("schedule_reminder_for_message"),
    MarkMessagePriority("mark_message_priority"),
    DraftWhatsAppReply("draft_whatsapp_reply"),
    OpenWhatsAppShareDraft("open_whatsapp_share_draft"),
    OpenWhatsAppClickToChat("open_whatsapp_click_to_chat"),
    SendReplyToActiveWhatsAppNotification("send_reply_to_active_whatsapp_notification"),
    PauseWhatsAppCapture("pause_whatsapp_capture"),
    ResumeWhatsAppCapture("resume_whatsapp_capture"),
    DeleteLocalWhatsAppData("delete_local_whatsapp_data");

    companion object {
        fun fromValue(value: String): WhatsAppToolName? = entries.firstOrNull { it.value == value }
    }
}

data class ToolParameterDefinition(
    val name: String,
    val type: ToolParameterType,
    val required: Boolean,
    val description: String
)

data class ToolDefinition(
    val name: WhatsAppToolName,
    val description: String,
    val safetyLevel: ToolSafetyLevel,
    val parameters: List<ToolParameterDefinition> = emptyList()
)

sealed interface ToolArgument {
    data class StringValue(val value: String) : ToolArgument
    data class IntegerValue(val value: Int) : ToolArgument
    data class BooleanValue(val value: Boolean) : ToolArgument
}

data class ToolProposal(
    val name: WhatsAppToolName,
    val arguments: Map<String, ToolArgument>,
    val definition: ToolDefinition
) {
    fun string(parameterName: String): String? = (arguments[parameterName] as? ToolArgument.StringValue)?.value
    fun integer(parameterName: String): Int? = (arguments[parameterName] as? ToolArgument.IntegerValue)?.value
    fun boolean(parameterName: String): Boolean? = (arguments[parameterName] as? ToolArgument.BooleanValue)?.value
}

sealed interface ToolCallParseResult {
    data class Valid(val proposal: ToolProposal) : ToolCallParseResult
    data class Invalid(val reason: String) : ToolCallParseResult
}
