package com.example.gemmacontrol.data.local

import androidx.room.TypeConverter
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationParseSource

class RoomTypeConverters {
    @TypeConverter
    fun fromConversationType(value: ConversationType): String = value.name

    @TypeConverter
    fun toConversationType(value: String): ConversationType = try {
        ConversationType.valueOf(value)
    } catch (e: Exception) {
        ConversationType.UNKNOWN
    }

    @TypeConverter
    fun fromParseSource(value: NotificationParseSource): String = value.name

    @TypeConverter
    fun toParseSource(value: String): NotificationParseSource = try {
        NotificationParseSource.valueOf(value)
    } catch (e: Exception) {
        NotificationParseSource.UNAVAILABLE
    }
}
