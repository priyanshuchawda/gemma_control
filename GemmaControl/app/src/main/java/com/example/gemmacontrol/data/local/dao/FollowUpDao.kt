package com.example.gemmacontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gemmacontrol.data.local.entity.FollowUpEntity
import com.example.gemmacontrol.data.local.entity.InboxPriority

@Dao
interface FollowUpDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(followUp: FollowUpEntity): Long

    @Query(
        """
        SELECT * FROM follow_ups
        WHERE completedAt IS NULL
        AND (:priority IS NULL OR priority = :priority)
        ORDER BY createdAt DESC
        LIMIT :limit
        """
    )
    suspend fun getPending(priority: InboxPriority?, limit: Int): List<FollowUpEntity>

    @Query("UPDATE follow_ups SET completedAt = :completedAt WHERE id = :id AND completedAt IS NULL")
    suspend fun markCompleted(id: String, completedAt: Long): Int

    @Query("DELETE FROM follow_ups")
    suspend fun deleteAll()
}
