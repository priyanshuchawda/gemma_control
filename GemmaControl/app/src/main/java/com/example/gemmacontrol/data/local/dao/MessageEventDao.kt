package com.example.gemmacontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gemmacontrol.data.local.entity.MessageEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEventEntity): Long

    @Query("SELECT * FROM message_events ORDER BY postedAt DESC")
    fun getAllMessagesFlow(): Flow<List<MessageEventEntity>>

    @Query("SELECT * FROM message_events ORDER BY postedAt DESC")
    suspend fun getAllMessages(): List<MessageEventEntity>

    @Query("SELECT * FROM message_events WHERE conversationId = :conversationId ORDER BY postedAt DESC")
    fun getMessagesForConversationFlow(conversationId: String): Flow<List<MessageEventEntity>>

    @Query("SELECT * FROM message_events WHERE conversationId = :conversationId ORDER BY postedAt DESC")
    suspend fun getMessagesForConversation(conversationId: String): List<MessageEventEntity>

    @Query("DELETE FROM message_events")
    suspend fun deleteAll()
}
