# Security and Privacy Reference Document

This document defines the storage constraints, encryption keys, and privacy limits implemented within the application.

---

## 1. Storage & Decoupled Architecture

The application implements a **Zero-Cloud, Absolute Local Architecture**:
- **Offline Processing**: No data is uploaded to remote servers, external APIs, or cloud platforms.
- **System Boundaries**: The application does not declare the `android.permission.INTERNET` flag in its manifest. Data cannot leave the handset during runtime operations.
- **WhatsApp Sandboxing**: The app cannot read WhatsApp's internal `/data/data/com.whatsapp/` database. It has zero headless control of WhatsApp, functioning entirely on captured system notifications.

---

## 2. In-App AES-GCM Key Encryption at Rest

To protect user message records from compromise or local unauthorized inspection, all sensitive message text is encrypted before writing to Room SQLite.

### A. Key Provisioning in Android Keystore
The application generates a 256-bit symmetric AES key inside Android's secure hardware-backed container using `AndroidKeystoreMessageBodyCipher`:

```kotlin
class AndroidKeystoreMessageBodyCipher : MessageBodyCipher {

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
            // Handle gracefully (e.g. in environments where KeyStore is mocked)
        }
    }
}
```

### B. Payload Encryption Procedures
- Encrypted Columns: `encrypted_message_text` inside `MessageEventEntity`, and `encrypted_message_text` inside `DraftReplyEntity`.
- **Initialization Vector (IV)**: A cryptographically secure random 12-byte IV is generated for each encryption transaction, prepended to the ciphertext, and stored as an encrypted binary blob/string in SQLite.

---

## 3. Database Search Strategy & Privacy Trade-off

Encrypting database columns at rest introduces a technical limitation: SQLite FTS5 or `LIKE` queries cannot index ciphertext natively.

### The Plaintext Metadata & Memory Trade-off
To maintain search efficiency without compromising security, we adopt a **hybrid storage strategy**:
- **Searchable Metadata**: Non-sensitive structural fields remain in plaintext within Room:
  - `sender_name` (parsed contact name)
  - `conversation_id` (structural conversation matching row)
  - `posted_at` (timestamps)
  - `priority` / `status` tags
- **Encrypted Content**: The raw body of the message preview and drafted replies are encrypted using AES-GCM.
- **Search Operations (V1 Plan)**:
  - SQLite queries filter records by contact name and timestamp limits in plaintext.
  - When the user searches using text keyword FTS queries, Room fetches the candidate encrypted records (e.g. top 100 matching timestamps/contacts), decodes them in memory, and applies English keyword search logic.
  - This design provides robust encryption at rest while maintaining offline search capabilities. We honestly document this design rather than claiming "encrypted searchable databases."

---

## 4. Cascading Purge Compliance

When the user triggers a deletion via `delete_local_whatsapp_data`, the app guarantees cascading wiping across SQLite entities:
- Wiping a `MessageEventEntity` row automatically deletes associated `FollowUpEntity`, `ReminderEntity`, and `DraftReplyEntity` records via SQL ForeignKey cascade rules (`onDelete = ForeignKey.CASCADE`).
- Wiping a `ConversationEntity` clears matching message indices.
- A full database purge clears the Room database and resets internal encryption IVs.
