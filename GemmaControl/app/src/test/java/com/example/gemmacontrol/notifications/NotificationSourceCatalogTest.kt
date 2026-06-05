package com.example.gemmacontrol.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSourceCatalogTest {

    @Test
    fun whatsappPackagesAreTheOnlyEnabledProductionCaptureSources() {
        NotificationSourceCatalog.whatsappPackageNames.forEach { packageName ->
            val source = NotificationSourceCatalog.classifyPackage(packageName)

            assertEquals(NotificationSourceType.WHATSAPP, source.type)
            assertEquals(NotificationSourceImplementationStatus.ENABLED, source.implementationStatus)
            assertTrue(NotificationSourceCatalog.isProductionCaptureEnabled(packageName))
            assertTrue(NotificationSourceCatalog.canUseActiveNotificationReply(packageName))
        }

        val enabledReadSources = NotificationSourceCatalog.enabledSourcesFor(NotificationQueryMode.READ)
        assertEquals(setOf(NotificationSourceType.WHATSAPP), enabledReadSources)
    }

    @Test
    fun plannedSourcesAreRecognizedButHaveNoEnabledCaptureOrReply() {
        val plannedPackages = mapOf(
            "com.google.android.apps.messaging" to NotificationSourceType.SMS,
            "com.google.android.gm" to NotificationSourceType.GMAIL,
            "com.google.android.dialer" to NotificationSourceType.PHONE,
            "com.google.android.calendar" to NotificationSourceType.CALENDAR
        )

        plannedPackages.forEach { (packageName, expectedType) ->
            val source = NotificationSourceCatalog.classifyPackage(packageName)

            assertEquals(expectedType, source.type)
            assertEquals(NotificationSourceImplementationStatus.PLANNED, source.implementationStatus)
            assertFalse(NotificationSourceCatalog.isProductionCaptureEnabled(packageName))
            assertFalse(NotificationSourceCatalog.canUseActiveNotificationReply(packageName))
            assertTrue(source.enabledActions.isEmpty())
            assertTrue(source.plannedActions.isNotEmpty())
        }
    }

    @Test
    fun unknownPackagesRemainUnsupportedAndNotReplyCapable() {
        val source = NotificationSourceCatalog.classifyPackage("com.example.random")

        assertEquals(NotificationSourceType.OTHER, source.type)
        assertEquals(NotificationSourceImplementationStatus.UNSUPPORTED, source.implementationStatus)
        assertFalse(NotificationSourceCatalog.isProductionCaptureEnabled("com.example.random"))
        assertFalse(NotificationSourceCatalog.canUseActiveNotificationReply("com.example.random"))
    }

    @Test
    fun whatsappParserUsesGenericSourceCatalogForSupportCheck() {
        assertTrue(WhatsAppNotificationParser.isPackageSupported("com.whatsapp"))
        assertFalse(WhatsAppNotificationParser.isPackageSupported("com.google.android.gm"))
        assertFalse(WhatsAppNotificationParser.isPackageSupported("com.google.android.apps.messaging"))
    }

    @Test
    fun parsedWhatsAppEventProjectsIntoGenericNotificationEvent() {
        val event = ParsedWhatsAppNotificationEvent(
            eventType = NotificationEventType.POSTED,
            notificationKey = "key-1",
            packageName = "com.whatsapp",
            observedAt = 100L,
            notificationPostedAt = 90L,
            conversationTitle = "Test Chat",
            conversationType = ConversationType.DIRECT,
            messages = listOf(
                ParsedMessagePreview(
                    senderName = "Test Chat",
                    messageText = "Photo",
                    timestamp = 90L,
                    contentKind = WhatsAppContentKind.PHOTO
                )
            ),
            currentMessageCount = 1,
            historicMessageCount = 0,
            hasReplyActionAtCaptureTime = true,
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            isContentUnavailable = false,
            dedupeCandidate = "dedupe",
            isCurrentlyActive = true
        )

        val generic = event.toGenericNotificationEvent()

        assertEquals(NotificationSourceType.WHATSAPP, generic.source.type)
        assertEquals(NotificationContentState.MEDIA_PLACEHOLDER, generic.messages.single().contentState)
        assertTrue(generic.hasReplyActionAtCaptureTime)
    }

    @Test
    fun genericQueryScopesCanRepresentFutureReadSummarizeSearchWithoutEnablingSources() {
        val readSmsScope = NotificationQueryScope(
            mode = NotificationQueryMode.READ,
            sourceTypes = setOf(NotificationSourceType.SMS),
            conversationName = "Test Sender",
            limit = 5
        )
        val searchGmailScope = NotificationQueryScope(
            mode = NotificationQueryMode.SEARCH,
            sourceTypes = setOf(NotificationSourceType.GMAIL),
            query = "invoice",
            sinceMinutes = 1440,
            limit = 10
        )

        assertEquals(NotificationQueryMode.READ, readSmsScope.mode)
        assertEquals(setOf(NotificationSourceType.SMS), readSmsScope.sourceTypes)
        assertEquals("invoice", searchGmailScope.query)
        assertFalse(NotificationSourceCatalog.enabledSourcesFor(NotificationQueryMode.SEARCH).contains(NotificationSourceType.GMAIL))
    }
}
