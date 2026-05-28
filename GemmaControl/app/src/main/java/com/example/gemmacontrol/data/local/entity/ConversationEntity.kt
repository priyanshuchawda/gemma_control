package com.example.gemmacontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gemmacontrol.notifications.ConversationType

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String, // opaque keyed token only
    val encryptedDisplayName: ByteArray?,
    val displayNameIv: ByteArray?,
    val conversationType: ConversationType,
    val encryptedVerifiedPhoneNumberE164: ByteArray? = null,
    val verifiedPhoneNumberIv: ByteArray? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConversationEntity

        if (id != other.id) return false
        if (encryptedDisplayName != null) {
            if (other.encryptedDisplayName == null) return false
            if (!encryptedDisplayName.contentEquals(other.encryptedDisplayName)) return false
        } else if (other.encryptedDisplayName != null) return false
        if (displayNameIv != null) {
            if (other.displayNameIv == null) return false
            if (!displayNameIv.contentEquals(other.displayNameIv)) return false
        } else if (other.displayNameIv != null) return false
        if (conversationType != other.conversationType) return false
        if (encryptedVerifiedPhoneNumberE164 != null) {
            if (other.encryptedVerifiedPhoneNumberE164 == null) return false
            if (!encryptedVerifiedPhoneNumberE164.contentEquals(other.encryptedVerifiedPhoneNumberE164)) return false
        } else if (other.encryptedVerifiedPhoneNumberE164 != null) return false
        if (verifiedPhoneNumberIv != null) {
            if (other.verifiedPhoneNumberIv == null) return false
            if (!verifiedPhoneNumberIv.contentEquals(other.verifiedPhoneNumberIv)) return false
        } else if (other.verifiedPhoneNumberIv != null) return false
        if (createdAt != other.createdAt) return false
        if (updatedAt != other.updatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (encryptedDisplayName?.contentHashCode() ?: 0)
        result = 31 * result + (displayNameIv?.contentHashCode() ?: 0)
        result = 31 * result + conversationType.hashCode()
        result = 31 * result + (encryptedVerifiedPhoneNumberE164?.contentHashCode() ?: 0)
        result = 31 * result + (verifiedPhoneNumberIv?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
