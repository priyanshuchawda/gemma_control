package com.example.gemmacontrol.ai.safety

import com.example.gemmacontrol.ai.media.MediaUnderstandingDecision
import com.example.gemmacontrol.ai.tools.ToolSafetyLevel

enum class AssistantOperationSafetyLevel {
    ReadOnly,
    LocalWrite,
    ConfirmationRequired,
    OpenExternalApp,
    SendMessage,
    DeleteData,
    MediaAnalysis,
    Reject
}

enum class SafetyModelGate {
    NotNeededForV1,
    FutureTextModerationIfGeneratedRepliesAreAdded,
    FutureShieldGemma2ImageGate
}

data class AssistantSafetyPolicyDecision(
    val level: AssistantOperationSafetyLevel,
    val requiresUserConfirmation: Boolean,
    val requiresActiveNotification: Boolean,
    val deterministicKotlinGate: Boolean,
    val modelGate: SafetyModelGate,
    val reason: String
)

object AssistantSafetyPolicy {

    fun forToolSafetyLevel(safetyLevel: ToolSafetyLevel): AssistantSafetyPolicyDecision {
        return when (safetyLevel) {
            ToolSafetyLevel.ReadOnly -> AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.ReadOnly,
                requiresUserConfirmation = false,
                requiresActiveNotification = false,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.NotNeededForV1,
                reason = "Read-only local data access; Kotlin still validates capability and bounded output."
            )
            ToolSafetyLevel.LocalWrite -> AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.LocalWrite,
                requiresUserConfirmation = false,
                requiresActiveNotification = false,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.NotNeededForV1,
                reason = "Local app data mutation only; no external message or app action."
            )
            ToolSafetyLevel.ConfirmationRequired -> AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.ConfirmationRequired,
                requiresUserConfirmation = true,
                requiresActiveNotification = false,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.NotNeededForV1,
                reason = "Model-proposed app state or draft action requires visible user confirmation."
            )
            ToolSafetyLevel.OpenExternalApp -> AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.OpenExternalApp,
                requiresUserConfirmation = true,
                requiresActiveNotification = false,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.NotNeededForV1,
                reason = "Opening WhatsApp or another app requires confirmation and intent validation."
            )
            ToolSafetyLevel.SendMessage -> AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.SendMessage,
                requiresUserConfirmation = true,
                requiresActiveNotification = true,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.NotNeededForV1,
                reason = "External send requires strict manual confirmation and a live RemoteInput target."
            )
            ToolSafetyLevel.DeleteData -> AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.DeleteData,
                requiresUserConfirmation = true,
                requiresActiveNotification = false,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.NotNeededForV1,
                reason = "Local data deletion requires strict manual confirmation and explicit delete intent."
            )
        }
    }

    fun forMediaUnderstanding(decision: MediaUnderstandingDecision): AssistantSafetyPolicyDecision {
        return if (decision.canInspectMediaBytes) {
            AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.MediaAnalysis,
                requiresUserConfirmation = true,
                requiresActiveNotification = false,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.FutureShieldGemma2ImageGate,
                reason = "Actual user-granted media analysis is future-only and needs confirmation plus image safety review."
            )
        } else {
            AssistantSafetyPolicyDecision(
                level = AssistantOperationSafetyLevel.Reject,
                requiresUserConfirmation = false,
                requiresActiveNotification = false,
                deterministicKotlinGate = true,
                modelGate = SafetyModelGate.NotNeededForV1,
                reason = "No media bytes are available, so model-based media analysis is rejected."
            )
        }
    }

    fun textModerationGateForV1(): SafetyModelGate {
        return SafetyModelGate.NotNeededForV1
    }
}
