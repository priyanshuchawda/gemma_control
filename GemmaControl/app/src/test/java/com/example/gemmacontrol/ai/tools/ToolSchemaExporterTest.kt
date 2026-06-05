package com.example.gemmacontrol.ai.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSchemaExporterTest {

    private val registry = WhatsAppToolRegistry.default()
    private val exporter = ToolSchemaExporter()
    private val json = Json

    @Test
    fun exportsStrictReplyToolAsOpenApiJsonSchema() {
        val schema = exporter.toOpenApiJson(
            registry.require(WhatsAppToolName.SendReplyToActiveWhatsAppNotification.value)
        )

        val root = json.parseToJsonElement(schema).jsonObject
        assertEquals("send_reply_to_active_whatsapp_notification", root["name"]?.jsonPrimitive?.content)
        assertTrue(root["description"]?.jsonPrimitive?.content.orEmpty().contains("strict manual send confirmation"))

        val parameters = root.getValue("parameters").jsonObject
        assertEquals("object", parameters["type"]?.jsonPrimitive?.content)
        val required = parameters.getValue("required").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("notification_key", "message_text"), required)

        val properties = parameters.getValue("properties").jsonObject
        assertEquals("string", properties.getValue("notification_key").jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("string", properties.getValue("message_text").jsonObject["type"]?.jsonPrimitive?.content)
    }

    @Test
    fun exportsNoParameterToolWithoutRequiredList() {
        val schema = exporter.toOpenApiJson(
            registry.require(WhatsAppToolName.PauseWhatsAppCapture.value)
        )

        val parameters = json.parseToJsonElement(schema)
            .jsonObject
            .getValue("parameters")
            .jsonObject

        assertEquals("object", parameters["type"]?.jsonPrimitive?.content)
        assertTrue(parameters.getValue("properties").jsonObject.isEmpty())
        assertFalse(parameters.containsKey("required"))
    }

    @Test
    fun exportsAllRegistryToolsWithUniqueNames() {
        val schemas = exporter.toOpenApiJson(registry)
        val names = schemas.map { schema ->
            json.parseToJsonElement(schema).jsonObject.getValue("name").jsonPrimitive.content
        }

        assertEquals(registry.tools.size, schemas.size)
        assertEquals(names.size, names.toSet().size)
    }
}
