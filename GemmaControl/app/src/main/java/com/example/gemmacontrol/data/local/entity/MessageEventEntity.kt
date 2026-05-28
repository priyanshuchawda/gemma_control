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
        Index(value = ["dedupeHash"], unique = true)
    ]
)
data class MessageEventEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val senderName: String?,
    val encryptedMessageText: ByteArray?,
    val encryptionIv: ByteArray?,
    val postedAt: Long,
    val notificationKey: String,
    val sourcePackage: String,
    val parseSource: NotificationParseSource,
    val dedupeHash: String,
    val isContentUnavailable: Boolean,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MessageEventEntity

        if (id != other.id) return false
        if (conversationId != other.conversationId) return false
        if (senderName != other.senderName) return false
        if (encryptedMessageText != null) {
            if (other.encryptedMessageText == null) return false
            if (!encryptedMessageText.contentEquals(other.encryptedMessageText)) return false
        } else if (other.encryptedMessageText != null) return false
        if (encryptionIv != null) {
            if (other.encryptionIv == null) return false
            if (!encryptionIv.contentEquals(other.encryptionIv)) return false
        } else if (other.encryptionIv != null) return false
        if (postedAt != other.postedAt) return false
        if (notificationKey != other.notificationKey) return false
        if (sourcePackage != other.sourcePackage) return false
        if (parseSource != other.parseSource) return false
        if (dedupeHash != other.dedupeHash) return false
        if (isContentUnavailable != other.isContentUnavailable) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + conversationId.hashCode()
        result = 31 * result + (senderName?.hashCode() ?: 0)
        result = 31 * result + (encryptedMessageText?.contentHashCode() ?: 0)
        result = 31 * result + (encryptionIv?.contentHashCode() ?: 0)
        result = 31 * result + postedAt.hashCode()
        result = 31 * result + notificationKey.hashCode()
        result = 31 * result + sourcePackage.hashCode()
        result = 31 * result + parseSource.hashCode()
        result = 31 * result + dedupeHash.hashCode()
        result = 31 * result + isContentUnavailable.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
