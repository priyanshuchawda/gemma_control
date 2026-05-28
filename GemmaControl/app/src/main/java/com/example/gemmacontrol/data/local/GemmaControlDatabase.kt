package com.example.gemmacontrol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gemmacontrol.data.local.dao.ActiveNotificationReferenceDao
import com.example.gemmacontrol.data.local.dao.ConversationDao
import com.example.gemmacontrol.data.local.dao.MessageEventDao
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import com.example.gemmacontrol.data.local.entity.ConversationEntity
import com.example.gemmacontrol.data.local.entity.MessageEventEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEventEntity::class,
        ActiveNotificationReferenceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class)
abstract class GemmaControlDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageEventDao(): MessageEventDao
    abstract fun activeNotificationReferenceDao(): ActiveNotificationReferenceDao

    companion object {
        @Volatile
        private var INSTANCE: GemmaControlDatabase? = null

        fun getDatabase(context: Context): GemmaControlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GemmaControlDatabase::class.java,
                    "gemma_control_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
