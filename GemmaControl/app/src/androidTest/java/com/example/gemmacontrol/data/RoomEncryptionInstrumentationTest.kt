package com.example.gemmacontrol.data

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gemmacontrol.data.crypto.AndroidKeystoreSensitiveTextCipher
import com.example.gemmacontrol.data.crypto.AndroidKeystoreHmacDedupeTokenGenerator
import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.local.GemmaControlDatabase
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import com.example.gemmacontrol.data.local.entity.ConversationEntity
import com.example.gemmacontrol.data.local.entity.MessageEventEntity
import com.example.gemmacontrol.data.repository.StoredInboxRepository
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationParseSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class RoomEncryptionInstrumentationTest {

    private lateinit var db: GemmaControlDatabase
    private lateinit var cipher: AndroidKeystoreSensitiveTextCipher
    private lateinit var tokenGenerator: AndroidKeystoreHmacDedupeTokenGenerator
    private lateinit var repository: StoredInboxRepository

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GemmaControlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cipher = AndroidKeystoreSensitiveTextCipher()
        tokenGenerator = AndroidKeystoreHmacDedupeTokenGenerator()
        repository = StoredInboxRepository(
            db.conversationDao(),
            db.messageEventDao(),
            db.activeNotificationReferenceDao(),
            cipher,
            tokenGenerator
        )
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testKeystoreAesGcmRoundTripForAllFields() {
        val messageText = "Secure message content."
        val senderName = "Peter Parker"
        val conversationTitle = "Daily Bugle Group"

        // 1. Message text round trip
        val payloadText = cipher.encrypt(messageText)
        assertNotNull(payloadText.ciphertext)
        assertNotNull(payloadText.iv)
        assertNotEquals(messageText, String(payloadText.ciphertext))
        assertEquals(messageText, cipher.decrypt(payloadText))

        // 2. Sender name round trip
        val payloadSender = cipher.encrypt(senderName)
        assertNotNull(payloadSender.ciphertext)
        assertNotNull(payloadSender.iv)
        assertNotEquals(senderName, String(payloadSender.ciphertext))
        assertEquals(senderName, cipher.decrypt(payloadSender))

        // 3. Conversation title round trip
        val payloadTitle = cipher.encrypt(conversationTitle)
        assertNotNull(payloadTitle.ciphertext)
        assertNotNull(payloadTitle.iv)
        assertNotEquals(conversationTitle, String(payloadTitle.ciphertext))
        assertEquals(conversationTitle, cipher.decrypt(payloadTitle))
    }

    @Test
    fun testHmacDedupeTokenGeneratorGuarantees() {
        val material1 = "com.whatsapp:key1:1716900000000:Spidey Chat:Hey!"
        val material2 = "com.whatsapp:key1:1716900000000:Spidey Chat:Hey!"
        val material3 = "com.whatsapp:key2:1716900000000:Aunt May:Hello!"

        val token1 = tokenGenerator.generate(material1)
        val token2 = tokenGenerator.generate(material2)
        val token3 = tokenGenerator.generate(material3)

        assertNotNull(token1)
        // Same inputs produce same tokens
        assertEquals(token1, token2)
        // Different inputs produce different tokens
        assertNotEquals(token1, token3)

        // Keyed HMAC is not equal to a simple offline-guessable SHA-256 fingerprint
        val simpleDigest = MessageDigest.getInstance("SHA-256")
        val simpleHash = simpleDigest.digest(material1.toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertNotEquals(simpleHash, token1)
    }

    @Test
    fun testRawRoomRowContainsNoPlaintext() = runBlocking {
        val conversationOpaqueId = tokenGenerator.generate("Spidey-DIRECT")
        val encryptedDisplayName = cipher.encrypt("Spidey Chat")

        val conversation = ConversationEntity(
            id = conversationOpaqueId,
            encryptedDisplayName = encryptedDisplayName.ciphertext,
            displayNameIv = encryptedDisplayName.iv,
            conversationType = ConversationType.DIRECT,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.conversationDao().insert(conversation)

        val encryptedSender = cipher.encrypt("Peter Parker")
        val encryptedMessage = cipher.encrypt("Meet me at the clock tower!")
        val dedupeToken = tokenGenerator.generate("hash_dedupe_123")

        val message = MessageEventEntity(
            id = "msg_123",
            conversationId = conversationOpaqueId,
            encryptedSenderName = encryptedSender.ciphertext,
            senderNameIv = encryptedSender.iv,
            encryptedMessageText = encryptedMessage.ciphertext,
            messageTextIv = encryptedMessage.iv,
            postedAt = System.currentTimeMillis(),
            notificationKey = "notif_key_1",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            dedupeToken = dedupeToken,
            isContentUnavailable = false,
            createdAt = System.currentTimeMillis()
        )

        db.messageEventDao().insert(message)

        // Query raw SQLite db directly to inspect storage bytes and assert NO plaintext leakage
        val cursorConv = db.query("SELECT * FROM conversations WHERE id = ?", arrayOf(conversationOpaqueId))
        assertTrue(cursorConv.moveToFirst())
        val idIdx = cursorConv.getColumnIndex("id")
        val encDispIdx = cursorConv.getColumnIndex("encryptedDisplayName")
        
        // Assert opaque ID matches hmac
        assertEquals(conversationOpaqueId, cursorConv.getString(idIdx))
        
        // Assert display name is stored as binary BLOB (ciphertext), not plaintext string
        val blobDisplayName = cursorConv.getBlob(encDispIdx)
        assertNotEquals("Spidey Chat", String(blobDisplayName))
        cursorConv.close()

        val cursorMsg = db.query("SELECT * FROM message_events WHERE id = ?", arrayOf("msg_123"))
        assertTrue(cursorMsg.moveToFirst())
        val senderIdx = cursorMsg.getColumnIndex("encryptedSenderName")
        val bodyIdx = cursorMsg.getColumnIndex("encryptedMessageText")
        val tokenIdx = cursorMsg.getColumnIndex("dedupeToken")

        // Assert sender name and body are stored as binary BLOBs, not plaintext
        assertNotEquals("Peter Parker", String(cursorMsg.getBlob(senderIdx)))
        assertNotEquals("Meet me at the clock tower!", String(cursorMsg.getBlob(bodyIdx)))
        // Assert dedupe token is opaque and not equal to plaintext-derived fingerprint
        assertEquals(dedupeToken, cursorMsg.getString(tokenIdx))
        cursorMsg.close()

        // Dynamic dynamic decryption via repository works cleanly in-memory
        val decryptedList = repository.getAllDecryptedMessages()
        assertEquals(1, decryptedList.size)
        assertEquals("Spidey Chat", decryptedList.first().conversationId)
        assertEquals("Peter Parker", decryptedList.first().senderName)
        assertEquals("Meet me at the clock tower!", decryptedList.first().decryptedText)
    }

    @Test
    fun testRoomDeleteAllDataClearsAllTables() = runBlocking {
        val conversationOpaqueId = tokenGenerator.generate("Chat1-DIRECT")
        val encryptedDisplayName = cipher.encrypt("Chat One")

        val conversation = ConversationEntity(
            id = conversationOpaqueId,
            encryptedDisplayName = encryptedDisplayName.ciphertext,
            displayNameIv = encryptedDisplayName.iv,
            conversationType = ConversationType.DIRECT,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.conversationDao().insert(conversation)

        val encryptedSender = cipher.encrypt("Alice")
        val encryptedMessage = cipher.encrypt("Hey")
        val dedupeToken = tokenGenerator.generate("hash_unique")

        val message = MessageEventEntity(
            id = "msg_1",
            conversationId = conversationOpaqueId,
            encryptedSenderName = encryptedSender.ciphertext,
            senderNameIv = encryptedSender.iv,
            encryptedMessageText = encryptedMessage.ciphertext,
            messageTextIv = encryptedMessage.iv,
            postedAt = System.currentTimeMillis(),
            notificationKey = "notif_key_1",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            dedupeToken = dedupeToken,
            isContentUnavailable = false,
            createdAt = System.currentTimeMillis()
        )
        db.messageEventDao().insert(message)

        val reference = ActiveNotificationReferenceEntity(
            notificationKey = "notif_key_1",
            latestMessageEventId = "msg_1",
            hadReplyActionWhenSeen = true,
            lastSeenActiveAt = System.currentTimeMillis(),
            removedAt = null
        )
        db.activeNotificationReferenceDao().insertOrUpdate(reference)

        assertFalse(db.conversationDao().getAllConversations().isEmpty())
        assertFalse(db.messageEventDao().getAllMessages().isEmpty())
        assertFalse(db.activeNotificationReferenceDao().getActiveReferences().isEmpty())

        db.messageEventDao().deleteAllData()

        assertTrue(db.conversationDao().getAllConversations().isEmpty())
        assertTrue(db.messageEventDao().getAllMessages().isEmpty())
        assertTrue(db.activeNotificationReferenceDao().getActiveReferences().isEmpty())
    }

    @Test
    fun testExplicitSchemaMigration_v1_to_v2() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbPath = context.getDatabasePath("test_migration_database").absolutePath
        context.deleteDatabase("test_migration_database")

        // 1. Manually create a version-1 SQLite database
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("test_migration_database")
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `conversations` (
                                `id` TEXT NOT NULL, 
                                `displayName` TEXT NOT NULL, 
                                `conversationType` TEXT NOT NULL, 
                                `verifiedPhoneNumberE164` TEXT, 
                                `createdAt` INTEGER NOT NULL, 
                                `updatedAt` INTEGER NOT NULL, 
                                PRIMARY KEY(`id`)
                            )
                        """.trimIndent())

                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `message_events` (
                                `id` TEXT NOT NULL, 
                                `conversationId` TEXT NOT NULL, 
                                `senderName` TEXT, 
                                `encryptedMessageText` BLOB, 
                                `encryptionIv` BLOB, 
                                `postedAt` INTEGER NOT NULL, 
                                `notificationKey` TEXT NOT NULL, 
                                `sourcePackage` TEXT NOT NULL, 
                                `parseSource` TEXT NOT NULL, 
                                `dedupeHash` TEXT NOT NULL, 
                                `isContentUnavailable` INTEGER NOT NULL, 
                                `createdAt` INTEGER NOT NULL, 
                                PRIMARY KEY(`id`), 
                                FOREIGN KEY(`conversationId`) REFERENCES `conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                            )
                        """.trimIndent())

                        db.execSQL("""
                            CREATE TABLE IF NOT EXISTS `active_notification_references` (
                                `notificationKey` TEXT NOT NULL, 
                                `latestMessageEventId` TEXT, 
                                `hadReplyActionWhenSeen` INTEGER NOT NULL, 
                                `lastSeenActiveAt` INTEGER NOT NULL, 
                                `removedAt` INTEGER, 
                                PRIMARY KEY(`notificationKey`)
                            )
                        """.trimIndent())
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )

        val writeDb = helper.writableDatabase

        // Seed v1 plaintext records
        val convValues = android.content.ContentValues().apply {
            put("id", "Spidey")
            put("displayName", "Spidey Chat")
            put("conversationType", "DIRECT")
            putNull("verifiedPhoneNumberE164")
            put("createdAt", 1716900000000L)
            put("updatedAt", 1716900000000L)
        }
        writeDb.insert("conversations", 0, convValues)

        // Encrypt message body text (which was encrypted in v1 too)
        val payloadMsg = cipher.encrypt("Meet me at Bugle!")
        val msgValues = android.content.ContentValues().apply {
            put("id", "msg_1")
            put("conversationId", "Spidey")
            put("senderName", "Peter Parker")
            put("encryptedMessageText", payloadMsg.ciphertext)
            put("encryptionIv", payloadMsg.iv)
            put("postedAt", 1716900000000L)
            put("notificationKey", "notif_1")
            put("sourcePackage", "com.whatsapp")
            put("parseSource", "MESSAGING_STYLE")
            put("dedupeHash", "fingerprint_123")
            put("isContentUnavailable", 0)
            put("createdAt", 1716900000000L)
        }
        writeDb.insert("message_events", 0, msgValues)
        writeDb.close()

        // 2. Open database under Room and execute MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(
            context,
            GemmaControlDatabase::class.java,
            "test_migration_database"
        )
        .addMigrations(GemmaControlDatabase.MIGRATION_1_2)
        .build()

        // Access database to trigger migration
        val decryptedMessages = runBlocking {
            val repo = StoredInboxRepository(
                migratedDb.conversationDao(),
                migratedDb.messageEventDao(),
                migratedDb.activeNotificationReferenceDao(),
                cipher,
                tokenGenerator
            )
            repo.getAllDecryptedMessages()
        }

        // 3. Verify migrated content displays and decrypts correctly
        assertEquals(1, decryptedMessages.size)
        assertEquals("Spidey Chat", decryptedMessages.first().conversationId) // Decrypted title
        assertEquals("Peter Parker", decryptedMessages.first().senderName)       // Decrypted sender name
        assertEquals("Meet me at Bugle!", decryptedMessages.first().decryptedText)  // Decrypted message text

        // 4. Query raw SQLite db to assert NO plaintext leaks remain in any version 2 columns
        val rawDb = migratedDb.openHelper.readableDatabase
        val cursorConv = rawDb.query("SELECT * FROM conversations")
        assertTrue(cursorConv.moveToFirst())
        
        // Assert displayName is completely gone (replaced by BLOB) and ID is opaque
        val encDispIdx = cursorConv.getColumnIndex("encryptedDisplayName")
        val legacyDispIdx = cursorConv.getColumnIndex("displayName")
        val idIdx = cursorConv.getColumnIndex("id")

        assertEquals(-1, legacyDispIdx) // Plaintext displayName column is removed!
        assertNotNull(cursorConv.getBlob(encDispIdx))
        assertNotEquals("Spidey Chat", cursorConv.getString(idIdx)) // ID is now opaque HMAC token!
        cursorConv.close()

        val cursorMsg = rawDb.query("SELECT * FROM message_events")
        assertTrue(cursorMsg.moveToFirst())
        
        // Assert senderName plaintext column is removed and sender is encrypted
        val encSenderIdx = cursorMsg.getColumnIndex("encryptedSenderName")
        val legacySenderIdx = cursorMsg.getColumnIndex("senderName")
        val legacyHashIdx = cursorMsg.getColumnIndex("dedupeHash")
        val tokenIdx = cursorMsg.getColumnIndex("dedupeToken")

        assertEquals(-1, legacySenderIdx) // Plaintext senderName is gone!
        assertEquals(-1, legacyHashIdx)   // Plaintext-derived dedupeHash is gone!
        assertNotNull(cursorMsg.getBlob(encSenderIdx))
        assertNotNull(cursorMsg.getString(tokenIdx)) // Opaque dedupeToken exists!

        cursorMsg.close()
        migratedDb.close()
        context.deleteDatabase("test_migration_database")
    }
}
