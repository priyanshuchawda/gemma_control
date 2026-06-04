package com.example.gemmacontrol.data.repository

import androidx.room.withTransaction
import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.crypto.SensitiveTextCipher
import com.example.gemmacontrol.data.crypto.DedupeTokenGenerator
import com.example.gemmacontrol.data.local.GemmaControlDatabase
import com.example.gemmacontrol.data.local.dao.ActiveNotificationReferenceDao
import com.example.gemmacontrol.data.local.dao.ConversationDao
import com.example.gemmacontrol.data.local.dao.MessageEventDao
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import com.example.gemmacontrol.data.local.entity.ConversationEntity
import com.example.gemmacontrol.data.local.entity.MessageEventEntity
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.ParsedMessagePreview
import com.example.gemmacontrol.notifications.ParsedWhatsAppNotificationEvent
import java.util.UUID

internal class StoredInboxEventPersistence(
    private val conversationDao: ConversationDao,
    private val messageEventDao: MessageEventDao,
    private val activeNotificationReferenceDao: ActiveNotificationReferenceDao,
    private val sensitiveTextCipher: SensitiveTextCipher,
    private val dedupeTokenGenerator: DedupeTokenGenerator,
    private val db: GemmaControlDatabase?,
    private val nowProvider: () -> Long = System::currentTimeMillis
) {
    suspend fun persist(
        event: ParsedWhatsAppNotificationEvent
    ): StoredInboxRepository.PersistCanonicalResult {
        val now = nowProvider()
        val latestMessage = event.messages.lastOrNull()
        val cryptoOutputs = buildCryptoOutputs(event, latestMessage)
            ?: return StoredInboxRepository.PersistCanonicalResult.SecureStorageUnavailable

        val persistDbOps = suspend {
            persistEntities(
                event = event,
                latestMessage = latestMessage,
                now = now,
                cryptoOutputs = cryptoOutputs
            )
        }
        return if (db != null) {
            db.withTransaction { persistDbOps() }
        } else {
            persistDbOps()
        }
    }

    private fun buildCryptoOutputs(
        event: ParsedWhatsAppNotificationEvent,
        latestMessage: ParsedMessagePreview?
    ): CryptoOutputs? {
        return try {
            val conversationTitle = event.conversationTitle ?: DEFAULT_CONVERSATION_TITLE
            CryptoOutputs(
                conversationOpaqueId = dedupeTokenGenerator.generate("${conversationTitle}-${event.conversationType.name}"),
                displayName = sensitiveTextCipher.encrypt(conversationTitle),
                senderName = encryptOptional(latestMessage?.senderName.orEmpty()),
                messageText = encryptOptional(latestMessage?.messageText.orEmpty()),
                dedupeToken = dedupeTokenGenerator.generate(event.dedupeCandidate ?: UUID.randomUUID().toString())
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun encryptOptional(value: String): EncryptedPayload? {
        return value.takeIf { it.isNotEmpty() }?.let { sensitiveTextCipher.encrypt(it) }
    }

    private suspend fun persistEntities(
        event: ParsedWhatsAppNotificationEvent,
        latestMessage: ParsedMessagePreview?,
        now: Long,
        cryptoOutputs: CryptoOutputs
    ): StoredInboxRepository.PersistCanonicalResult {
        upsertConversation(event, now, cryptoOutputs)
        val inserted = insertMessage(event, latestMessage, now, cryptoOutputs)
        updateActiveReference(event, inserted.messageId, now)
        return if (inserted.isDuplicate) {
            StoredInboxRepository.PersistCanonicalResult.DuplicateReferenced
        } else {
            StoredInboxRepository.PersistCanonicalResult.Stored(inserted.messageId)
        }
    }

    private suspend fun upsertConversation(
        event: ParsedWhatsAppNotificationEvent,
        now: Long,
        cryptoOutputs: CryptoOutputs
    ) {
        val existingConversation = conversationDao.getById(cryptoOutputs.conversationOpaqueId)
        val conversationType = event.conversationType.takeUnless { it == ConversationType.UNKNOWN }
            ?: existingConversation?.conversationType
            ?: ConversationType.UNKNOWN

        val conversation = existingConversation?.copy(
            encryptedDisplayName = cryptoOutputs.displayName.ciphertext,
            displayNameIv = cryptoOutputs.displayName.iv,
            conversationType = conversationType,
            updatedAt = now
        ) ?: ConversationEntity(
            id = cryptoOutputs.conversationOpaqueId,
            encryptedDisplayName = cryptoOutputs.displayName.ciphertext,
            displayNameIv = cryptoOutputs.displayName.iv,
            conversationType = conversationType,
            encryptedVerifiedPhoneNumberE164 = null,
            verifiedPhoneNumberIv = null,
            createdAt = now,
            updatedAt = now
        )

        if (existingConversation == null) {
            conversationDao.insert(conversation)
        } else {
            conversationDao.update(conversation)
        }
    }

    private suspend fun insertMessage(
        event: ParsedWhatsAppNotificationEvent,
        latestMessage: ParsedMessagePreview?,
        now: Long,
        cryptoOutputs: CryptoOutputs
    ): MessageInsertResolution {
        val messageId = UUID.randomUUID().toString()
        val messageEntity = MessageEventEntity(
            id = messageId,
            conversationId = cryptoOutputs.conversationOpaqueId,
            encryptedSenderName = cryptoOutputs.senderName?.ciphertext,
            senderNameIv = cryptoOutputs.senderName?.iv,
            encryptedMessageText = cryptoOutputs.messageText?.ciphertext,
            messageTextIv = cryptoOutputs.messageText?.iv,
            postedAt = latestMessage?.timestamp ?: event.notificationPostedAt ?: now,
            notificationKey = event.notificationKey,
            sourcePackage = event.packageName,
            parseSource = event.parseSource,
            dedupeToken = cryptoOutputs.dedupeToken,
            isContentUnavailable = event.isContentUnavailable,
            createdAt = now
        )

        val insertResult = messageEventDao.insert(messageEntity)
        if (insertResult != DUPLICATE_INSERT_RESULT) {
            return MessageInsertResolution(messageId = messageId, isDuplicate = false)
        }

        val existing = messageEventDao.getByDedupeToken(cryptoOutputs.dedupeToken)
        return MessageInsertResolution(
            messageId = existing?.id ?: messageId,
            isDuplicate = true
        )
    }

    private suspend fun updateActiveReference(
        event: ParsedWhatsAppNotificationEvent,
        messageId: String,
        now: Long
    ) {
        activeNotificationReferenceDao.insertOrUpdate(
            ActiveNotificationReferenceEntity(
                notificationKey = event.notificationKey,
                latestMessageEventId = messageId,
                hadReplyActionWhenSeen = event.hasReplyActionAtCaptureTime,
                lastSeenActiveAt = now,
                removedAt = null
            )
        )
    }

    private data class CryptoOutputs(
        val conversationOpaqueId: String,
        val displayName: EncryptedPayload,
        val senderName: EncryptedPayload?,
        val messageText: EncryptedPayload?,
        val dedupeToken: String
    )

    private data class MessageInsertResolution(
        val messageId: String,
        val isDuplicate: Boolean
    )

    private companion object {
        const val DEFAULT_CONVERSATION_TITLE = "WhatsApp Chat"
        const val DUPLICATE_INSERT_RESULT = -1L
    }
}
