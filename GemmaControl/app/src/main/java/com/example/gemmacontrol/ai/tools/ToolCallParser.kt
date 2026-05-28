package com.example.gemmacontrol.ai.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ToolCallParser(
    private val registry: WhatsAppToolRegistry
) {
    private val json = Json {
        ignoreUnknownKeys = false
        isLenient = false
    }

    fun parse(rawText: String): ToolCallParseResult {
        val root = try {
            json.parseToJsonElement(rawText).jsonObject
        } catch (e: Exception) {
            return ToolCallParseResult.Invalid("Malformed JSON tool call")
        }

        val envelope = root["functionCall"]?.asObjectOrNull() ?: root
        val name = envelope["name"]?.asStringOrNull()
            ?: return ToolCallParseResult.Invalid("Missing tool name")
        val definition = registry.find(name)
            ?: return ToolCallParseResult.Invalid("Unsupported tool: $name")
        val rawArguments = envelope["parameters"]?.asObjectOrNull()
            ?: envelope["args"]?.asObjectOrNull()
            ?: JsonObject(emptyMap())

        val expectedNames = definition.parameters.map { it.name }.toSet()
        val unknownArgument = rawArguments.keys.firstOrNull { it !in expectedNames }
        if (unknownArgument != null) {
            return ToolCallParseResult.Invalid("Unsupported parameter for $name: $unknownArgument")
        }

        val parsedArguments = mutableMapOf<String, ToolArgument>()
        for (parameter in definition.parameters) {
            val element = rawArguments[parameter.name]
            if (element == null) {
                if (parameter.required) {
                    return ToolCallParseResult.Invalid("Missing required parameter: ${parameter.name}")
                }
                continue
            }
            val parsed = parseArgument(parameter, element)
                ?: return ToolCallParseResult.Invalid("Invalid ${parameter.name}: expected ${parameter.type.name.lowercase()}")
            parsedArguments[parameter.name] = parsed
        }

        return validateProposal(
            ToolProposal(
                name = definition.name,
                arguments = parsedArguments,
                definition = definition
            )
        )
    }

    private fun parseArgument(
        parameter: ToolParameterDefinition,
        element: JsonElement
    ): ToolArgument? {
        val primitive = element as? JsonPrimitive ?: return null
        return when (parameter.type) {
            ToolParameterType.String -> primitive.contentOrNull?.let { ToolArgument.StringValue(it) }
            ToolParameterType.Integer -> primitive.intOrNull?.let { ToolArgument.IntegerValue(it) }
            ToolParameterType.Boolean -> primitive.booleanOrNull?.let { ToolArgument.BooleanValue(it) }
        }
    }

    private fun validateProposal(proposal: ToolProposal): ToolCallParseResult {
        proposal.string("message_text")?.let { messageText ->
            if (messageText.isBlank()) {
                return ToolCallParseResult.Invalid("Invalid message_text: must not be blank")
            }
            if (messageText.length > 1000) {
                return ToolCallParseResult.Invalid("Invalid message_text: maximum 1000 characters")
            }
        }

        proposal.string("phone_number_e164")?.let { phone ->
            if (!E164_REGEX.matches(phone)) {
                return ToolCallParseResult.Invalid("Invalid phone_number_e164: must be E.164 format")
            }
        }

        proposal.integer("limit")?.let { limit ->
            if (limit !in 1..100) {
                return ToolCallParseResult.Invalid("Invalid limit: must be between 1 and 100")
            }
        }

        proposal.string("priority")?.let { priority ->
            if (priority !in setOf("HIGH", "NORMAL", "LOW")) {
                return ToolCallParseResult.Invalid("Invalid priority: expected HIGH, NORMAL, or LOW")
            }
        }

        return ToolCallParseResult.Valid(proposal)
    }

    private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement.asStringOrNull(): String? = (this as? JsonPrimitive)?.jsonPrimitive?.contentOrNull

    private companion object {
        val E164_REGEX = Regex("^\\+[1-9]\\d{7,14}$")
    }
}
