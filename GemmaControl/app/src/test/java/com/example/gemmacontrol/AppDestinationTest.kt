package com.example.gemmacontrol

import junit.framework.TestCase.assertEquals
import org.junit.Test

class AppDestinationTest {
    @Test
    fun bottomBarDestinations_areOrderedForPrimaryAppFlow() {
        assertEquals(
            listOf(
                AppDestination.Home,
                AppDestination.Voice,
                AppDestination.Inbox,
                AppDestination.Settings
            ),
            AppDestination.bottomBarDestinations
        )
    }

    @Test
    fun fromNavKey_mapsNavigationKeysToTopLevelDestinations() {
        assertEquals(AppDestination.Home, AppDestination.fromNavKey(Main))
        assertEquals(AppDestination.Voice, AppDestination.fromNavKey(VoiceAssistant))
        assertEquals(AppDestination.Inbox, AppDestination.fromNavKey(StoredInbox))
        assertEquals(AppDestination.Settings, AppDestination.fromNavKey(AppSettings))
    }

    @Test
    fun toNavKey_mapsTopLevelDestinationsBackToNavigationKeys() {
        assertEquals(Main, AppDestination.Home.toNavKey())
        assertEquals(VoiceAssistant, AppDestination.Voice.toNavKey())
        assertEquals(StoredInbox, AppDestination.Inbox.toNavKey())
        assertEquals(AppSettings, AppDestination.Settings.toNavKey())
    }
}
