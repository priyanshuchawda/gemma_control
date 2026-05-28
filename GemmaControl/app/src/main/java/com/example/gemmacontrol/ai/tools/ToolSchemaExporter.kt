package com.example.gemmacontrol.ai.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ToolSchemaExporter {

    fun toOpenApiJson(registry: WhatsAppToolRegistry): List<String> {
        return registry.tools.map(::toOpenApiJson)
    }

    fun toOpenApiJson(definition: ToolDefinition): String {
        return buildJsonObject {
            put("name", definition.name.value)
            put("description", "${definition.description} Requires ${definition.safetyLevel.label}.")
            put("parameters", parametersSchema(definition.parameters))
        }.toString()
    }

    private fun parametersSchema(parameters: List<ToolParameterDefinition>): JsonObject {
        return buildJsonObject {
            put("type", "object")
            put(
                "properties",
                JsonObject(
                    parameters.associate { parameter ->
                        parameter.name to parameterSchema(parameter)
                    }
                )
            )
            val required = parameters.filter { it.required }
            if (required.isNotEmpty()) {
                put(
                    "required",
                    JsonArray(required.map { JsonPrimitive(it.name) })
                )
            }
        }
    }

    private fun parameterSchema(parameter: ToolParameterDefinition): JsonObject {
        return buildJsonObject {
            put("type", parameter.type.openApiType)
            put("description", parameter.description)
        }
    }

    private val ToolParameterType.openApiType: String
        get() = when (this) {
            ToolParameterType.String -> "string"
            ToolParameterType.Integer -> "integer"
            ToolParameterType.Boolean -> "boolean"
        }

    private val ToolSafetyLevel.label: String
        get() = when (this) {
            ToolSafetyLevel.ReadOnly -> "read-only execution"
            ToolSafetyLevel.LocalWrite -> "local-write approval"
            ToolSafetyLevel.ConfirmationRequired -> "user confirmation"
            ToolSafetyLevel.StrictManualConfirmation -> "strict manual confirmation"
        }
}
