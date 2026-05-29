package com.example.gemmacontrol.data.repository

import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.crypto.SensitiveTextCipher
import com.example.gemmacontrol.data.crypto.DedupeTokenGenerator
import com.example.gemmacontrol.data.local.dao.ActiveNotificationReferenceDao
import com.example.gemmacontrol.data.local.dao.ConversationDao
import com.example.gemmacontrol.data.local.dao.MessageEventDao
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import com.example.gemmacontrol.data.local.entity.ConversationEntity
import com.example.gemmacontrol.data.local.entity.MessageEventEntity
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationParseSource
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

import androidx.room.withTransaction
import com.example.gemmacontrol.ai.tools.LocalWhatsAppDataRepository
import com.example.gemmacontrol.data.local.GemmaControlDatabase
import java.util.Locale

class StoredInboxRepository(
    private val conversationDao: ConversationDao,
    private val messageEventDao: MessageEventDao,
    private val activeNotificationReferenceDao: ActiveNotificationReferenceDao,
    private val sensitiveTextCipher: SensitiveTextCipher,
    private val dedupeTokenGenerator: DedupeTokenGenerator,
    private val db: GemmaControlDatabase? = null
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
        val createdAt: Long
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

    private fun decryptConversationTitle(entity: ConversationEntity): String {
        return if (entity.encryptedDisplayName != null && entity.displayNameIv != null) {
            try {
                sensitiveTextCipher.decrypt(EncryptedPayload(entity.encryptedDisplayName, entity.displayNameIv))
            } catch (e: Exception) {
                "[Decryption Failed]"
            }
        } else {
            entity.id
        }
    }

    private fun normalizeConversationName(value: String): String {
        return value.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase(Locale.ROOT)
    }

    private fun decryptMessageEntity(entity: MessageEventEntity, conversationMap: Map<String, String>): DecryptedMessage {
        val decryptedText = if (entity.encryptedMessageText != null && entity.messageTextIv != null) {
            try {
                sensitiveTextCipher.decrypt(
                    EncryptedPayload(entity.encryptedMessageText, entity.messageTextIv)
                )
            } catch (e: Exception) {
                "[Decryption Failed]"
            }
        } else {
            null
        }

        val decryptedSender = if (entity.encryptedSenderName != null && entity.senderNameIv != null) {
            try {
                sensitiveTextCipher.decrypt(
                    EncryptedPayload(entity.encryptedSenderName, entity.senderNameIv)
                )
            } catch (e: Exception) {
                "[Decryption Failed]"
            }
        } else {
            null
        }

        val conversationTitle = conversationMap[entity.conversationId] ?: entity.conversationId

        return DecryptedMessage(
            id = entity.id,
            conversationId = conversationTitle,
            senderName = decryptedSender,
            decryptedText = decryptedText,
            postedAt = entity.postedAt,
            notificationKey = entity.notificationKey,
            sourcePackage = entity.sourcePackage,
            parseSource = entity.parseSource,
            isContentUnavailable = entity.isContentUnavailable,
            createdAt = entity.createdAt
        )
    }
}
