package com.example.gemmacontrol.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.gemmacontrol.notifications.NotificationParseSource

@Entity(
    tableName = "message_events",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["dedupeToken"], unique = true)
    ]
)
data class MessageEventEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String, // opaque conversation ID
    val encryptedSenderName: ByteArray?,
    val senderNameIv: ByteArray?,
    val encryptedMessageText: ByteArray?,
    val messageTextIv: ByteArray?,
    val postedAt: Long,
    val notificationKey: String,
    val sourcePackage: String,
    val parseSource: NotificationParseSource,
    val dedupeToken: String, // HMAC token, not plaintext SHA-256
    val isContentUnavailable: Boolean,
    val createdAt: Long,
    val priority: InboxPriority = InboxPriority.NORMAL
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEventEntity

        if (id != other.id) return false
        if (conversationId != other.conversationId) return false
        if (encryptedSenderName != null) {
            if (other.encryptedSenderName == null) return false
            if (!encryptedSenderName.contentEquals(other.encryptedSenderName)) return false
        } else if (other.encryptedSenderName != null) return false
        if (senderNameIv != null) {
            if (other.senderNameIv == null) return false
            if (!senderNameIv.contentEquals(other.senderNameIv)) return false
        } else if (other.senderNameIv != null) return false
        if (encryptedMessageText != null) {
            if (other.encryptedMessageText == null) return false
            if (!encryptedMessageText.contentEquals(other.encryptedMessageText)) return false
        } else if (other.encryptedMessageText != null) return false
        if (messageTextIv != null) {
            if (other.messageTextIv == null) return false
            if (!messageTextIv.contentEquals(other.messageTextIv)) return false
        } else if (other.messageTextIv != null) return false
        if (postedAt != other.postedAt) return false
        if (notificationKey != other.notificationKey) return false
        if (sourcePackage != other.sourcePackage) return false
        if (parseSource != other.parseSource) return false
        if (dedupeToken != other.dedupeToken) return false
        if (isContentUnavailable != other.isContentUnavailable) return false
        if (createdAt != other.createdAt) return false
        if (priority != other.priority) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + (encryptedSenderName?.contentHashCode() ?: 0)
        result = 31 * result + (senderNameIv?.contentHashCode() ?: 0)
        result = 31 * result + (encryptedMessageText?.contentHashCode() ?: 0)
        result = 31 * result + (messageTextIv?.contentHashCode() ?: 0)
        result = 31 * result + postedAt.hashCode()
        result = 31 * result + notificationKey.hashCode()
        result = 31 * result + sourcePackage.hashCode()
        result = 31 * result + parseSource.hashCode()
        result = 31 * result + dedupeToken.hashCode()
        result = 31 * result + isContentUnavailable.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + priority.hashCode()
        return result
    }
}
