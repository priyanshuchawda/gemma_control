package com.example.gemmacontrol.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = true
)
@TypeConverters(RoomTypeConverters::class)
abstract class GemmaControlDatabase : RoomDatabase() {

    abstract fun conversationDao(): ConversationDao
    abstract fun messageEventDao(): MessageEventDao
    abstract fun activeNotificationReferenceDao(): ActiveNotificationReferenceDao

    companion object {
        @Volatile
        private var INSTANCE: GemmaControlDatabase? = null

        fun getMigration_1_2(
            cipher: com.example.gemmacontrol.data.crypto.SensitiveTextCipher,
            tokenGen: com.example.gemmacontrol.data.crypto.DedupeTokenGenerator
        ): Migration {
            return object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    try {
                        // 1. Enable secure delete to overwrite legacy plaintext bytes upon drop/delete
                        db.query("PRAGMA secure_delete=ON").close()

                        // 2. Create temporary tables representing the hardened v2 schema
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `conversations_v2` (
                                `id` TEXT NOT NULL, 
                                `encryptedDisplayName` BLOB, 
                                `displayNameIv` BLOB, 
                                `conversationType` TEXT NOT NULL, 
                                `encryptedVerifiedPhoneNumberE164` BLOB, 
                                `verifiedPhoneNumberIv` BLOB, 
                                `createdAt` INTEGER NOT NULL, 
                                `updatedAt` INTEGER NOT NULL, 
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())

                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `message_events_v2` (
                                `id` TEXT NOT NULL, 
                                `conversationId` TEXT NOT NULL, 
                                `encryptedSenderName` BLOB, 
                                `senderNameIv` BLOB, 
                                `encryptedMessageText` BLOB, 
                                `messageTextIv` BLOB, 
                                `postedAt` INTEGER NOT NULL, 
                                `notificationKey` TEXT NOT NULL, 
                                `sourcePackage` TEXT NOT NULL, 
                                `parseSource` TEXT NOT NULL, 
                                `dedupeToken` TEXT NOT NULL, 
                                `isContentUnavailable` INTEGER NOT NULL, 
                                `createdAt` INTEGER NOT NULL, 
                                PRIMARY KEY(`id`), 
                                FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                            )
                        """.trimIndent())

                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_message_events_dedupeToken` ON `message_events_v2` (`dedupeToken`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_message_events_conversationId` ON `message_events_v2` (`conversationId`)")

                        // 3. Migrate existing v1 plaintext data with Keystore-backed GCM-encryption and HMAC hashing
                        val oldConvIdToOpaqueMap = mutableMapOf<String, String>()

                        val convCursor = db.query("SELECT * FROM conversations")
                        try {
                            val idCol = convCursor.getColumnIndex("id")
                            val dispCol = convCursor.getColumnIndex("displayName")
                            val typeCol = convCursor.getColumnIndex("conversationType")
                            val phoneCol = convCursor.getColumnIndex("verifiedPhoneNumberE164")
                            val createdCol = convCursor.getColumnIndex("createdAt")
                            val updatedCol = convCursor.getColumnIndex("updatedAt")

                            if (idCol != -1 && dispCol != -1 && typeCol != -1 && createdCol != -1 && updatedCol != -1) {
                                while (convCursor.moveToNext()) {
                                    val oldId = convCursor.getString(idCol)
                                    val displayName = convCursor.getString(dispCol)
                                    val typeStr = convCursor.getString(typeCol)
                                    val phone = if (phoneCol != -1) convCursor.getString(phoneCol) else null
                                    val createdAt = convCursor.getLong(createdCol)
                                    val updatedAt = convCursor.getLong(updatedCol)

                                    // Generate opaque conversation ID using HMAC-SHA256
                                    val opaqueId = tokenGen.generate("$oldId-$typeStr")
                                    oldConvIdToOpaqueMap[oldId] = opaqueId

                                    // Encrypt display name and phone number
                                    val encTitle = cipher.encrypt(displayName)
                                    val encPhone = if (!phone.isNullOrEmpty()) cipher.encrypt(phone) else null

                                    val values = android.content.ContentValues().apply {
                                        put("id", opaqueId)
                                        put("encryptedDisplayName", encTitle.ciphertext)
                                        put("displayNameIv", encTitle.iv)
                                        put("conversationType", typeStr)
                                        if (encPhone != null) {
                                            put("encryptedVerifiedPhoneNumberE164", encPhone.ciphertext)
                                            put("verifiedPhoneNumberIv", encPhone.iv)
                                        } else {
                                            putNull("encryptedVerifiedPhoneNumberE164")
                                            putNull("verifiedPhoneNumberIv")
                                        }
                                        put("createdAt", createdAt)
                                        put("updatedAt", updatedAt)
                                    }
                                    db.insert("conversations_v2", 4, values) // SQLite ON CONFLICT IGNORE
                                }
                            }
                        } finally {
                            convCursor.close()
                        }

                        val msgCursor = db.query("SELECT * FROM message_events")
                        try {
                            val idCol = msgCursor.getColumnIndex("id")
                            val convCol = msgCursor.getColumnIndex("conversationId")
                            val senderCol = msgCursor.getColumnIndex("senderName")
                            val textCol = msgCursor.getColumnIndex("encryptedMessageText")
                            val ivCol = msgCursor.getColumnIndex("encryptionIv")
                            val postedCol = msgCursor.getColumnIndex("postedAt")
                            val keyCol = msgCursor.getColumnIndex("notificationKey")
                            val pkgCol = msgCursor.getColumnIndex("sourcePackage")
                            val srcCol = msgCursor.getColumnIndex("parseSource")
                            val hashCol = msgCursor.getColumnIndex("dedupeHash")
                            val unavailableCol = msgCursor.getColumnIndex("isContentUnavailable")
                            val createdCol = msgCursor.getColumnIndex("createdAt")

                            if (idCol != -1 && convCol != -1 && senderCol != -1 && textCol != -1 && ivCol != -1 &&
                                postedCol != -1 && keyCol != -1 && pkgCol != -1 && srcCol != -1 && hashCol != -1 &&
                                unavailableCol != -1 && createdCol != -1
                            ) {
                                while (msgCursor.moveToNext()) {
                                    val id = msgCursor.getString(idCol)
                                    val oldConvId = msgCursor.getString(convCol)
                                    val sender = msgCursor.getString(senderCol)
                                    val encText = msgCursor.getBlob(textCol)
                                    val ivText = msgCursor.getBlob(ivCol)
                                    val postedAt = msgCursor.getLong(postedCol)
                                    val key = msgCursor.getString(keyCol)
                                    val pkg = msgCursor.getString(pkgCol)
                                    val parseSrc = msgCursor.getString(srcCol)
                                    val oldHash = msgCursor.getString(hashCol)
                                    val unavailable = msgCursor.getInt(unavailableCol)
                                    val createdAt = msgCursor.getLong(createdCol)

                                    val opaqueConvId = oldConvIdToOpaqueMap[oldConvId] ?: tokenGen.generate("$oldConvId-UNKNOWN")
                                    val encSender = if (!sender.isNullOrEmpty()) cipher.encrypt(sender) else null
                                    val dedupeToken = tokenGen.generate(oldHash)

                                    val values = android.content.ContentValues().apply {
                                        put("id", id)
                                        put("conversationId", opaqueConvId)
                                        if (encSender != null) {
                                            put("encryptedSenderName", encSender.ciphertext)
                                            put("senderNameIv", encSender.iv)
                                        } else {
                                            putNull("encryptedSenderName")
                                            putNull("senderNameIv")
                                        }
                                        if (encText != null && ivText != null) {
                                            put("encryptedMessageText", encText)
                                            put("messageTextIv", ivText)
                                        } else {
                                            putNull("encryptedMessageText")
                                            putNull("messageTextIv")
                                        }
                                        put("postedAt", postedAt)
                                        put("notificationKey", key)
                                        put("sourcePackage", pkg)
                                        put("parseSource", parseSrc)
                                        put("dedupeToken", dedupeToken)
                                        put("isContentUnavailable", unavailable)
                                        put("createdAt", createdAt)
                                    }
                                    db.insert("message_events_v2", 4, values) // SQLite ON CONFLICT IGNORE
                                }
                            }
                        } finally {
                            msgCursor.close()
                        }

                        // 4. Drop v1 legacy tables and rename v2 tables
                        db.execSQL("DROP TABLE conversations")
                        db.execSQL("DROP TABLE message_events")
                        db.execSQL("ALTER TABLE conversations_v2 RENAME TO conversations")
                        db.execSQL("ALTER TABLE message_events_v2 RENAME TO message_events")
                    } catch (e: Exception) {
                        throw IllegalStateException("Migration failed: secure storage is unavailable.", e)
                    }
                }
            }
        }

        val MIGRATION_1_2: Migration by lazy {
            getMigration_1_2(
                com.example.gemmacontrol.data.crypto.AndroidKeystoreSensitiveTextCipher(),
                com.example.gemmacontrol.data.crypto.AndroidKeystoreHmacDedupeTokenGenerator()
            )
        }

        fun getDatabase(context: Context): GemmaControlDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GemmaControlDatabase::class.java,
                    "gemma_control_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
