package com.example.gemmacontrol.ai.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallParserTest {

    private val parser = ToolCallParser(WhatsAppToolRegistry.default())

    @Test
    fun parsesFunctionGemmaNameAndParametersShape() {
        val result = parser.parse(
            """
            {
              "name": "send_reply_to_active_whatsapp_notification",
              "parameters": {
                "notification_key": "active-key-1",
                "message_text": "I am in a meeting"
              }
            }
            """.trimIndent()
        )

        assertTrue(result is ToolCallParseResult.Valid)
        val proposal = (result as ToolCallParseResult.Valid).proposal
        assertEquals(WhatsAppToolName.SendReplyToActiveWhatsAppNotification, proposal.name)
        assertEquals("active-key-1", proposal.string("notification_key"))
        assertEquals("I am in a meeting", proposal.string("message_text"))
        assertEquals(ToolSafetyLevel.StrictManualConfirmation, proposal.definition.safetyLevel)
    }

    @Test
    fun parsesGalleryStyleFunctionCallEnvelope() {
        val result = parser.parse(
            """
            {
              "functionCall": {
                "name": "open_whatsapp_click_to_chat",
                "args": {
                  "phone_number_e164": "+15551234567",
                  "message_text": "Running late"
                }
              }
            }
            """.trimIndent()
        )

        assertTrue(result is ToolCallParseResult.Valid)
        val proposal = (result as ToolCallParseResult.Valid).proposal
        assertEquals(WhatsAppToolName.OpenWhatsAppClickToChat, proposal.name)
        assertEquals("+15551234567", proposal.string("phone_number_e164"))
    }

    @Test
    fun rejectsUnknownToolsAndMissingRequiredParameters() {
        val unknown = parser.parse("""{"name":"turnOnFlashlight","parameters":{}}""")
        assertEquals(
            ToolCallParseResult.Invalid("Unsupported tool: turnOnFlashlight"),
            unknown
        )

        val missing = parser.parse("""{"name":"draft_whatsapp_reply","parameters":{"conversation_name":"Mom"}}""")
        assertEquals(
            ToolCallParseResult.Invalid("Missing required parameter: message_text"),
            missing
        )
    }

    @Test
    fun rejectsUnsafeParameterValues() {
        val badPhone = parser.parse(
            """
            {
              "name": "open_whatsapp_click_to_chat",
              "parameters": {
                "phone_number_e164": "5551234567",
                "message_text": "Hi"
              }
            }
            """.trimIndent()
        )

        assertEquals(
            ToolCallParseResult.Invalid("Invalid phone_number_e164: must be E.164 format"),
            badPhone
        )

        val emptyReply = parser.parse(
            """
            {
              "name": "send_reply_to_active_whatsapp_notification",
              "parameters": {
                "notification_key": "active-key-1",
                "message_text": "   "
              }
            }
            """.trimIndent()
        )

        assertEquals(
            ToolCallParseResult.Invalid("Invalid message_text: must not be blank"),
            emptyReply
        )

        val badStatus = parser.parse(
            """
            {
              "name": "get_actionable_inbox",
              "parameters": {
                "status": "OPEN",
                "limit": 10
              }
            }
            """.trimIndent()
        )

        assertEquals(
            ToolCallParseResult.Invalid("Invalid status: expected PENDING or COMPLETED"),
            badStatus
        )
    }
}
