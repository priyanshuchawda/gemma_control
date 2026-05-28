package com.example.gemmacontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_notification_references")
data class ActiveNotificationReferenceEntity(
    @PrimaryKey
    val notificationKey: String,
    val latestMessageEventId: String?,
    val hadReplyActionWhenSeen: Boolean,
    val lastSeenActiveAt: Long,
    val removedAt: Long?
)
