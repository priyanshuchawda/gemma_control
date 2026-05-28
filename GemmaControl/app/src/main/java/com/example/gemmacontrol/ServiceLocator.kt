package com.example.gemmacontrol

import android.content.Context
import com.example.gemmacontrol.data.crypto.AndroidKeystoreSensitiveTextCipher
import com.example.gemmacontrol.data.crypto.AndroidKeystoreHmacDedupeTokenGenerator
import com.example.gemmacontrol.data.crypto.SensitiveTextCipher
import com.example.gemmacontrol.data.crypto.DedupeTokenGenerator
import com.example.gemmacontrol.data.local.GemmaControlDatabase
import com.example.gemmacontrol.data.preferences.DataStoreCapturePreferencesRepository
import com.example.gemmacontrol.data.repository.NotificationPersistenceCoordinator
import com.example.gemmacontrol.data.repository.StoredInboxRepository

object ServiceLocator {
    @Volatile
    private var database: GemmaControlDatabase? = null
    @Volatile
    private var preferencesRepository: DataStoreCapturePreferencesRepository? = null
    @Volatile
    private var storedInboxRepository: StoredInboxRepository? = null
    @Volatile
    private var persistenceCoordinator: NotificationPersistenceCoordinator? = null
    @Volatile
    private var sensitiveTextCipher: SensitiveTextCipher? = null
    @Volatile
    private var dedupeTokenGenerator: DedupeTokenGenerator? = null

    private fun getDatabase(context: Context): GemmaControlDatabase {
        return database ?: synchronized(this) {
            database ?: GemmaControlDatabase.getDatabase(context).also { database = it }
        }
    }

    fun getPreferencesRepository(context: Context): DataStoreCapturePreferencesRepository {
        return preferencesRepository ?: synchronized(this) {
            preferencesRepository ?: DataStoreCapturePreferencesRepository(context.applicationContext).also { preferencesRepository = it }
        }
    }

    private fun getSensitiveTextCipher(): SensitiveTextCipher {
        return sensitiveTextCipher ?: synchronized(this) {
            sensitiveTextCipher ?: AndroidKeystoreSensitiveTextCipher().also { sensitiveTextCipher = it }
        }
    }

    private fun getDedupeTokenGenerator(): DedupeTokenGenerator {
        return dedupeTokenGenerator ?: synchronized(this) {
            dedupeTokenGenerator ?: AndroidKeystoreHmacDedupeTokenGenerator().also { dedupeTokenGenerator = it }
        }
    }

    fun getStoredInboxRepository(context: Context): StoredInboxRepository {
        return storedInboxRepository ?: synchronized(this) {
            val db = getDatabase(context)
            storedInboxRepository ?: StoredInboxRepository(
                db.conversationDao(),
                db.messageEventDao(),
                db.activeNotificationReferenceDao(),
                getSensitiveTextCipher(),
                getDedupeTokenGenerator(),
                db
            ).also { storedInboxRepository = it }
        }
    }


    fun getPersistenceCoordinator(context: Context): NotificationPersistenceCoordinator {
        return persistenceCoordinator ?: synchronized(this) {
            val db = getDatabase(context)
            persistenceCoordinator ?: NotificationPersistenceCoordinator(
                getStoredInboxRepository(context),
                getPreferencesRepository(context),
                db.activeNotificationReferenceDao()
            ).also { persistenceCoordinator = it }
        }
    }
}
