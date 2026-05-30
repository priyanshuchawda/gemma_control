package com.example.gemmacontrol.data.repository

import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.crypto.SensitiveTextCipher
import com.example.gemmacontrol.data.crypto.DedupeTokenGenerator
import androidx.room.withTransaction
import com.example.gemmacontrol.ai.tools.LocalActionableInboxItem
import com.example.gemmacontrol.ai.tools.LocalFollowUp
import com.example.gemmacontrol.ai.tools.LocalWhatsAppDataRepository
import com.example.gemmacontrol.ai.tools.LocalWhatsAppMessage
import com.example.gemmacontrol.data.local.GemmaControlDatabase
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
import com.example.gemmacontrol.data.reminder.ReminderScheduler
import com.example.gemmacontrol.data.reminder.ReminderTimeParser
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StoredInboxRepository(
    private val conversationDao: ConversationDao,
    private val messageEventDao: MessageEventDao,
    private val activeNotificationReferenceDao: ActiveNotificationReferenceDao,
    private val sensitiveTextCipher: SensitiveTextCipher,
    private val dedupeTokenGenerator: DedupeTokenGenerator,
    private val db: GemmaControlDatabase? = null,
    private val followUpDao: FollowUpDao? = null,
    private val reminderDao: ReminderDao? = null,
    private val reminderScheduler: ReminderScheduler? = null,
    private val nowProvider: () -> Long = System::currentTimeMillis
) : LocalWhatsAppDataRepository {
    sealed interface PersistCanonicalResult {
        data class Stored(val messageId: String) : PersistCanonicalResult
        data object DuplicateReferenced : PersistCanonicalResult
        data object SkippedByPolicy : PersistCanonicalResult
        data object SecureStorageUnavailable : PersistCanonicalResult
    }

    private data class CryptoOutputs(
        val conversationOpaqueId: String,
        val encryptedDisplayName: ByteArray,
        val displayNameIv: ByteArray,
        val encryptedSenderName: ByteArray?,
        val senderNameIv: ByteArray?,
        val encryptedMessageText: ByteArray?,
        val messageTextIv: ByteArray?,
        val dedupeToken: String
    )

    private data class ActionableInboxFilters(
        val status: String?,
        val priority: InboxPriority?,
        val limit: Int
    )

    data class DecryptedMessage(
        val id: String,
        val conversationId: String, // Decrypted conversation display name
        val senderName: String?,    // Decrypted sender name
        val decryptedText: String?,
        val postedAt: Long,
        val notificationKey: String,
        val sourcePackage: String,
        val parseSource: NotificationParseSource,
        val isContentUnavailable: Boolean,
        val createdAt: Long,
        val priority: String
    )

    data class StoredConversation(
        val entity: ConversationEntity,
        val decryptedDisplayName: String,
        val latestMessage: DecryptedMessage?
    )

    suspend fun persistCanonicalEvent(event: ParsedWhatsAppNotificationEvent): PersistCanonicalResult {
        val now = System.currentTimeMillis()
        val latestMessage = event.messages.lastOrNull()

        // 1. Wrap all cryptographic computations in a fail-closed try-catch block.
        // If Keystore encryption or HMAC token generation throws an exception, no DB write occurs.
        val cryptoOutputs = try {
            val identityMaterial = "${event.conversationTitle ?: "WhatsApp Chat"}-${event.conversationType.name}"
            val conversationOpaqueId = dedupeTokenGenerator.generate(identityMaterial)

            val conversationTitleToStore = event.conversationTitle ?: "WhatsApp Chat"
            val encryptedTitlePayload = sensitiveTextCipher.encrypt(conversationTitleToStore)

            val senderNameToStore = latestMessage?.senderName ?: ""
            val encryptedSenderPayload = if (senderNameToStore.isNotEmpty()) {
                sensitiveTextCipher.encrypt(senderNameToStore)
            } else {
                null
            }

            val textToEncrypt = latestMessage?.messageText ?: ""
            val encryptedMessagePayload = if (textToEncrypt.isNotEmpty()) {
                sensitiveTextCipher.encrypt(textToEncrypt)
            } else {
                null
            }

            val dedupeTokenToStore = dedupeTokenGenerator.generate(event.dedupeCandidate ?: UUID.randomUUID().toString())

            CryptoOutputs(
                conversationOpaqueId = conversationOpaqueId,
                encryptedDisplayName = encryptedTitlePayload.ciphertext,
                displayNameIv = encryptedTitlePayload.iv,
                encryptedSenderName = encryptedSenderPayload?.ciphertext,
                senderNameIv = encryptedSenderPayload?.iv,
                encryptedMessageText = encryptedMessagePayload?.ciphertext,
                messageTextIv = encryptedMessagePayload?.iv,
                dedupeToken = dedupeTokenToStore
            )
        } catch (e: Exception) {
            // Fail closed: return SecureStorageUnavailable, leaving the database untouched.
            return PersistCanonicalResult.SecureStorageUnavailable
        }

        // 2. Perform DB operations inside a Room transaction block (if db is provided) to guarantee all-or-nothing atomicity.
        val executeDbOps = suspend {
            val existingConversation = conversationDao.getById(cryptoOutputs.conversationOpaqueId)
            if (existingConversation == null) {
                val conversation = ConversationEntity(
                    id = cryptoOutputs.conversationOpaqueId,
                    encryptedDisplayName = cryptoOutputs.encryptedDisplayName,
                    displayNameIv = cryptoOutputs.displayNameIv,
                    conversationType = event.conversationType,
                    encryptedVerifiedPhoneNumberE164 = null,
                    verifiedPhoneNumberIv = null,
                    createdAt = now,
                    updatedAt = now
                )
                conversationDao.insert(conversation)
            } else {
                val updated = existingConversation.copy(
                    encryptedDisplayName = cryptoOutputs.encryptedDisplayName,
                    displayNameIv = cryptoOutputs.displayNameIv,
                    conversationType = if (event.conversationType != ConversationType.UNKNOWN) event.conversationType else existingConversation.conversationType,
                    updatedAt = now
                )
                conversationDao.update(updated)
            }

            val messageId = UUID.randomUUID().toString()
            val messageEntity = MessageEventEntity(
                id = messageId,
                conversationId = cryptoOutputs.conversationOpaqueId,
                encryptedSenderName = cryptoOutputs.encryptedSenderName,
                senderNameIv = cryptoOutputs.senderNameIv,
                encryptedMessageText = cryptoOutputs.encryptedMessageText,
                messageTextIv = cryptoOutputs.messageTextIv,
                postedAt = latestMessage?.timestamp ?: event.notificationPostedAt ?: now,
                notificationKey = event.notificationKey,
                sourcePackage = event.packageName,
                parseSource = event.parseSource,
                dedupeToken = cryptoOutputs.dedupeToken,
                isContentUnavailable = event.isContentUnavailable,
                createdAt = now
            )

            val insertResult = messageEventDao.insert(messageEntity)
            val (actualMessageId, isDuplicate) = if (insertResult == -1L) {
                val existing = messageEventDao.getByDedupeToken(cryptoOutputs.dedupeToken)
                (existing?.id ?: messageId) to true
            } else {
                messageId to false
            }

            val reference = ActiveNotificationReferenceEntity(
                notificationKey = event.notificationKey,
                latestMessageEventId = actualMessageId,
                hadReplyActionWhenSeen = event.hasReplyActionAtCaptureTime,
                lastSeenActiveAt = now,
                removedAt = null
            )
            activeNotificationReferenceDao.insertOrUpdate(reference)

            if (isDuplicate) {
                PersistCanonicalResult.DuplicateReferenced
            } else {
                PersistCanonicalResult.Stored(actualMessageId)
            }
        }

        return if (db != null) {
            db.withTransaction { executeDbOps() }
        } else {
            executeDbOps()
        }
    }

    suspend fun markNotificationRemoved(notificationKey: String) {
        val reference = activeNotificationReferenceDao.getByKey(notificationKey)
        if (reference != null) {
            val updated = reference.copy(removedAt = System.currentTimeMillis())
            activeNotificationReferenceDao.insertOrUpdate(updated)
        }
    }

    fun getAllDecryptedMessagesFlow(): Flow<List<DecryptedMessage>> {
        return messageEventDao.getAllMessagesFlow().map { entities ->
            val conversations = conversationDao.getAllConversations()
            val titleMap = conversations.associate { conv ->
                conv.id to decryptConversationTitle(conv)
            }
            entities.map { decryptMessageEntity(it, titleMap) }
        }
    }

    suspend fun getAllDecryptedMessages(): List<DecryptedMessage> {
        val conversations = conversationDao.getAllConversations()
        val titleMap = conversations.associate { conv ->
            conv.id to decryptConversationTitle(conv)
        }
        return messageEventDao.getAllMessages().map { decryptMessageEntity(it, titleMap) }
    }

    fun getStoredConversationsFlow(): Flow<List<StoredConversation>> {
        return conversationDao.getAllConversationsFlow().map { conversations ->
            conversations.map { conversation ->
                val latestMessageEntity = messageEventDao.getMessagesForConversation(conversation.id).firstOrNull()
                val latestMessage = latestMessageEntity?.let {
                    val title = decryptConversationTitle(conversation)
                    decryptMessageEntity(it, mapOf(conversation.id to title))
                }
                val decryptedDisplayName = decryptConversationTitle(conversation)
                StoredConversation(conversation, decryptedDisplayName, latestMessage)
            }
        }
    }

    override suspend fun deleteAllData() {
        messageEventDao.deleteAllData()
    }

    override suspend fun deleteConversationData(conversationName: String): Boolean {
        val normalizedName = normalizeConversationName(conversationName)
        if (normalizedName.isBlank()) {
            return false
        }

        val deleteMatchingConversations = suspend {
            val matchingConversations = conversationDao.getAllConversations()
                .filter { conversation ->
                    normalizeConversationName(decryptConversationTitle(conversation)) == normalizedName
                }
            if (matchingConversations.isEmpty()) {
                false
            } else {
                matchingConversations.forEach { conversation ->
                    activeNotificationReferenceDao.deleteForConversation(conversation.id)
                    conversationDao.deleteById(conversation.id)
                }
                true
            }
        }

        return if (db != null) {
            db.withTransaction { deleteMatchingConversations() }
        } else {
            deleteMatchingConversations()
        }
    }

    override suspend fun createFollowUp(
        messageEventId: String,
        title: String,
        dueAt: String?,
        priority: String?
    ): String? {
        val dao = followUpDao ?: return null
        val cleanMessageEventId = messageEventId.trim()
        val cleanTitle = title.trim()
        if (cleanMessageEventId.isBlank() || cleanTitle.isBlank()) {
            return null
        }
        if (messageEventDao.getById(cleanMessageEventId) == null) {
            return null
        }

        val followUpId = UUID.randomUUID().toString()
        val entity = FollowUpEntity(
            id = followUpId,
            messageEventId = cleanMessageEventId,
            title = cleanTitle,
            dueAt = dueAt?.trim()?.takeIf { it.isNotBlank() },
            priority = parsePriority(priority, default = InboxPriority.NORMAL),
            createdAt = System.currentTimeMillis(),
            completedAt = null
        )
        dao.insert(entity)
        return followUpId
    }

    override suspend fun listPendingFollowUps(limit: Int, priority: String?): List<LocalFollowUp> {
        val dao = followUpDao ?: return emptyList()
        val safeLimit = localToolLimit(limit)
        return dao.getPending(parsePriorityOrNull(priority), safeLimit)
            .map { it.toLocalFollowUp() }
    }

    override suspend fun markFollowUpCompleted(followUpId: String): Boolean {
        val dao = followUpDao ?: return false
        val cleanFollowUpId = followUpId.trim()
        if (cleanFollowUpId.isBlank()) {
            return false
        }
        return dao.markCompleted(cleanFollowUpId, System.currentTimeMillis()) > 0
    }

    override suspend fun markMessagePriority(messageEventId: String, priority: String): Boolean {
        val cleanMessageEventId = messageEventId.trim()
        if (cleanMessageEventId.isBlank()) {
            return false
        }
        val parsedPriority = parseMessagePriority(priority) ?: return false
        return messageEventDao.updatePriority(cleanMessageEventId, parsedPriority) > 0
    }

    override suspend fun scheduleReminder(
        messageEventId: String,
        remindAt: String,
        reminderNote: String?
    ): String? {
        val dao = reminderDao ?: return null
        val scheduler = reminderScheduler ?: return null
        val cleanMessageEventId = messageEventId.trim()
        if (cleanMessageEventId.isBlank() || messageEventDao.getById(cleanMessageEventId) == null) {
            return null
        }
        val remindAtEpochMillis = ReminderTimeParser.parseEpochMillis(remindAt) ?: return null
        if (remindAtEpochMillis <= nowProvider()) {
            return null
        }

        val encryptedNote = try {
            reminderNote?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { sensitiveTextCipher.encrypt(it) }
        } catch (e: Exception) {
            return null
        }
        val reminderId = UUID.randomUUID().toString()
        dao.insert(
            ReminderEntity(
                id = reminderId,
                messageEventId = cleanMessageEventId,
                encryptedReminderNote = encryptedNote?.ciphertext,
                reminderNoteIv = encryptedNote?.iv,
                remindAtEpochMillis = remindAtEpochMillis,
                createdAt = nowProvider(),
                scheduledWorkName = null,
                deliveredAt = null
            )
        )
        val workName = scheduler.schedule(reminderId, remindAtEpochMillis)
        dao.setScheduledWorkName(reminderId, workName)
        return reminderId
    }

    override suspend fun listRecentMessages(
        conversationName: String?,
        limit: Int,
        sinceMinutes: Int?
    ): List<LocalWhatsAppMessage> {
        val safeLimit = localToolLimit(limit)
        val normalizedConversation = conversationName
            ?.let { normalizeConversationName(it) }
            ?.takeIf { it.isNotBlank() }
        val postedAfter = sinceMinutes
            ?.takeIf { it > 0 }
            ?.let { nowProvider() - it * MILLIS_PER_MINUTE }

        return getAllDecryptedMessages()
            .asSequence()
            .filter { message ->
                normalizedConversation == null ||
                    normalizeConversationName(message.conversationId) == normalizedConversation
            }
            .filter { message ->
                postedAfter == null || message.postedAt >= postedAfter
            }
            .sortedByDescending { it.postedAt }
            .take(safeLimit)
            .map { it.toLocalWhatsAppMessage() }
            .toList()
    }

    override suspend fun searchMessages(query: String, conversationName: String?): List<LocalWhatsAppMessage> {
        val normalizedQuery = normalizeSearchText(query)
        if (normalizedQuery.isBlank()) {
            return emptyList()
        }
        val normalizedConversation = conversationName
            ?.let { normalizeConversationName(it) }
            ?.takeIf { it.isNotBlank() }

        return getAllDecryptedMessages()
            .asSequence()
            .filter { message ->
                normalizedConversation == null ||
                    normalizeConversationName(message.conversationId) == normalizedConversation
            }
            .filter { message ->
                listOfNotNull(
                    message.conversationId,
                    message.senderName,
                    message.decryptedText
                ).any { normalizeSearchText(it).contains(normalizedQuery) }
            }
            .sortedByDescending { it.postedAt }
            .take(DEFAULT_SEARCH_LIMIT)
            .map { it.toLocalWhatsAppMessage() }
            .toList()
    }

    override suspend fun getMessageDetails(messageEventId: String): LocalWhatsAppMessage? {
        val cleanMessageEventId = messageEventId.trim()
        if (cleanMessageEventId.isBlank()) {
            return null
        }
        return getAllDecryptedMessages()
            .firstOrNull { it.id == cleanMessageEventId }
            ?.toLocalWhatsAppMessage()
    }

    override suspend fun getActionableInbox(
        status: String?,
        priority: String?,
        limit: Int
    ): List<LocalActionableInboxItem> {
        val filters = parseActionableInboxFilters(status, priority, limit) ?: return emptyList()
        val messages = getAllDecryptedMessages()
        val messagesById = messages.associateBy { it.id }
        val followUps = followUpDao
            ?.getByStatus(filters.status, filters.priority, filters.limit)
            .orEmpty()
        val followUpItems = followUps.mapNotNull { followUp ->
            val message = messagesById[followUp.messageEventId] ?: return@mapNotNull null
            followUp.toLocalActionableInboxItem(message)
        }

        val followUpMessageIds = followUps.map { it.messageEventId }.toSet()
        val priorityMessageItems = priorityActionableItems(messages, filters, followUpMessageIds)

        return (followUpItems + priorityMessageItems)
            .sortedWith(
                compareBy<LocalActionableInboxItem> { if (it.status == ACTIONABLE_STATUS_PENDING) 0 else 1 }
                    .thenByDescending { it.updatedAt }
            )
            .take(filters.limit)
    }

    private fun decryptConversationTitle(entity: ConversationEntity): String {
        return decryptPayloadOrFallback(
            ciphertext = entity.encryptedDisplayName,
            iv = entity.displayNameIv,
            fallback = entity.id
        )
    }

    private fun normalizeConversationName(value: String): String {
        return value.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    private fun FollowUpEntity.toLocalFollowUp(): LocalFollowUp {
        return LocalFollowUp(
            id = id,
            messageEventId = messageEventId,
            title = title,
            dueAt = dueAt,
            priority = priority.name,
            createdAt = createdAt,
            completedAt = completedAt
        )
    }

    private fun DecryptedMessage.toLocalWhatsAppMessage(): LocalWhatsAppMessage {
        return LocalWhatsAppMessage(
            id = id,
            conversationName = conversationId,
            senderName = senderName,
            text = decryptedText,
            postedAt = postedAt,
            priority = priority
        )
    }

    private fun FollowUpEntity.toLocalActionableInboxItem(message: DecryptedMessage): LocalActionableInboxItem {
        return LocalActionableInboxItem(
            id = id,
            messageEventId = messageEventId,
            type = ACTIONABLE_TYPE_FOLLOW_UP,
            title = title,
            conversationName = message.conversationId,
            text = message.decryptedText,
            priority = priority.name,
            status = if (completedAt == null) ACTIONABLE_STATUS_PENDING else ACTIONABLE_STATUS_COMPLETED,
            dueAt = dueAt,
            updatedAt = completedAt ?: createdAt
        )
    }

    private fun DecryptedMessage.toPriorityActionableInboxItem(): LocalActionableInboxItem {
        return LocalActionableInboxItem(
            id = id,
            messageEventId = id,
            type = ACTIONABLE_TYPE_PRIORITY_MESSAGE,
            title = "High priority message",
            conversationName = conversationId,
            text = decryptedText,
            priority = priority,
            status = ACTIONABLE_STATUS_PENDING,
            dueAt = null,
            updatedAt = postedAt
        )
    }

    private fun decryptMessageEntity(entity: MessageEventEntity, conversationMap: Map<String, String>): DecryptedMessage {
        val conversationTitle = conversationMap[entity.conversationId] ?: entity.conversationId

        return DecryptedMessage(
            id = entity.id,
            conversationId = conversationTitle,
            senderName = decryptOptionalPayload(entity.encryptedSenderName, entity.senderNameIv),
            decryptedText = decryptOptionalPayload(entity.encryptedMessageText, entity.messageTextIv),
            postedAt = entity.postedAt,
            notificationKey = entity.notificationKey,
            sourcePackage = entity.sourcePackage,
            parseSource = entity.parseSource,
            isContentUnavailable = entity.isContentUnavailable,
            createdAt = entity.createdAt,
            priority = entity.priority.name
        )
    }

    private fun decryptOptionalPayload(ciphertext: ByteArray?, iv: ByteArray?): String? {
        if (ciphertext == null || iv == null) {
            return null
        }
        return decryptPayloadOrFallback(ciphertext, iv, DECRYPTION_FAILED)
    }

    private fun decryptPayloadOrFallback(ciphertext: ByteArray?, iv: ByteArray?, fallback: String): String {
        if (ciphertext == null || iv == null) {
            return fallback
        }
        return try {
            sensitiveTextCipher.decrypt(EncryptedPayload(ciphertext, iv))
        } catch (e: Exception) {
            DECRYPTION_FAILED
        }
    }

    private fun parseActionableInboxFilters(
        status: String?,
        priority: String?,
        limit: Int
    ): ActionableInboxFilters? {
        val statusFilter = parseActionableStatus(status)
        if (!status.isNullOrBlank() && statusFilter == null) {
            return null
        }
        val priorityFilter = parsePriorityOrNull(priority)
        if (!priority.isNullOrBlank() && priorityFilter == null) {
            return null
        }
        return ActionableInboxFilters(
            status = statusFilter,
            priority = priorityFilter,
            limit = localToolLimit(limit)
        )
    }

    private fun priorityActionableItems(
        messages: List<DecryptedMessage>,
        filters: ActionableInboxFilters,
        followUpMessageIds: Set<String>
    ): List<LocalActionableInboxItem> {
        if (filters.status != null && filters.status != ACTIONABLE_STATUS_PENDING) {
            return emptyList()
        }
        return messages.asSequence()
            .filter { it.priority == InboxPriority.HIGH.name }
            .filter { filters.priority == null || it.priority == filters.priority.name }
            .filter { it.id !in followUpMessageIds }
            .map { it.toPriorityActionableInboxItem() }
            .toList()
    }

    private fun parsePriority(value: String?, default: InboxPriority): InboxPriority {
        return parsePriorityOrNull(value) ?: default
    }

    private fun parsePriorityOrNull(value: String?): InboxPriority? {
        val cleanValue = value?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (cleanValue.isBlank()) {
            return null
        }
        return try {
            InboxPriority.valueOf(cleanValue)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMessagePriority(value: String): InboxPriority? {
        return when (parsePriorityOrNull(value)) {
            InboxPriority.HIGH -> InboxPriority.HIGH
            InboxPriority.NORMAL -> InboxPriority.NORMAL
            else -> null
        }
    }

    private fun parseActionableStatus(value: String?): String? {
        val cleanValue = value?.trim()?.uppercase(Locale.ROOT).orEmpty()
        if (cleanValue.isBlank()) {
            return null
        }
        return cleanValue.takeIf {
            it == ACTIONABLE_STATUS_PENDING || it == ACTIONABLE_STATUS_COMPLETED
        }
    }

    private fun normalizeSearchText(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private fun localToolLimit(limit: Int): Int = limit.coerceIn(MIN_LOCAL_TOOL_LIMIT, MAX_LOCAL_TOOL_LIMIT)

    private companion object {
        const val DEFAULT_SEARCH_LIMIT = 10
        const val MIN_LOCAL_TOOL_LIMIT = 1
        const val MAX_LOCAL_TOOL_LIMIT = 100
        const val MILLIS_PER_MINUTE = 60_000L
        const val ACTIONABLE_TYPE_FOLLOW_UP = "FOLLOW_UP"
        const val ACTIONABLE_TYPE_PRIORITY_MESSAGE = "PRIORITY_MESSAGE"
        const val ACTIONABLE_STATUS_PENDING = "PENDING"
        const val ACTIONABLE_STATUS_COMPLETED = "COMPLETED"
        const val DECRYPTION_FAILED = "[Decryption Failed]"
    }
}
