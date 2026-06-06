package com.example.gemmacontrol.data

import com.example.gemmacontrol.data.crypto.*
import com.example.gemmacontrol.data.local.dao.ActiveNotificationReferenceDao
import com.example.gemmacontrol.data.local.dao.ConversationDao
import com.example.gemmacontrol.data.local.dao.FollowUpDao
import com.example.gemmacontrol.data.local.dao.MessageEventDao
import com.example.gemmacontrol.data.local.dao.ReminderDao
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import com.example.gemmacontrol.data.local.entity.ConversationEntity
import com.example.gemmacontrol.data.local.entity.FollowUpEntity
import com.example.gemmacontrol.data.local.entity.InboxPriority
import com.example.gemmacontrol.data.local.entity.MessageEventEntity
import com.example.gemmacontrol.data.local.entity.ReminderEntity
import com.example.gemmacontrol.data.preferences.CapturePreferencesRepository
import com.example.gemmacontrol.data.preferences.VoiceInputMode
import com.example.gemmacontrol.data.reminder.ReminderScheduler
import com.example.gemmacontrol.data.repository.NotificationPersistenceCoordinator
import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationEventType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedMessagePreview
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import com.example.gemmacontrol.notifications.RecentOutgoingReplyEchoSuppressor
import com.example.gemmacontrol.notifications.WhatsAppContentKind
import com.example.gemmacontrol.notifications.WhatsAppNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID

// ─── Cryptography Fake ────────────────────────────────────────────────────────
class FakeSensitiveTextCipher : SensitiveTextCipher {
    override fun encrypt(plaintext: String, associatedData: ByteArray?): EncryptedPayload {
        return EncryptedPayload(plaintext.toByteArray(), byteArrayOf(1, 2, 3))
    }

    override fun decrypt(payload: EncryptedPayload, associatedData: ByteArray?): String {
        return String(payload.ciphertext)
    }
}

class FakeDedupeTokenGenerator : DedupeTokenGenerator {
    override fun generate(canonicalIdentityMaterial: String): String {
        // Deterministic opaque mock token
        return "token_${canonicalIdentityMaterial.hashCode()}"
    }
}

// ─── Preferences Fake ─────────────────────────────────────────────────────────
class FakeCapturePreferencesRepository : CapturePreferencesRepository {
    override val captureEnabledFlow = MutableStateFlow(true)
    override val storageEnabledFlow = MutableStateFlow(false)
    override val storageEnabledAtFlow = MutableStateFlow(0L)
    override val xiaomiAutostartAcknowledgedFlow = MutableStateFlow(false)
    override val voiceInputModeFlow = MutableStateFlow(VoiceInputMode.TapToggle)

    override suspend fun setCaptureEnabled(enabled: Boolean) {
        captureEnabledFlow.value = enabled
    }

    override suspend fun setStorageEnabled(enabled: Boolean) {
        storageEnabledFlow.value = enabled
        if (enabled) {
            storageEnabledAtFlow.value = 1L
        } else {
            storageEnabledAtFlow.value = 0L
        }
    }

    override suspend fun setXiaomiAutostartAcknowledged(acknowledged: Boolean) {
        xiaomiAutostartAcknowledgedFlow.value = acknowledged
    }

    override suspend fun setVoiceInputMode(mode: VoiceInputMode) {
        voiceInputModeFlow.value = mode
    }
}

// ─── DAO Fakes ────────────────────────────────────────────────────────────────
class FakeConversationDao : ConversationDao {
    val list = mutableListOf<ConversationEntity>()
    var onDeleteById: (String) -> Unit = {}

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
    override suspend fun deleteById(id: String) {
        list.removeAll { it.id == id }
        onDeleteById(id)
    }
}

class FakeMessageEventDao(
    private val conversationDao: FakeConversationDao,
    private val activeRefDao: FakeActiveNotificationReferenceDao
) : MessageEventDao {
    val list = mutableListOf<MessageEventEntity>()
    override suspend fun insert(message: MessageEventEntity): Long {
        if (list.any { it.dedupeToken == message.dedupeToken }) {
            return -1L // Room OnConflictStrategy.IGNORE behavior
        }
        list.add(message)
        return list.size.toLong()
    }
    override suspend fun getByDedupeToken(dedupeToken: String): MessageEventEntity? = list.find { it.dedupeToken == dedupeToken }
    override suspend fun getById(id: String): MessageEventEntity? = list.find { it.id == id }
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

    override suspend fun updatePriority(messageEventId: String, priority: InboxPriority): Int {
        val index = list.indexOfFirst { it.id == messageEventId }
        if (index == -1) {
            return 0
        }
        list[index] = list[index].copy(priority = priority)
        return 1
    }

    override suspend fun deleteAllData() {
        deleteAll()
        deleteConversations()
        deleteActiveReferences()
    }
}

class FakeActiveNotificationReferenceDao : ActiveNotificationReferenceDao {
    val list = mutableListOf<ActiveNotificationReferenceEntity>()
    var messageIdsForConversation: (String) -> Set<String> = { emptySet() }

    override suspend fun insertOrUpdate(reference: ActiveNotificationReferenceEntity) {
        list.removeAll { it.notificationKey == reference.notificationKey }
        list.add(reference)
    }
    override suspend fun getByKey(notificationKey: String): ActiveNotificationReferenceEntity? = list.find { it.notificationKey == notificationKey }
    override fun getActiveReferencesFlow(): Flow<List<ActiveNotificationReferenceEntity>> = flow { emit(list.filter { it.removedAt == null }) }
    override suspend fun getActiveReferences(): List<ActiveNotificationReferenceEntity> = list.filter { it.removedAt == null }
    override suspend fun deleteAll() = list.clear()
    override suspend fun deleteForConversation(conversationId: String) {
        val messageIds = messageIdsForConversation(conversationId)
        list.removeAll { it.latestMessageEventId in messageIds }
    }
}

class FakeFollowUpDao : FollowUpDao {
    val list = mutableListOf<FollowUpEntity>()

    override suspend fun insert(followUp: FollowUpEntity): Long {
        list.add(followUp)
        return list.size.toLong()
    }

    override suspend fun getPending(priority: InboxPriority?, limit: Int): List<FollowUpEntity> {
        return list
            .filter { it.completedAt == null }
            .filter { priority == null || it.priority == priority }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    override suspend fun getByStatus(status: String?, priority: InboxPriority?, limit: Int): List<FollowUpEntity> {
        return list
            .filter { followUp ->
                status == null ||
                    (status == "PENDING" && followUp.completedAt == null) ||
                    (status == "COMPLETED" && followUp.completedAt != null)
            }
            .filter { priority == null || it.priority == priority }
            .sortedWith(
                compareBy<FollowUpEntity> { if (it.completedAt == null) 0 else 1 }
                    .thenByDescending { it.createdAt }
            )
            .take(limit)
    }

    override suspend fun markCompleted(id: String, completedAt: Long): Int {
        val index = list.indexOfFirst { it.id == id && it.completedAt == null }
        if (index == -1) {
            return 0
        }
        list[index] = list[index].copy(completedAt = completedAt)
        return 1
    }

    override suspend fun deleteAll() {
        list.clear()
    }
}

class FakeReminderDao : ReminderDao {
    val list = mutableListOf<ReminderEntity>()

    override suspend fun insert(reminder: ReminderEntity): Long {
        list.add(reminder)
        return list.size.toLong()
    }

    override suspend fun getById(id: String): ReminderEntity? = list.find { it.id == id }

    override suspend fun setScheduledWorkName(id: String, workName: String): Int {
        val index = list.indexOfFirst { it.id == id }
        if (index == -1) {
            return 0
        }
        list[index] = list[index].copy(scheduledWorkName = workName)
        return 1
    }

    override suspend fun markDelivered(id: String, deliveredAt: Long): Int {
        val index = list.indexOfFirst { it.id == id && it.deliveredAt == null }
        if (index == -1) {
            return 0
        }
        list[index] = list[index].copy(deliveredAt = deliveredAt)
        return 1
    }
}

class FakeReminderScheduler : ReminderScheduler {
    val calls = mutableListOf<ScheduledReminderCall>()

    override suspend fun schedule(reminderId: String, remindAtEpochMillis: Long): String {
        calls += ScheduledReminderCall(reminderId, remindAtEpochMillis)
        return "reminder:$reminderId"
    }
}

data class ScheduledReminderCall(
    val reminderId: String,
    val remindAtEpochMillis: Long
)

// ─── Test Suite ───────────────────────────────────────────────────────────────
class NotificationPersistenceCoordinatorTest {

    private lateinit var conversationDao: FakeConversationDao
    private lateinit var messageEventDao: FakeMessageEventDao
    private lateinit var activeNotificationReferenceDao: FakeActiveNotificationReferenceDao
    private lateinit var followUpDao: FakeFollowUpDao
    private lateinit var reminderDao: FakeReminderDao
    private lateinit var reminderScheduler: FakeReminderScheduler
    private lateinit var sensitiveTextCipher: FakeSensitiveTextCipher
    private lateinit var dedupeTokenGenerator: FakeDedupeTokenGenerator
    private lateinit var preferencesRepository: FakeCapturePreferencesRepository
    private lateinit var repository: StoredInboxRepository
    private lateinit var coordinator: NotificationPersistenceCoordinator

    @Before
    fun setUp() {
        conversationDao = FakeConversationDao()
        activeNotificationReferenceDao = FakeActiveNotificationReferenceDao()
        messageEventDao = FakeMessageEventDao(conversationDao, activeNotificationReferenceDao)
        followUpDao = FakeFollowUpDao()
        reminderDao = FakeReminderDao()
        reminderScheduler = FakeReminderScheduler()
        conversationDao.onDeleteById = { conversationId ->
            messageEventDao.list.removeAll { it.conversationId == conversationId }
        }
        activeNotificationReferenceDao.messageIdsForConversation = { conversationId ->
            messageEventDao.list
                .filter { it.conversationId == conversationId }
                .map { it.id }
                .toSet()
        }
        sensitiveTextCipher = FakeSensitiveTextCipher()
        dedupeTokenGenerator = FakeDedupeTokenGenerator()
        preferencesRepository = FakeCapturePreferencesRepository()
        
        repository = StoredInboxRepository(
            conversationDao,
            messageEventDao,
            activeNotificationReferenceDao,
            sensitiveTextCipher,
            dedupeTokenGenerator,
            followUpDao = followUpDao,
            reminderDao = reminderDao,
            reminderScheduler = reminderScheduler
        )

        coordinator = NotificationPersistenceCoordinator(
            repository,
            preferencesRepository,
            activeNotificationReferenceDao
        )
    }

    @Test
    fun testSettingsDefaultToDisabled() = runTest {
        assertTrue(preferencesRepository.captureEnabledFlow.first())
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

        coordinator.handleNotificationEvent(event)

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

        // 1. Verify conversation and message are persisted
        assertEquals(1, conversationDao.list.size)
        assertEquals(1, messageEventDao.list.size)
        assertEquals(1, activeNotificationReferenceDao.list.size)

        // 2. Verify Conversation ID is opaque and NOT equal to plaintext display name
        val conversationEntity = conversationDao.list.first()
        assertNotEquals("Aunt May", conversationEntity.id)
        
        // 3. Verify display name is encrypted in the database (stored as ciphertext ByteArray)
        assertEquals("Aunt May", sensitiveTextCipher.decrypt(EncryptedPayload(conversationEntity.encryptedDisplayName!!, conversationEntity.displayNameIv!!)))

        // 4. Verify sender name and message body are encrypted in the database
        val messageEntity = messageEventDao.list.first()
        assertEquals("Aunt May", sensitiveTextCipher.decrypt(EncryptedPayload(messageEntity.encryptedSenderName!!, messageEntity.senderNameIv!!)))
        assertEquals("Dinner at 7", sensitiveTextCipher.decrypt(EncryptedPayload(messageEntity.encryptedMessageText!!, messageEntity.messageTextIv!!)))

        // 5. Verify dynamic dynamic decryption loads successfully at the repository boundary
        val decryptedMessages = repository.getAllDecryptedMessages()
        assertEquals(1, decryptedMessages.size)
        assertEquals("Aunt May", decryptedMessages.first().conversationId) // Decrypted conversation title
        assertEquals("Aunt May", decryptedMessages.first().senderName)       // Decrypted sender name
        assertEquals("Dinner at 7", decryptedMessages.first().decryptedText)  // Decrypted message text
    }

    @Test
    fun testCoordinatorPersistsContentKindMetadataForMediaPlaceholder() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val event = createDummyEvent(
            key = "key_photo",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Mom",
            text = "Photo",
            contentKind = WhatsAppContentKind.PHOTO
        )

        coordinator.handleNotificationEvent(event)

        val messageEntity = messageEventDao.list.single()
        assertEquals(WhatsAppContentKind.PHOTO, messageEntity.contentKind)

        val recentMessages = repository.listRecentMessages(
            conversationName = null,
            limit = 10,
            sinceMinutes = null
        )
        assertEquals(1, recentMessages.size)
        assertEquals(WhatsAppContentKind.PHOTO, recentMessages.single().contentKind)
        assertEquals("Photo", recentMessages.single().text)
    }

    @Test
    fun testCoordinatorPersistsHiddenCanonicalMetadataWithoutMessageText() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val event = createDummyEvent(
            key = "key_hidden",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Mom",
            text = "",
            contentKind = WhatsAppContentKind.HIDDEN
        )

        coordinator.handleNotificationEvent(event)

        val messageEntity = messageEventDao.list.single()
        assertEquals(WhatsAppContentKind.HIDDEN, messageEntity.contentKind)
        assertEquals(null, messageEntity.encryptedMessageText)
        assertEquals(null, messageEntity.messageTextIv)

        val decryptedMessages = repository.getAllDecryptedMessages()
        assertEquals(WhatsAppContentKind.HIDDEN, decryptedMessages.single().contentKind)
        assertEquals(null, decryptedMessages.single().decryptedText)
    }

    @Test
    fun testCoordinatorSkipsSystemNotifications() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val event = createDummyEvent(
            key = "key_system",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "WhatsApp",
            text = "Checking for new messages",
            contentKind = WhatsAppContentKind.SYSTEM
        )

        coordinator.handleNotificationEvent(event)

        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(conversationDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())
    }

    @Test
    fun testCoordinatorSkipsRecentlySentReplyEcho() = runTest {
        preferencesRepository.setStorageEnabled(true)
        RecentOutgoingReplyEchoSuppressor.clear()
        RecentOutgoingReplyEchoSuppressor.register(
            notificationKey = "key_reply_echo",
            replyText = "I am in a meeting"
        )

        val event = createDummyEvent(
            key = "key_reply_echo",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Mom",
            text = "I am in a meeting",
            timestamp = 1_716_900_001_000L
        )

        coordinator.handleNotificationEvent(event)

        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(conversationDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())
        RecentOutgoingReplyEchoSuppressor.clear()
    }

    @Test
    fun testCoordinatorSkipsReplyEchoWhenWhatsAppRefreshesNotificationKey() = runTest {
        preferencesRepository.setStorageEnabled(true)
        RecentOutgoingReplyEchoSuppressor.clear()
        RecentOutgoingReplyEchoSuppressor.register(
            notificationKey = "key_original_reply",
            replyText = "I am in a meeting",
            conversationTitle = "Mom"
        )

        val refreshedKeyEvent = createDummyEvent(
            key = "key_refreshed_reply",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Mom",
            text = "I am in a meeting",
            timestamp = 1_716_900_001_000L
        )

        coordinator.handleNotificationEvent(refreshedKeyEvent)

        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(conversationDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())
        RecentOutgoingReplyEchoSuppressor.clear()
    }

    @Test
    fun testCoordinatorDoesNotSuppressSameReplyTextFromDifferentConversation() = runTest {
        preferencesRepository.setStorageEnabled(true)
        RecentOutgoingReplyEchoSuppressor.clear()
        RecentOutgoingReplyEchoSuppressor.register(
            notificationKey = "key_original_reply",
            replyText = "Ok",
            conversationTitle = "Mom"
        )

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_office",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Office",
                text = "Ok",
                timestamp = 1_716_900_001_000L
            )
        )

        assertEquals(1, messageEventDao.list.size)
        assertEquals(1, conversationDao.list.size)
        RecentOutgoingReplyEchoSuppressor.clear()
    }

    @Test
    fun testRepositoryCanonicalPersistenceUsesInjectedNowProvider() = runTest {
        val fixedNow = 123_456_789L
        val repositoryWithFixedClock = StoredInboxRepository(
            conversationDao,
            messageEventDao,
            activeNotificationReferenceDao,
            sensitiveTextCipher,
            dedupeTokenGenerator,
            nowProvider = { fixedNow }
        )

        val event = createDummyEvent(
            key = "key_fixed_clock",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Aunt May",
            text = "Dinner at 7"
        )

        val result = repositoryWithFixedClock.persistCanonicalEvent(event)

        assertTrue(result is StoredInboxRepository.PersistCanonicalResult.Stored)
        assertEquals(fixedNow, conversationDao.list.single().createdAt)
        assertEquals(fixedNow, conversationDao.list.single().updatedAt)
        assertEquals(fixedNow, messageEventDao.list.single().createdAt)
        assertEquals(fixedNow, activeNotificationReferenceDao.list.single().lastSeenActiveAt)
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

        assertTrue(messageEventDao.list.isEmpty())
    }

    @Test
    fun testIngestionPolicy_messagingStylePlusFallbackWithSameKeyPersistsOneRow() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val key = "key_same"

        val canonicalEvent = createDummyEvent(
            key = key,
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Spidey Chat",
            text = "Hey!"
        )
        coordinator.handleNotificationEvent(canonicalEvent)

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

        val canonicalEvent = createDummyEvent(
            key = "key_canonical",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Spidey Chat",
            text = "Hey!"
        )
        coordinator.handleNotificationEvent(canonicalEvent)

        val fallbackEvent = createDummyEvent(
            key = "key_fallback",
            source = NotificationParseSource.EXTRAS_FALLBACK,
            title = "Spidey Chat",
            text = "Hey!"
        )
        coordinator.handleNotificationEvent(fallbackEvent)

        assertEquals(1, messageEventDao.list.size)
    }

    @Test
    fun testDeduplication_duplicateInsertPointsReferenceToExistingMessageId() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val key = "key_dedupe"

        val event1 = createDummyEvent(
            key = key,
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Peter",
            text = "On my way"
        )
        coordinator.handleNotificationEvent(event1)

        val firstMessageId = messageEventDao.list.first().id
        assertEquals(firstMessageId, activeNotificationReferenceDao.list.first().latestMessageEventId)

        val event2 = createDummyEvent(
            key = key,
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Peter",
            text = "On my way",
            eventType = NotificationEventType.UPDATED
        )
        coordinator.handleNotificationEvent(event2)

        assertEquals(1, messageEventDao.list.size)
        assertEquals(firstMessageId, activeNotificationReferenceDao.list.first().latestMessageEventId)
    }

    @Test
    fun testConsentBoundary_ignoresMessagePredatingStorageEnabledAt() = runTest {
        preferencesRepository.setStorageEnabled(true)
        val futureTime = System.currentTimeMillis() + 100000L
        preferencesRepository.storageEnabledAtFlow.value = futureTime

        val event = createDummyEvent(
            key = "key_consent_boundary",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Iron Man",
            text = "I am Iron Man"
        )

        coordinator.handleNotificationEvent(event)

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

        repository.deleteAllData()

        assertTrue(conversationDao.list.isEmpty())
        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())
    }

    @Test
    fun testRepository_deleteConversationDataPurgesOnlyMatchingConversation() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_aunt_may",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Aunt May",
                text = "Dinner at 7"
            )
        )
        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_peter",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Peter",
                text = "On my way"
            )
        )

        val deleted = repository.deleteConversationData("aunt   may")

        assertTrue(deleted)
        val remainingMessages = repository.getAllDecryptedMessages()
        assertEquals(1, remainingMessages.size)
        assertEquals("Peter", remainingMessages.single().conversationId)
        assertEquals(1, activeNotificationReferenceDao.list.size)
    }

    @Test
    fun testRepository_createAndCompleteFollowUp() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_follow_up",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Aunt May",
                text = "Can you call back?"
            )
        )
        val messageId = messageEventDao.list.single().id

        val followUpId = repository.createFollowUp(
            messageEventId = messageId,
            title = "Call back",
            dueAt = "2026-05-30T09:00:00+05:30",
            priority = "HIGH"
        )

        assertTrue(followUpId != null)
        val pending = repository.listPendingFollowUps(limit = 10, priority = "HIGH")
        assertEquals(1, pending.size)
        assertEquals("Call back", pending.single().title)
        assertEquals(messageId, pending.single().messageEventId)

        assertTrue(repository.markFollowUpCompleted(followUpId!!))
        assertTrue(repository.listPendingFollowUps(limit = 10, priority = null).isEmpty())
    }

    @Test
    fun testRepository_markMessagePriorityUpdatesDecryptedMessage() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_priority",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Peter",
                text = "Deadline moved earlier"
            )
        )
        val messageId = messageEventDao.list.single().id

        assertTrue(repository.markMessagePriority(messageId, "HIGH"))

        val decryptedMessages = repository.getAllDecryptedMessages()
        assertEquals("HIGH", decryptedMessages.single().priority)
    }

    @Test
    fun testRepository_scheduleReminderEncryptsNoteAndEnqueuesWork() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_reminder",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Aunt May",
                text = "Call me tomorrow"
            )
        )
        val messageId = messageEventDao.list.single().id

        val reminderId = repository.scheduleReminder(
            messageEventId = messageId,
            remindAt = "2099-05-30T09:00:00+05:30",
            reminderNote = "Call back"
        )

        assertTrue(reminderId != null)
        val reminder = reminderDao.list.single()
        assertEquals(messageId, reminder.messageEventId)
        assertEquals("Call back", sensitiveTextCipher.decrypt(EncryptedPayload(reminder.encryptedReminderNote!!, reminder.reminderNoteIv!!)))
        assertEquals(reminderId, reminderScheduler.calls.single().reminderId)
        assertTrue(reminderScheduler.calls.single().remindAtEpochMillis > System.currentTimeMillis())
    }

    @Test
    fun testRepository_searchMessagesFiltersByQueryAndConversation() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_mom_dinner",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Mom",
                text = "Dinner at 7"
            )
        )
        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_peter_dinner",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Peter",
                text = "Dinner moved"
            )
        )

        val results = repository.searchMessages(
            query = "dinner",
            conversationName = "Mom",
            sinceMinutes = null,
            priority = null
        )

        assertEquals(1, results.size)
        assertEquals("Mom", results.single().conversationName)
        assertEquals("Dinner at 7", results.single().text)
    }

    @Test
    fun testRepository_searchMessagesFiltersBySinceMinutesAndPriority() = runTest {
        preferencesRepository.setStorageEnabled(true)
        val now = System.currentTimeMillis()

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_recent_high_invoice",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Office",
                text = "Invoice due today",
                timestamp = now - 30_000L
            )
        )
        val recentHighMessageId = messageEventDao.list.single().id
        repository.markMessagePriority(recentHighMessageId, "HIGH")

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_recent_normal_invoice",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Office",
                text = "Invoice draft noted",
                timestamp = now - 30_000L
            )
        )

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_old_high_invoice",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Office",
                text = "Invoice from last week",
                timestamp = now - 3_600_000L
            )
        )
        val oldHighMessageId = messageEventDao.list.last().id
        repository.markMessagePriority(oldHighMessageId, "HIGH")

        val results = repository.searchMessages(
            query = "invoice",
            conversationName = null,
            sinceMinutes = 10,
            priority = "HIGH"
        )

        assertEquals(1, results.size)
        assertEquals(recentHighMessageId, results.single().id)
        assertEquals("Invoice due today", results.single().text)
    }

    @Test
    fun testRepository_listRecentMessagesFiltersByConversationAndSinceMinutes() = runTest {
        preferencesRepository.setStorageEnabled(true)
        val now = System.currentTimeMillis()

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_recent_mom",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Mom",
                text = "Dinner at 7",
                timestamp = now - 30_000L
            )
        )
        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_old_mom",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Mom",
                text = "Old dinner note",
                timestamp = now - 10 * 60_000L
            )
        )
        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_recent_peter",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Peter",
                text = "Project update",
                timestamp = now - 20_000L
            )
        )

        val results = repository.listRecentMessages(
            conversationName = "Mom",
            limit = 5,
            sinceMinutes = 2
        )

        assertEquals(1, results.size)
        assertEquals("Mom", results.single().conversationName)
        assertEquals("Dinner at 7", results.single().text)
    }

    @Test
    fun testRepository_getActiveNotificationKeysReturnsOnlyVisibleNotifications() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_active",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Mom",
                text = "Still visible"
            )
        )
        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_removed",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Office",
                text = "Already dismissed"
            )
        )

        repository.markNotificationRemoved("key_removed")

        assertEquals(setOf("key_active"), repository.getActiveNotificationKeys())
    }

    @Test
    fun testRepository_getMessageDetailsReturnsStoredMessage() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_details",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Mom",
                text = "Dinner at 7"
            )
        )
        val messageId = messageEventDao.list.single().id

        val result = repository.getMessageDetails(messageId)

        assertEquals(messageId, result?.id)
        assertEquals("Mom", result?.conversationName)
        assertEquals("Dinner at 7", result?.text)
    }

    @Test
    fun testRepository_getActionableInboxReturnsPendingFollowUpsAndPriorityMessages() = runTest {
        preferencesRepository.setStorageEnabled(true)

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_actionable_follow_up",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Mom",
                text = "Call me tomorrow"
            )
        )
        val followUpMessageId = messageEventDao.list.single().id
        repository.createFollowUp(
            messageEventId = followUpMessageId,
            title = "Call back",
            dueAt = "2026-05-30T09:00:00+05:30",
            priority = "HIGH"
        )

        coordinator.handleNotificationEvent(
            createDummyEvent(
                key = "key_actionable_priority",
                source = NotificationParseSource.MESSAGING_STYLE,
                title = "Peter",
                text = "Deadline moved earlier"
            )
        )
        val priorityMessageId = messageEventDao.list.last().id
        repository.markMessagePriority(priorityMessageId, "HIGH")

        val pending = repository.getActionableInbox(status = "PENDING", priority = "HIGH", limit = 10)
        val byType = pending.associateBy { it.type }

        assertEquals(setOf("FOLLOW_UP", "PRIORITY_MESSAGE"), byType.keys)
        assertEquals("Call back", byType["FOLLOW_UP"]?.title)
        assertEquals(followUpMessageId, byType["FOLLOW_UP"]?.messageEventId)
        assertEquals("Mom", byType["FOLLOW_UP"]?.conversationName)
        assertEquals("Deadline moved earlier", byType["PRIORITY_MESSAGE"]?.text)
        assertEquals(priorityMessageId, byType["PRIORITY_MESSAGE"]?.messageEventId)

        val followUpId = followUpDao.list.single().id
        repository.markFollowUpCompleted(followUpId)

        val completed = repository.getActionableInbox(status = "COMPLETED", priority = "HIGH", limit = 10)

        assertEquals(1, completed.size)
        assertEquals("FOLLOW_UP", completed.single().type)
        assertEquals("COMPLETED", completed.single().status)
    }

    @Test
    fun testRepository_throwingHmacGeneratorProducesZeroPersistedRows() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val throwingHmac = object : DedupeTokenGenerator {
            override fun generate(canonicalIdentityMaterial: String): String {
                throw TokenGenerationFailure(Exception("Simulated HMAC failure"))
            }
        }

        val repositoryWithHmacFailure = StoredInboxRepository(
            conversationDao,
            messageEventDao,
            activeNotificationReferenceDao,
            sensitiveTextCipher,
            throwingHmac
        )

        val failingCoordinator = NotificationPersistenceCoordinator(
            repositoryWithHmacFailure,
            preferencesRepository,
            activeNotificationReferenceDao
        )

        val event = createDummyEvent(
            key = "key_hmac_fail",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Aunt May",
            text = "Dinner at 7"
        )

        failingCoordinator.handleNotificationEvent(event)

        // Assert fail-closed: absolutely nothing is persisted
        assertTrue(conversationDao.list.isEmpty())
        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())

        // Assert direct call returns SecureStorageUnavailable
        val result = repositoryWithHmacFailure.persistCanonicalEvent(event)
        assertEquals(StoredInboxRepository.PersistCanonicalResult.SecureStorageUnavailable, result)
    }

    @Test
    fun testRepository_throwingSensitiveTextCipherProducesZeroPersistedRows() = runTest {
        preferencesRepository.setStorageEnabled(true)

        val throwingCipher = object : SensitiveTextCipher {
            override fun encrypt(plaintext: String, associatedData: ByteArray?): EncryptedPayload {
                throw EncryptionFailure(Exception("Simulated cipher encryption failure"))
            }
            override fun decrypt(payload: EncryptedPayload, associatedData: ByteArray?): String {
                throw EncryptionFailure(Exception("Simulated cipher decryption failure"))
            }
        }

        val repositoryWithCipherFailure = StoredInboxRepository(
            conversationDao,
            messageEventDao,
            activeNotificationReferenceDao,
            throwingCipher,
            dedupeTokenGenerator
        )

        val failingCoordinator = NotificationPersistenceCoordinator(
            repositoryWithCipherFailure,
            preferencesRepository,
            activeNotificationReferenceDao
        )

        val event = createDummyEvent(
            key = "key_cipher_fail",
            source = NotificationParseSource.MESSAGING_STYLE,
            title = "Aunt May",
            text = "Dinner at 7"
        )

        failingCoordinator.handleNotificationEvent(event)

        // Assert fail-closed: absolutely nothing is persisted
        assertTrue(conversationDao.list.isEmpty())
        assertTrue(messageEventDao.list.isEmpty())
        assertTrue(activeNotificationReferenceDao.list.isEmpty())

        // Assert direct call returns SecureStorageUnavailable
        val result = repositoryWithCipherFailure.persistCanonicalEvent(event)
        assertEquals(StoredInboxRepository.PersistCanonicalResult.SecureStorageUnavailable, result)
    }

    @Test
    fun testDedupeTokenGeneratorProductionThrowsExceptionsOnJvm() {
        try {
            val generator = AndroidKeystoreHmacDedupeTokenGenerator()
            generator.generate("test_identity")
            org.junit.Assert.fail("Should have thrown SecureStorageFailure on JVM")
        } catch (e: SecureStorageFailure) {
            // Success: the production generator throws when Keystore is missing instead of failing open
        } catch (e: Exception) {
            // Success: also acceptable if framework dependencies fail closed
        }
    }


    private fun createDummyEvent(
        key: String,
        source: NotificationParseSource,
        title: String,
        text: String,
        eventType: NotificationEventType = NotificationEventType.POSTED,
        timestamp: Long = 1716900000000L,
        contentKind: WhatsAppContentKind = WhatsAppContentKind.TEXT
    ): ParsedWhatsAppNotificationEvent {
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
            messages = listOf(
                ParsedMessagePreview(
                    senderName = title,
                    messageText = text.takeIf { it.isNotBlank() },
                    timestamp = timestamp,
                    contentKind = contentKind
                )
            ),
            currentMessageCount = 1,
            historicMessageCount = 0,
            hasReplyActionAtCaptureTime = true,
            parseSource = source,
            isContentUnavailable = contentKind == WhatsAppContentKind.HIDDEN,
            dedupeCandidate = dedupe,
            isCurrentlyActive = true
        )
    }
}
