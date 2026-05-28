package com.example.gemmacontrol.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gemmacontrol.data.crypto.AndroidKeystoreMessageBodyCipher
import com.example.gemmacontrol.data.crypto.EncryptedPayload
import com.example.gemmacontrol.data.local.GemmaControlDatabase
import com.example.gemmacontrol.data.local.entity.ActiveNotificationReferenceEntity
import com.example.gemmacontrol.data.local.entity.ConversationEntity
import com.example.gemmacontrol.data.local.entity.MessageEventEntity
import com.example.gemmacontrol.notifications.ConversationType
import com.example.gemmacontrol.notifications.NotificationParseSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomEncryptionInstrumentationTest {

    private lateinit var db: GemmaControlDatabase
    private lateinit var cipher: AndroidKeystoreMessageBodyCipher

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GemmaControlDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cipher = AndroidKeystoreMessageBodyCipher()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testKeystoreAesGcmRoundTrip() {
        val plaintext = "Secure on-device message body content."
        
        val payload = cipher.encrypt(plaintext)
        assertNotNull(payload.ciphertext)
        assertNotNull(payload.iv)
        
        val ciphertextString = String(payload.ciphertext)
        assertNotEquals(plaintext, ciphertextString)

        val decrypted = cipher.decrypt(payload)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun testRoomDaoInsertAndReadEncryptedText() = runBlocking {
        val conversation = ConversationEntity(
            id = "Spidey",
            displayName = "Spidey Chat",
            conversationType = ConversationType.DIRECT,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.conversationDao().insert(conversation)

        val plaintext = "Meet me at the clock tower!"
        val payload = cipher.encrypt(plaintext)

        val message = MessageEventEntity(
            id = "msg_123",
            conversationId = "Spidey",
            senderName = "Peter Parker",
            encryptedMessageText = payload.ciphertext,
            encryptionIv = payload.iv,
            postedAt = System.currentTimeMillis(),
            notificationKey = "notif_key_1",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            dedupeHash = "hash_dedupe_123",
            isContentUnavailable = false,
            createdAt = System.currentTimeMillis()
        )

        db.messageEventDao().insert(message)

        val retrieved = db.messageEventDao().getMessagesForConversation("Spidey").firstOrNull()
        assertNotNull(retrieved)
        
        assertNotEquals(plaintext, String(retrieved!!.encryptedMessageText!!))

        val decryptedText = cipher.decrypt(EncryptedPayload(retrieved.encryptedMessageText!!, retrieved.encryptionIv!!))
        assertEquals(plaintext, decryptedText)
    }

    @Test
    fun testRoomUniqueDedupeConstraintPreventsDuplicateRows() = runBlocking {
        val conversation = ConversationEntity(
            id = "Spidey",
            displayName = "Spidey Chat",
            conversationType = ConversationType.DIRECT,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.conversationDao().insert(conversation)

        val msg1 = MessageEventEntity(
            id = "msg_1",
            conversationId = "Spidey",
            senderName = "Peter Parker",
            encryptedMessageText = cipher.encrypt("Msg 1").ciphertext,
            encryptionIv = cipher.encrypt("Msg 1").iv,
            postedAt = System.currentTimeMillis(),
            notificationKey = "notif_key_1",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            dedupeHash = "same_hash",
            isContentUnavailable = false,
            createdAt = System.currentTimeMillis()
        )

        val msg2 = MessageEventEntity(
            id = "msg_2",
            conversationId = "Spidey",
            senderName = "Peter Parker",
            encryptedMessageText = cipher.encrypt("Msg 1 again").ciphertext,
            encryptionIv = cipher.encrypt("Msg 1 again").iv,
            postedAt = System.currentTimeMillis() + 1000,
            notificationKey = "notif_key_1",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            dedupeHash = "same_hash",
            isContentUnavailable = false,
            createdAt = System.currentTimeMillis()
        )

        val row1 = db.messageEventDao().insert(msg1)
        val row2 = db.messageEventDao().insert(msg2)

        assertNotEquals(-1L, row1)
        assertEquals(-1L, row2)

        val allMessages = db.messageEventDao().getMessagesForConversation("Spidey")
        assertEquals(1, allMessages.size)
    }

    @Test
    fun testRoomDeleteAllDataClearsAllTables() = runBlocking {
        val conversation = ConversationEntity(
            id = "Chat1",
            displayName = "Chat One",
            conversationType = ConversationType.DIRECT,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.conversationDao().insert(conversation)

        val message = MessageEventEntity(
            id = "msg_1",
            conversationId = "Chat1",
            senderName = "Alice",
            encryptedMessageText = byteArrayOf(1, 2),
            encryptionIv = byteArrayOf(3, 4),
            postedAt = System.currentTimeMillis(),
            notificationKey = "notif_key_1",
            sourcePackage = "com.whatsapp",
            parseSource = NotificationParseSource.MESSAGING_STYLE,
            dedupeHash = "hash_unique",
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

        db.messageEventDao().deleteAll()
        db.conversationDao().deleteAll()
        db.activeNotificationReferenceDao().deleteAll()

        assertTrue(db.conversationDao().getAllConversations().isEmpty())
        assertTrue(db.messageEventDao().getAllMessages().isEmpty())
        assertTrue(db.activeNotificationReferenceDao().getActiveReferences().isEmpty())
    }
}
