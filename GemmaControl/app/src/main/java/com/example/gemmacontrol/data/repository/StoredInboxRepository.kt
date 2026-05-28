package com.example.gemmacontrol.data.repository

import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.crypto.MessageBodyCipher
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

class StoredInboxRepository(
    private val conversationDao: ConversationDao,
    private val messageEventDao: MessageEventDao,
    private val activeNotificationReferenceDao: ActiveNotificationReferenceDao,
    private val messageBodyCipher: MessageBodyCipher
) {
    data class DecryptedMessage(
        val id: String,
        val conversationId: String,
        val senderName: String?,
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
        val latestMessage: DecryptedMessage?
    )

    suspend fun persistCanonicalEvent(event: ParsedWhatsAppNotificationEvent) {
        val conversationId = event.conversationTitle ?: "Unknown Chat"
        val now = System.currentTimeMillis()

        // 1. Ensure conversation exists
        val existingConversation = conversationDao.getById(conversationId)
        if (existingConversation == null) {
            val conversation = ConversationEntity(
                id = conversationId,
                displayName = event.conversationTitle ?: "WhatsApp Chat",
                conversationType = event.conversationType,
                verifiedPhoneNumberE164 = null,
                createdAt = now,
                updatedAt = now
            )
            conversationDao.insert(conversation)
        } else {
            val updated = existingConversation.copy(
                displayName = event.conversationTitle ?: existingConversation.displayName,
                conversationType = if (event.conversationType != ConversationType.UNKNOWN) event.conversationType else existingConversation.conversationType,
                updatedAt = now
            )
            conversationDao.update(updated)
        }

        // 2. Encrypt and persist message event
        val latestMessage = event.messages.lastOrNull()
        val textToEncrypt = latestMessage?.messageText ?: ""
        
        val encryptedPayload = if (textToEncrypt.isNotEmpty()) {
            messageBodyCipher.encrypt(textToEncrypt)
        } else {
            null
        }

        val messageId = UUID.randomUUID().toString()
        val messageEntity = MessageEventEntity(
            id = messageId,
            conversationId = conversationId,
            senderName = latestMessage?.senderName,
            encryptedMessageText = encryptedPayload?.ciphertext,
            encryptionIv = encryptedPayload?.iv,
            postedAt = latestMessage?.timestamp ?: event.notificationPostedAt ?: now,
            notificationKey = event.notificationKey,
            sourcePackage = event.packageName,
            parseSource = event.parseSource,
            dedupeHash = event.dedupeCandidate ?: UUID.randomUUID().toString(),
            isContentUnavailable = event.isContentUnavailable,
            createdAt = now
        )

        messageEventDao.insert(messageEntity)

        // 3. Track active reference
        val reference = ActiveNotificationReferenceEntity(
            notificationKey = event.notificationKey,
            latestMessageEventId = messageId,
            hadReplyActionWhenSeen = event.hasReplyActionAtCaptureTime,
            lastSeenActiveAt = now,
            removedAt = null
        )
        activeNotificationReferenceDao.insertOrUpdate(reference)
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
            entities.map { decryptMessageEntity(it) }
        }
    }

    suspend fun getAllDecryptedMessages(): List<DecryptedMessage> {
        return messageEventDao.getAllMessages().map { decryptMessageEntity(it) }
    }

    fun getStoredConversationsFlow(): Flow<List<StoredConversation>> {
        return conversationDao.getAllConversationsFlow().map { conversations ->
            conversations.map { conversation ->
                val latestMessageEntity = messageEventDao.getMessagesForConversation(conversation.id).firstOrNull()
                val latestMessage = latestMessageEntity?.let { decryptMessageEntity(it) }
                StoredConversation(conversation, latestMessage)
            }
        }
    }

    suspend fun deleteAllData() {
        messageEventDao.deleteAll()
        conversationDao.deleteAll()
        activeNotificationReferenceDao.deleteAll()
    }

    private fun decryptMessageEntity(entity: MessageEventEntity): DecryptedMessage {
        val decryptedText = if (entity.encryptedMessageText != null && entity.encryptionIv != null) {
            try {
                messageBodyCipher.decrypt(
                    EncryptedPayload(entity.encryptedMessageText, entity.encryptionIv)
                )
            } catch (e: Exception) {
                "[Decryption Failed]"
            }
        } else {
            null
        }

        return DecryptedMessage(
            id = entity.id,
            conversationId = entity.conversationId,
            senderName = entity.senderName,
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
