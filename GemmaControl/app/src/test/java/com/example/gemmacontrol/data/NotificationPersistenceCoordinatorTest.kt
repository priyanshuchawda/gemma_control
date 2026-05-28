package com.example.gemmacontrol.data

import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.crypto.MessageBodyCipher
import com.example.gemmacontrol.data.local.dao.ActiveNotificationReferenceDao
import com.example.gemmacontrol.data.local.dao.ConversationDao
import com.example.gemmacontrol.data.local.dao.MessageEventDao
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import com.example.gemmacontrol.data.local.entity.ConversationEntity
import com.example.gemmacontrol.data.local.entity.MessageEventEntity
import com.example.gemmacontrol.data.preferences.CapturePreferencesRepository
import com.example.gemmacontrol.data.repository.NotificationPersistenceCoordinator
import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedMessagePreview
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import com.example.gemmacontrol.notifications.WhatsAppNotificationParser
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

// ─── Cryptography Fake ────────────────────────────────────────────────────────
class FakeMessageBodyCipher : MessageBodyCipher {
    override fun encrypt(plaintext: String): EncryptedPayload {
        return EncryptedPayload(plaintext.toByteArray(), byteArrayOf(1, 2, 3))
    }

    override fun decrypt(payload: EncryptedPayload): String {
        return String(payload.ciphertext)
    }
}

// ─── Preferences Fake ─────────────────────────────────────────────────────────
class FakeCapturePreferencesRepository : CapturePreferencesRepository {
    override val captureEnabledFlow = MutableStateFlow(true)
    override val storageEnabledFlow = MutableStateFlow(false)
    override val storageEnabledAtFlow = MutableStateFlow(0L)

    override suspend fun setCaptureEnabled(enabled: Boolean) {
        captureEnabledFlow.value = enabled
    }

    override suspend fun setStorageEnabled(enabled: Boolean) {
        storageEnabledFlow.value = enabled
        if (enabled) {
            // Set to 1L in the fake so that mock messages (with timestamp 1716900000000L) are always post-consent
            storageEnabledAtFlow.value = 1L
        } else {
            storageEnabledAtFlow.value = 0L
        }
    }
}

// ─── DAO Fakes ────────────────────────────────────────────────────────────────
class FakeConversationDao : ConversationDao {
    val list = mutableListOf<ConversationEntity>()
    override suspend fun insert(conversation: ConversationEntity): Long {
        list.add(conversation)
        return list.size.toLong()
    }
    override suspend fun update(conversation: ConversationEntity) {
        val idx = list.indexOfFirst { it.id == conversation.id }
        if (idx != -1) list[idx] = conversation
    }
    override suspend fun getById(id: String): ConversationEntity? = list.find { it.id == id }
    override fun getAllConversationsFlow(): Flow<List<ConversationEntity>> = flow { emit(list) }
    override suspend fun getAllConversations(): List<ConversationEntity> = list
    override suspend fun deleteAll() = list.clear()
}

class FakeMessageEventDao(
    private val conversationDao: FakeConversationDao,
    private val activeRefDao: FakeActiveNotificationReferenceDao
) : MessageEventDao {
    val list = mutableListOf<MessageEventEntity>()
    override suspend fun insert(message: MessageEventEntity): Long {
        if (list.any { it.dedupeHash == message.dedupeHash }) {
            return -1L // Room OnConflictStrategy.IGNORE behavior
        }
        list.add(message)
        return list.size.toLong()
    }
    override suspend fun getByDedupeHash(dedupeHash: String): MessageEventEntity? = list.find { it.dedupeHash == dedupeHash }
    override fun getAllMessagesFlow(): Flow<List<MessageEventEntity>> = flow { emit(list) }
    override suspend fun getAllMessages(): List<MessageEventEntity> = list
    override fun getMessagesForConversationFlow(conversationId: String): Flow<List<MessageEventEntity>> = flow { emit(list.filter { it.conversationId == conversationId }) }
    override suspend fun getMessagesForConversation(conversationId: String): List<MessageEventEntity> = list.filter { it.conversationId == conversationId }
    override suspend fun deleteAll() = list.clear()

    override suspend fun deleteConversations() {
        conversationDao.deleteAll()
    }

    override suspend fun deleteActiveReferences() {
        activeRefDao.deleteAll()
    }

    override suspend fun deleteAllData() {
        deleteAll()
        deleteConversations()
        deleteActiveReferences()
    }
}

class FakeActiveNotificationReferenceDao : ActiveNotificationReferenceDao {
    val list = mutableListOf<ActiveNotificationReferenceEntity>()
    override suspend fun insertOrUpdate(reference: ActiveNotificationReferenceEntity) {
        list.removeAll { it.notificationKey == reference.notificationKey }
        list.add(reference)
    }
    override suspend fun getByKey(notificationKey: String): ActiveNotificationReferenceEntity? = list.find { it.notificationKey == notificationKey }
    override fun getActiveReferencesFlow(): Flow<List<ActiveNotificationReferenceEntity>> = flow { emit(list.filter { it.removedAt == null }) }
    override suspend fun getActiveReferences(): List<ActiveNotificationReferenceEntity> = list.filter { it.removedAt == null }
    override suspend fun deleteAll() = list.clear()
}

// ─── Test Suite ───────────────────────────────────────────────────────────────
class NotificationPersistenceCoordinatorTest {

    private lateinit var conversationDao: FakeConversationDao
    private lateinit var messageEventDao: FakeMessageEventDao
    private lateinit var activeNotificationReferenceDao: FakeActiveNotificationReferenceDao
    private lateinit var messageBodyCipher: FakeMessageBodyCipher
    private lateinit var preferencesRepository: FakeCapturePreferencesRepository
    private lateinit var repository: StoredInboxRepository
    private lateinit var coordinator: NotificationPersistenceCoordinator

    @Before
    fun setUp() {
        conversationDao = FakeConversationDao()
        activeNotificationReferenceDao = FakeActiveNotificationReferenceDao()
        messageEventDao = FakeMessageEventDao(conversationDao, activeNotificationReferenceDao)
        messageBodyCipher = FakeMessageBodyCipher()
        preferencesRepository = FakeCapturePreferencesRepository()
        
        repository = StoredInboxRepository(
            conversationDao,
            messageEventDao,
            activeNotificationReferenceDao,
            messageBodyCipher
        )

        coordinator = NotificationPersistenceCoordinator(
            repository,
            preferencesRepository,
            activeNotificationReferenceDao
        )
    }

    @Test
    fun testSettingsDefaultToDisabled() = runTest {
        // captureEnabled = true
        assertTrue(preferencesRepository.captureEnabledFlow.first())
        // storageEnabled = false by default
        assertFalse(preferencesRepository.storageEnabledFlow.first())
    }

    @Test
    fun testCoordinatorSkipsPersistenceWhenStorageDisabled() = runTest {
        val event = createDummyEvent(
            key = "key_test",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Aunt May",
            text = "Dinner at 7"
        )

        // storageEnabled is false by default
        coordinator.handleNotificationEvent(event)

        // Verify nothing was persisted
        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(conversationDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())
    }

    @Test
    fun testCoordinatorPersistsCanonicalMessagingStyleEvent() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val event = createDummyEvent(
            key = "key_test",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Aunt May",
            text = "Dinner at 7"
        )

        coordinator.handleNotificationEvent(event)

        // Verify conversation and message are persisted
        assertEquals(1, conversationDao.list.size)
        assertEquals("Aunt May", conversationDao.list.first().id)
        assertEquals(1, messageEventDao.list.size)
        assertEquals("Dinner at 7", String(messageEventDao.list.first().encryptedMessageText!!))
        assertEquals(1, activeNotificationReferenceDao.list.size)
    }

    @Test
    fun testIngestionPolicy_fallbackOnlyEventDoesNotPersist() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val event = createDummyEvent(
            key = "key_fallback_only",
            source = NotificationParseSource.EXTRAS_FALLBACK,
            title = "Aunt May",
            text = "Dinner at 7"
        )

        coordinator.handleNotificationEvent(event)

        // Under the new policy, fallback events remain volatile-only in debug feed
        // and are NOT written to Room storage.
        assertTrue(messageEventDao.list.isEmpty())
    }

    @Test
    fun testIngestionPolicy_messagingStylePlusFallbackWithSameKeyPersistsOneRow() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val key = "key_same"

        // 1. MESSAGING_STYLE event
        val canonicalEvent = createDummyEvent(
            key = key,
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Spidey Chat",
            text = "Hey!"
        )
        coordinator.handleNotificationEvent(canonicalEvent)

        // 2. EXTRAS_FALLBACK summary event with same key
        val fallbackEvent = createDummyEvent(
            key = key,
            source = NotificationParseSource.EXTRAS_FALLBACK,
            title = "Spidey Chat",
            text = "Hey!"
        )
        coordinator.handleNotificationEvent(fallbackEvent)

        assertEquals(1, messageEventDao.list.size)
    }

    @Test
    fun testIngestionPolicy_messagingStylePlusFallbackWithDifferentKeysPersistsOneRow() = runTest {
        preferencesRepository.setStorageEnabled(true)

        // 1. MESSAGING_STYLE event with key "key_canonical"
        val canonicalEvent = createDummyEvent(
            key = "key_canonical",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Spidey Chat",
            text = "Hey!"
        )
        coordinator.handleNotificationEvent(canonicalEvent)

        // 2. EXTRAS_FALLBACK summary event with different key "key_fallback"
        val fallbackEvent = createDummyEvent(
            key = "key_fallback",
            source = NotificationParseSource.EXTRAS_FALLBACK,
            title = "Spidey Chat",
            text = "Hey!"
        )
        coordinator.handleNotificationEvent(fallbackEvent)

        // Fallback is ignored, so we have exactly one persisted row
        assertEquals(1, messageEventDao.list.size)
    }

    @Test
    fun testDeduplication_duplicateInsertPointsReferenceToExistingMessageId() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val key = "key_dedupe"

        // First message
        val event1 = createDummyEvent(
            key = key,
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Peter",
            text = "On my way"
        )
        coordinator.handleNotificationEvent(event1)

        val firstMessageId = messageEventDao.list.first().id
        assertEquals(firstMessageId, activeNotificationReferenceDao.list.first().latestMessageEventId)

        // Second message (duplicate of the first due to same dedupeHash)
        val event2 = createDummyEvent(
            key = key,
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Peter",
            text = "On my way",
            eventType = NotificationEventType.UPDATED
        )
        coordinator.handleNotificationEvent(event2)

        // List size remains 1
        assertEquals(1, messageEventDao.list.size)
        // Reference latestMessageEventId must point to the existing message ID, NOT a random new one
        assertEquals(firstMessageId, activeNotificationReferenceDao.list.first().latestMessageEventId)
    }

    @Test
    fun testConsentBoundary_ignoresMessagePredatingStorageEnabledAt() = runTest {
        preferencesRepository.setStorageEnabled(true)
        // Set storageEnabledAt to a future time
        val futureTime = System.currentTimeMillis() + 100000L
        preferencesRepository.storageEnabledAtFlow.value = futureTime

        val event = createDummyEvent(
            key = "key_consent_boundary",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Iron Man",
            text = "I am Iron Man"
        )

        coordinator.handleNotificationEvent(event)

        // Verify nothing was persisted because the message timestamp predated storage consent
        assertTrue(messageEventDao.list.isEmpty())
    }

    @Test
    fun testRepository_deleteAllDataPurgesAllTables() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val event = createDummyEvent(
            key = "key_purge",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Aunt May",
            text = "Dinner at 7"
        )

        coordinator.handleNotificationEvent(event)

        assertFalse(conversationDao.list.isEmpty())
        assertFalse(messageEventDao.list.isEmpty())
        assertFalse(activeNotificationReferenceDao.list.isEmpty())

        // Call repository delete-all path
        repository.deleteAllData()

        assertTrue(conversationDao.list.isEmpty())
        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())
    }

    private fun createDummyEvent(
        key: String,
        source: NotificationParseSource,
        title: String,
        text: String,
        eventType: NotificationEventType = NotificationEventType.POSTED
    ): ParsedWhatsAppNotificationEvent {
        val timestamp = 1716900000000L
        val dedupe = WhatsAppNotificationParser.generateDedupeCandidate(
            packageName = "com.whatsapp",
            notificationKey = key,
            timestamp = timestamp,
            conversationTitle = title,
            senderName = title,
            messageText = text
        )

        return ParsedWhatsAppNotificationEvent(
            eventType = eventType,
            notificationKey = key,
            packageName = "com.whatsapp",
            observedAt = System.currentTimeMillis(),
            notificationPostedAt = timestamp,
            conversationTitle = title,
            conversationType = if (source == NotificationParseSource.MESSAGING_STYLE) ConversationType.DIRECT else ConversationType.UNKNOWN,
            messages = listOf(ParsedMessagePreview(senderName = title, messageText = text, timestamp = timestamp)),
            currentMessageCount = 1,
            historicMessageCount = 0,
            hasReplyActionAtCaptureTime = true,
            parseSource = source,
            isContentUnavailable = false,
            dedupeCandidate = dedupe,
            isCurrentlyActive = true
        )
    }
}
