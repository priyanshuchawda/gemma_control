package com.example.gemmacontrol.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiReliabilityDiagnosticsTest {

    @Test
    fun disabledNotificationListenerBlocksCapture() {
        val state = buildXiaomiReliabilityDiagnosticState(
            notificationListenerEnabled = false,
            postNotificationsGranted = true,
            microphoneGranted = true,
            latestEventObservedAt = 1_000L,
            nowMillis = 2_000L
        )

        assertEquals(XiaomiReliabilityLevel.Blocked, state.level)
        assertEquals("Notification access disabled", state.title)
        assertTrue(state.summary.contains("Notification Listener Access"))
    }

    @Test
    fun enabledListenerWithoutEventsShowsRecentEventWarning() {
        val state = buildXiaomiReliabilityDiagnosticState(
            notificationListenerEnabled = true,
            postNotificationsGranted = true,
            microphoneGranted = true,
            latestEventObservedAt = null,
            nowMillis = 2_000L
        )

        assertEquals(XiaomiReliabilityLevel.Warning, state.level)
        assertEquals("No WhatsApp events captured yet", state.title)
        assertTrue(state.summary.contains("send a test WhatsApp message"))
    }

    @Test
    fun staleListenerEventShowsIdleWarning() {
        val state = buildXiaomiReliabilityDiagnosticState(
            notificationListenerEnabled = true,
            postNotificationsGranted = true,
            microphoneGranted = true,
            latestEventObservedAt = 0L,
            nowMillis = 3 * 60 * 60 * 1000L
        )

        assertEquals(XiaomiReliabilityLevel.Warning, state.level)
        assertEquals("No recent WhatsApp listener events", state.title)
        assertEquals(180L, state.latestEventAgeMinutes)
    }

    @Test
    fun allSignalsReadyShowsHealthyState() {
        val state = buildXiaomiReliabilityDiagnosticState(
            notificationListenerEnabled = true,
            postNotificationsGranted = true,
            microphoneGranted = true,
            latestEventObservedAt = 60_000L,
            nowMillis = 120_000L
        )

        assertEquals(XiaomiReliabilityLevel.Ready, state.level)
        assertEquals("Xiaomi background capture looks ready", state.title)
        assertEquals(1L, state.latestEventAgeMinutes)
    }
}
