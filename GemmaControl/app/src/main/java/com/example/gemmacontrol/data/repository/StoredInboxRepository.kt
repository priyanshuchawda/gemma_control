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

class StoredInboxRepository(
    private val conversationDao: ConversationDao,
    private val messageEventDao: MessageEventDao,
    private val activeNotificationReferenceDao: ActiveNotificationReferenceDao,
    private val sensitiveTextCipher: SensitiveTextCipher,
    private val dedupeTokenGenerator: DedupeTokenGenerator
) {
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

    suspend fun persistCanonicalEvent(event: ParsedWhatsAppNotificationEvent) {
        val now = System.currentTimeMillis()

        // 1. Generate opaque conversation ID and encrypt conversation display name
        val identityMaterial = "${event.conversationTitle ?: "WhatsApp Chat"}-${event.conversationType.name}"
        val conversationOpaqueId = dedupeTokenGenerator.generate(identityMaterial)

        val conversationTitleToStore = event.conversationTitle ?: "WhatsApp Chat"
        val encryptedTitlePayload = sensitiveTextCipher.encrypt(conversationTitleToStore)

        val existingConversation = conversationDao.getById(conversationOpaqueId)
        if (existingConversation == null) {
            val conversation = ConversationEntity(
                id = conversationOpaqueId,
                encryptedDisplayName = encryptedTitlePayload.ciphertext,
                displayNameIv = encryptedTitlePayload.iv,
                conversationType = event.conversationType,
                encryptedVerifiedPhoneNumberE164 = null,
                verifiedPhoneNumberIv = null,
                createdAt = now,
                updatedAt = now
            )
            conversationDao.insert(conversation)
        } else {
            val updated = existingConversation.copy(
                encryptedDisplayName = encryptedTitlePayload.ciphertext,
                displayNameIv = encryptedTitlePayload.iv,
                conversationType = if (event.conversationType != ConversationType.UNKNOWN) event.conversationType else existingConversation.conversationType,
                updatedAt = now
            )
            conversationDao.update(updated)
        }

        // 2. Encrypt sender name and message event details
        val latestMessage = event.messages.lastOrNull()
        
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

        val messageId = UUID.randomUUID().toString()
        val messageEntity = MessageEventEntity(
            id = messageId,
            conversationId = conversationOpaqueId,
            encryptedSenderName = encryptedSenderPayload?.ciphertext,
            senderNameIv = encryptedSenderPayload?.iv,
            encryptedMessageText = encryptedMessagePayload?.ciphertext,
            messageTextIv = encryptedMessagePayload?.iv,
            postedAt = latestMessage?.timestamp ?: event.notificationPostedAt ?: now,
            notificationKey = event.notificationKey,
            sourcePackage = event.packageName,
            parseSource = event.parseSource,
            dedupeToken = dedupeTokenToStore,
            isContentUnavailable = event.isContentUnavailable,
            createdAt = now
        )

        val insertResult = messageEventDao.insert(messageEntity)
        val actualMessageId = if (insertResult == -1L) {
            val existing = messageEventDao.getByDedupeToken(dedupeTokenToStore)
            existing?.id ?: messageId
        } else {
            messageId
        }

        // 3. Track active reference
        val reference = ActiveNotificationReferenceEntity(
            notificationKey = event.notificationKey,
            latestMessageEventId = actualMessageId,
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
            val conversations = conversationDao.getAllConversations()
            val titleMap = conversations.associate { conv ->
                val decryptedTitle = if (conv.encryptedDisplayName != null && conv.displayNameIv != null) {
                    try {
                        sensitiveTextCipher.decrypt(EncryptedPayload(conv.encryptedDisplayName, conv.displayNameIv))
                    } catch (e: Exception) {
                        "[Decryption Failed]"
                    }
                } else {
                    conv.id
                }
                conv.id to decryptedTitle
            }
            entities.map { decryptMessageEntity(it, titleMap) }
        }
    }

    suspend fun getAllDecryptedMessages(): List<DecryptedMessage> {
        val conversations = conversationDao.getAllConversations()
        val titleMap = conversations.associate { conv ->
            val decryptedTitle = if (conv.encryptedDisplayName != null && conv.displayNameIv != null) {
                try {
                    sensitiveTextCipher.decrypt(EncryptedPayload(conv.encryptedDisplayName, conv.displayNameIv))
                } catch (e: Exception) {
                    "[Decryption Failed]"
                }
            } else {
                conv.id
            }
            conv.id to decryptedTitle
        }
        return messageEventDao.getAllMessages().map { decryptMessageEntity(it, titleMap) }
    }

    fun getStoredConversationsFlow(): Flow<List<StoredConversation>> {
        return conversationDao.getAllConversationsFlow().map { conversations ->
            conversations.map { conversation ->
                val latestMessageEntity = messageEventDao.getMessagesForConversation(conversation.id).firstOrNull()
                val latestMessage = latestMessageEntity?.let {
                    val title = if (conversation.encryptedDisplayName != null && conversation.displayNameIv != null) {
                        try {
                            sensitiveTextCipher.decrypt(EncryptedPayload(conversation.encryptedDisplayName, conversation.displayNameIv))
                        } catch (e: Exception) {
                            "[Decryption Failed]"
                        }
                    } else {
                        conversation.id
                    }
                    decryptMessageEntity(it, mapOf(conversation.id to title))
                }
                val decryptedDisplayName = if (conversation.encryptedDisplayName != null && conversation.displayNameIv != null) {
                    try {
                        sensitiveTextCipher.decrypt(EncryptedPayload(conversation.encryptedDisplayName, conversation.displayNameIv))
                    } catch (e: Exception) {
                        "[Decryption Failed]"
                    }
                } else {
                    conversation.id
                }
                StoredConversation(conversation, decryptedDisplayName, latestMessage)
            }
        }
    }

    suspend fun deleteAllData() {
        messageEventDao.deleteAllData()
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
