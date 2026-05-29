package com.example.gemmacontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gemmacontrol.data.local.entity.ReminderEntity

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("UPDATE reminders SET scheduledWorkName = :workName WHERE id = :id")
    suspend fun setScheduledWorkName(id: String, workName: String): Int

    @Query("UPDATE reminders SET deliveredAt = :deliveredAt WHERE id = :id AND deliveredAt IS NULL")
    suspend fun markDelivered(id: String, deliveredAt: Long): Int
}
