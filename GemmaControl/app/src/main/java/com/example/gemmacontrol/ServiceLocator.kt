package com.example.gemmacontrol

import android.content.Context
import com.example.gemmacontrol.data.crypto.AndroidKeystoreMessageBodyCipher
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
    private var messageBodyCipher: AndroidKeystoreMessageBodyCipher? = null

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

    private fun getMessageBodyCipher(): AndroidKeystoreMessageBodyCipher {
        return messageBodyCipher ?: synchronized(this) {
            messageBodyCipher ?: AndroidKeystoreMessageBodyCipher().also { messageBodyCipher = it }
        }
    }

    fun getStoredInboxRepository(context: Context): StoredInboxRepository {
        return storedInboxRepository ?: synchronized(this) {
            val db = getDatabase(context)
            storedInboxRepository ?: StoredInboxRepository(
                db.conversationDao(),
                db.messageEventDao(),
                db.activeNotificationReferenceDao(),
                getMessageBodyCipher()
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
