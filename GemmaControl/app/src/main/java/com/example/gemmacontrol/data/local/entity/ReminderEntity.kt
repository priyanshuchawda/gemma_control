package com.example.gemmacontrol.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = MessageEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageEventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageEventId"]),
        Index(value = ["remindAtEpochMillis"]),
        Index(value = ["deliveredAt"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val messageEventId: String,
    val encryptedReminderNote: ByteArray?,
    val reminderNoteIv: ByteArray?,
    val remindAtEpochMillis: Long,
    val createdAt: Long,
    val scheduledWorkName: String?,
    val deliveredAt: Long?
)
