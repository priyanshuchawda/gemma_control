package com.example.gemmacontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveNotificationReferenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reference: ActiveNotificationReferenceEntity)

    @Query("SELECT * FROM active_notification_references WHERE notificationKey = :notificationKey")
    suspend fun getByKey(notificationKey: String): ActiveNotificationReferenceEntity?

    @Query("SELECT * FROM active_notification_references WHERE removedAt IS NULL")
    fun getActiveReferencesFlow(): Flow<List<ActiveNotificationReferenceEntity>>

    @Query("SELECT * FROM active_notification_references WHERE removedAt IS NULL")
    suspend fun getActiveReferences(): List<ActiveNotificationReferenceEntity>

    @Query("DELETE FROM active_notification_references")
    suspend fun deleteAll()

    @Query(
        """
        DELETE FROM active_notification_references
        WHERE latestMessageEventId IN (
            SELECT id FROM message_events WHERE conversationId = :conversationId
        )
        """
    )
    suspend fun deleteForConversation(conversationId: String)
}
