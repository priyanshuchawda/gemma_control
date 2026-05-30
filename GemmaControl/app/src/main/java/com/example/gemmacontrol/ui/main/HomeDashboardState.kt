package com.example.gemmacontrol.ui.main

import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent

enum class HomeModelReadiness(
    val label: String,
    val description: String
) {
    Ready(
        label = "Ready",
        description = "FunctionGemma model file is installed locally."
    ),
    Missing(
        label = "Missing",
        description = "Install MobileActions-270M in Settings before model proposals can run."
    )
}

data class HomeDashboardData(
    val notifications: List<ParsedWhatsAppNotificationEvent>,
    val isPermissionGranted: Boolean,
    val storedMessageCount: Int,
    val actionableItemCount: Int,
    val modelReadiness: HomeModelReadiness
)

sealed interface HomeDashboardScreenState {
    data object Loading : HomeDashboardScreenState
}

data class HomeDashboardReadyState(
    val notifications: List<ParsedWhatsAppNotificationEvent>,
    val summary: HomeDashboardSummary,
    val storedMessageCount: Int,
    val actionableItemCount: Int,
    val modelReadiness: HomeModelReadiness,
    val modelReadinessLabel: String
) : HomeDashboardScreenState

fun buildHomeDashboardReadyState(data: HomeDashboardData): HomeDashboardReadyState {
    return HomeDashboardReadyState(
        notifications = data.notifications,
        summary = buildHomeDashboardSummary(
            notifications = data.notifications,
            isPermissionGranted = data.isPermissionGranted
        ),
        storedMessageCount = data.storedMessageCount,
        actionableItemCount = data.actionableItemCount,
        modelReadiness = data.modelReadiness,
        modelReadinessLabel = data.modelReadiness.label
    )
}
