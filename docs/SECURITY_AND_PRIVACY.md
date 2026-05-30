# Security and Privacy Reference Document

This document defines the storage constraints, encryption keys, and privacy limits implemented within the application.

---

## 1. Storage & Decoupled Architecture

The application implements a **Zero-Cloud, Absolute Local Architecture**:
- **Offline Processing**: No data is uploaded to remote servers, external APIs, or cloud platforms.
- **System Boundaries**: The application declares `android.permission.INTERNET` only for explicit FunctionGemma `.litertlm` model binary downloads and `android.permission.POST_NOTIFICATIONS` only for user-confirmed local reminder alerts. WhatsApp notification content, prompts, tool calls, replies, and local database rows do not leave the handset during runtime operations.
- **WhatsApp Sandboxing**: The app cannot read WhatsApp's internal `/data/data/com.whatsapp/` database. It has zero headless control of WhatsApp, functioning entirely on captured system notifications.

---

## 2. In-App AES-GCM Key Encryption at Rest

To protect user message records and metadata from compromise or local unauthorized inspection, all sensitive text content is encrypted before writing to Room SQLite.

### A. Key Provisioning in Android Keystore
The application generates a 256-bit symmetric AES key inside Android's secure Keystore container using `AndroidKeystoreSensitiveTextCipher`:

```kotlin
class AndroidKeystoreSensitiveTextCipher : SensitiveTextCipher {

    private val provider = "AndroidKeyStore"
    private val keyAlias = "gemma_control_message_key"
    private val transformation = "AES/GCM/NoPadding"

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(provider)
            keyStore.load(null)
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, provider)
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(
                        keyAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                )
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Handle gracefully
        }
    }
}
```

### B. Payload Encryption Procedures
- **Encrypted Columns**:
  - `ConversationEntity.encryptedDisplayName` (contact/group display name)
  - `ConversationEntity.encryptedVerifiedPhoneNumberE164` (phone numbers if parsed)
  - `MessageEventEntity.encryptedSenderName` (message sender display name)
  - `MessageEventEntity.encryptedMessageText` (message body text)
- **Initialization Vector (IV)**: A cryptographically secure random 12-byte IV is generated for each field encryption transaction and stored alongside the ciphertext in the database as a separate binary BLOB.
- **Dynamic Decryption**: plain text is decrypted dynamically in-memory only when fetching rows at the repository boundary, preventing any plaintext storage at rest.

### C. Keyed Deduplication Fingerprints & Opaque Identifiers
To prevent dictionary attacks and offline guesses by attackers who copy the SQLite database, structural identifiers are hashed using a secret 256-bit HMAC-SHA256 key (`gemma_control_hmac_key`) locked inside the Android Keystore:
- **Opaque Conversation ID**: `conversationOpaqueId = HMAC-SHA256("Title-Type")`. The database never stores the raw plaintext title as its primary key.
- **Opaque Dedupe Token**: `dedupeToken = HMAC-SHA256("FingerprintMaterial")`. Plaintext-derived SHA-256 strings are not written to SQLite.

---

## 3. Database Search Strategy & Privacy Trade-off

Encrypting database columns at rest introduces a technical limitation: SQLite FTS5 or `LIKE` queries cannot index ciphertext natively.

### The Plaintext Metadata & Memory Trade-off
To maintain search efficiency without compromising security, we adopt an **in-memory decryption strategy**:
- **Non-Sensitive Metadata**: Only non-sensitive, operational structural metadata remains in plaintext:
  - `postedAt` (timestamps)
  - `sourcePackage` (e.g. `"com.whatsapp"`)
  - `parseSource` (e.g. `"MESSAGING_STYLE"`)
  - `notificationKey` (required for active references)
- **Search Operations**:
  - SQLite queries can filter records by timestamps or parse source in plaintext.
  - For full-text keyword searches, the repository loads the candidates (which are bounded by the 100-entry inbox history limit), decrypts them dynamically in-memory, and filters matches. This provides absolute privacy at rest with high execution speeds.

---

## 4. Cascading Purge Compliance & SQLite Forensic Erasure

When the user triggers a deletion via `Delete All` in the UI, the app guarantees cascading wiping across SQLite entities:
- Wiping a `MessageEventEntity` row automatically deletes associated records.
- Purging all data wipes `message_events`, `conversations`, and `active_notification_references` atomically inside a single transaction.

### SQLite Deletion & Forensic Erasure Limits
Dropping legacy tables and deleting SQLite rows removes active columns and data from the active database schema catalog. However, in standard SQLite databases, deleted content is normally not immediately overwritten in the underlying database pages or journal/WAL structures:
- **Mitigation**: During database migrations (such as `MIGRATION_1_2`), the system executes `PRAGMA secure_delete=ON` before dropping legacy plaintext tables. This tells SQLite to actively overwrite dropped tables and deleted content with zeroes in the database file.
- **Forensic Erasure Disclaimer**: While `secure_delete` mitigates direct byte leakage inside SQLite pages, forensic erasure of historical bytes from rollback journals, WAL (Write-Ahead Logging) files, or raw block storage sectors is **not** claimed unless separately implemented and verified.


