package com.example.gemmacontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.gemmacontrol.notifications.ConversationType

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val conversationType: ConversationType,
    val verifiedPhoneNumberE164: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
