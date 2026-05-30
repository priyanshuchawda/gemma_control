package com.example.gemmacontrol

import androidx.navigation3.runtime.NavKey

enum class AppDestination(
    val label: String,
    val contentDescription: String
) {
    Home(
        label = "Home",
        contentDescription = "Home"
    ),
    Voice(
        label = "Voice",
        contentDescription = "Voice Assistant"
    ),
    Inbox(
        label = "Inbox",
        contentDescription = "Stored Inbox"
    ),
    Settings(
        label = "Settings",
        contentDescription = "Settings"
    );

    fun toNavKey(): NavKey {
        return when (this) {
            Home -> Main
            Voice -> VoiceAssistant
            Inbox -> StoredInbox
            Settings -> AppSettings
        }
    }

    companion object {
        val bottomBarDestinations = listOf(Home, Voice, Inbox, Settings)

        fun fromNavKey(navKey: NavKey): AppDestination {
            return when (navKey) {
                Main -> Home
                VoiceAssistant -> Voice
                StoredInbox -> Inbox
                AppSettings -> Settings
                else -> Home
            }
        }
    }
}
