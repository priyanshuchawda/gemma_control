package com.example.gemmacontrol.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "follow_ups",
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
        Index(value = ["completedAt"]),
        Index(value = ["priority"])
    ]
)
data class FollowUpEntity(
    @PrimaryKey
    val id: String,
    val messageEventId: String,
    val title: String,
    val dueAt: String?,
    val priority: InboxPriority,
    val createdAt: Long,
    val completedAt: Long?
)
