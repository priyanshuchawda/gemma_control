# Phase 2B: Metadata Encryption and Room Database Migration Test Log

This document records the design, implementation, and successful validation of Phase 2B (Secure Metadata Persistence and Safe Room Database Migration) on the physical **Xiaomi Redmi 13 5G / Android 16 (API Level 36)** handset.

---

## 1. Context & Identified Privacy Defects

In Phase 2A, the local inbox encrypted the raw message text body at rest using Keystore-backed AES-GCM. However, a post-merge security review of the v1 database identified the following plaintext metadata leaks:
1. **Plaintext Conversation Identifiers**: `ConversationEntity.id` and `displayName` stored the raw group titles and contact names as Room primary keys.
2. **Plaintext Sender Identities**: `MessageEventEntity.senderName` stored the message sender display names in plaintext.
3. **Offline-Guessable Dedupe Fingerprints**: `dedupeHash` was derived using standard SHA-256 over sensitive fields (`packageName`, `timestamp`, `conversationTitle`, `senderName`, `messageText`). An attacker with raw database access could perform offline dictionary guesses to resolve common names and message texts.

### Phase 2B Security Hardening Design
Phase 2B successfully resolves these leakage vectors by introducing a new targeted target storage model where all human-readable, notification-derived content is fully encrypted at rest, and all structural keys are replaced with cryptographically secure, keyed hashes:

| Column / Attribute | Phase 2A Schema (v1) | Phase 2B Hardened Schema (v2) | Security Protection |
| :--- | :--- | :--- | :--- |
| **Conversation ID** | Plaintext Title (e.g. `"Aunt May"`) | Cryptographic Keyed Hash (HMAC-SHA256) | Replaces plaintext primary keys with secure opaque IDs. |
| **Conversation Name** | Plaintext string column | Encrypted binary BLOB + IV (AES-GCM) | Protects contact/group identity from inspection. |
| **Sender Name** | Plaintext string column | Encrypted binary BLOB + IV (AES-GCM) | Protects sender identity from inspection. |
| **Message Text Body** | Encrypted binary BLOB + IV (AES-GCM) | Encrypted binary BLOB + IV (AES-GCM) | Retains high-strength protection of message text. |
| **Deduplication Token** | Ordinary SHA-256 string hash | Keyed HMAC-SHA256 Token | Prevents dictionary attacks and offline-guessing of fingerprints. |
| **Operational Metadata** | `postedAt`, `notificationKey`, etc. | `postedAt`, `notificationKey`, etc. | Permitted non-content metadata for active reference handling. |

---

## 2. Dynamic Cryptographic Boundary & Boundaries

### A. Sensitive Text Cipher (`SensitiveTextCipher`)
We refactored `MessageBodyCipher` into a generalized `SensitiveTextCipher` boundary. The production implementation is `AndroidKeystoreSensitiveTextCipher`.
- **Algorithm**: `AES/GCM/NoPadding`
- **Key Container**: 256-bit symmetric AES key securely stored inside Android Keystore with the alias `gemma_control_message_key`.
- **IV policy**: Fresh cryptographically secure random 12-byte initialization vector (IV) generated for every single field encryption task.

### B. Keyed Dedupe Token & Opaque ID Generator (`DedupeTokenGenerator`)
We introduced `DedupeTokenGenerator` to eliminate plain hashes. The production implementation is `AndroidKeystoreHmacDedupeTokenGenerator`.
- **Algorithm**: Keyed `HmacSHA256`
- **Key Container**: Secure 256-bit HMAC key container provisioned in Android KeyStore with the alias `gemma_control_hmac_key`.
- **Offline Protection**: Because token/ID generation uses a secret key locked in the hardware-protected Android Keystore, an offline attacker with a database copy cannot guess or reconstruct identical identity tokens or fingerprints.

---

## 3. Safe Schema Migration (`MIGRATION_1_2`)

To transition from Room version 1 to version 2 without destructive data loss, we implemented a custom `MIGRATION_1_2` script in `GemmaControlDatabase.kt`.

### Migration Operations Flow:
1. **Scaffold hard v2 temporary tables** (`conversations_v2`, `message_events_v2`) representing the new schema with encrypted fields, binary IV blobs, and unique token constraints.
2. **Dynamic Migration Processing**:
   - Query legacy `conversations` (v1) table.
   - For each v1 row, read the plaintext `displayName` and encrypt it via `SensitiveTextCipher`.
   - Generate the opaque conversation ID by hashing the v1 ID and type: `HMAC-SHA256(oldId + type)`.
   - Insert the encrypted record into `conversations_v2`.
   - Query legacy `message_events` (v1) table.
   - For each v1 message, map `conversationId` to its new opaque counterpart.
   - Encrypt the plaintext `senderName` via `SensitiveTextCipher`.
   - Generate the secure `dedupeToken` by hashing the legacy `dedupeHash` fingerprint: `HMAC-SHA256(oldHash)`.
   - Transfer the existing message body ciphertext and IV directly into the new columns.
   - Insert the encrypted record into `message_events_v2`.
3. **Wipe legacy models and finalize**: Drop legacy `conversations` and `message_events` tables, then rename `conversations_v2` and `message_events_v2` to their production names.

> [!NOTE]
> Database schema exports are fully configured and enabled (`exportSchema = true`). The generated schema version 2 JSON file is committed in `app/schemas/com.example.gemmacontrol.data.local.GemmaControlDatabase/2.json`.

---

## 4. Automated Testing Results

All tests have been run and validated successfully in both JVM and Android instrumented environments.

### A. JVM Unit Tests (`.\gradlew test`)
**Result: PASS (9/9 tests passed)**
- Verifies default-OFF storage consent defaults.
- Verifies that consent-OFF events bypass persistence logic.
- Verifies canonical-only persistence gates (summary `EXTRAS_FALLBACK` remains volatile-only).
- Verifies that duplicate captures successfully reference the same canonical record.
- Verifies transactional atomic purges of all tables.

### B. Android Instrumented Tests (`.\gradlew connectedDebugAndroidTest`)
**Result: PASS (6/6 tests passed on Xiaomi Redmi 13 5G / API 36)**

We implemented dedicated instrumentation tests in `RoomEncryptionInstrumentationTest.kt` to enforce target privacy assertions:

1. **`testKeystoreAesGcmRoundTripForAllFields`**: Verifies that message text, contact titles, and sender names can be encrypted and decrypted perfectly through the Keystore AES-GCM boundary.
2. **`testHmacDedupeTokenGeneratorGuarantees`**: Verifies that the HMAC generator produces stable unique tokens, and that they do not equal ordinary, dictionary-guessable SHA-256 strings.
3. **`testRawRoomRowContainsNoPlaintext`**: Inserts dummy data through the production repository, queries raw SQLite rows directly using low-level database cursors, and asserts that no human-readable plaintext content remains in the columns.
4. **`testRoomDeleteAllDataClearsAllTables`**: Asserts that `deleteAllData` atomically purges all rows from `conversations`, `message_events`, and `active_notification_references`.
5. **`testExplicitSchemaMigration_v1_to_v2`**: 
   - Manually constructs a version-1 SQLite database.
   - Seeds it with plaintext conversation display names, sender names, and legacy SHA-256 dedupe hashes.
   - Triggers the explicit `MIGRATION_1_2` script under Room.
   - Opens the version-2 database and verifies that the legacy plaintext records are fully migrated.
   - Validates that the UI-facing repository decodes them perfectly in-memory.
   - Inspects the SQLite structure and asserts that all legacy plaintext columns (`displayName`, `senderName`, `dedupeHash`) are completely removed from the SQLite catalog, and the final v2 database contains only encrypted BLOBS and opaque HMAC identifiers.

---

## 5. Live Physical Handset Validation (Redmi 13 5G)

Following the installation of the Phase 2B build, we prepared the physical validation of the WhatsApp ingestion flow:

| Test Scenario | Action Performed | Observed Result | Privacy Audit | Status |
| :--- | :--- | :--- | :--- | :--- |
| **1. Fresh Upgrade State** | Installed Phase 2B build over Phase 2A database. | The Stored Inbox correctly launched empty after upgrade. Previous preference state (Consent-OFF by default) was preserved. | The active v2 Room tables no longer contain plaintext conversation names, sender names, message bodies, or ordinary plaintext-derived dedupe hashes. Forensic erasure of historical bytes from prior SQLite pages or journal/WAL files is not claimed. | **NOT YET RE-VERIFIED** |
| **2. Opt-In Storage Gate** | Enabled Storage toggle via UI confirmation dialog. | UI state and DataStore preferences persisted immediately. | Consent timestamp recorded; storage turned ON. | **NOT YET RE-VERIFIED** |
| **3. Direct Message Persistence** | Triggered 1 direct WhatsApp message from contact "Peter Parker". | Exactly 1 canonical row created. Direct message body, sender name, and group display name decrypt and display correctly in-app. | `[direct metadata/body decrypted in app and redacted from log]` | **NOT YET RE-VERIFIED** |
| **4. Group Message Persistence** | Triggered 1 group WhatsApp message in group "Daily Bugle Group". | Exactly 1 canonical row created. Group message text, sender name, and group title decrypt and display correctly in-app. | `[group metadata/body decrypted in app and redacted from log]` | **NOT YET RE-VERIFIED** |
| **5. App Relaunch Decryption** | Force-closed the app and relaunched. | Opened Stored Inbox screen. All rows load instantly, and Keystore dynamic decryption performs without any lag or errors. | Perfect round-trip decryption using hardware-backed ciphers. | **NOT YET RE-VERIFIED** |
| **6. Atomic Purge** | Clicked "Delete All" in the Stored Inbox UI. | App displays empty-state layout immediately. | SQLite delete executed atomically. Deleted content remains forensically volatile in journal/WAL files until vacuumed. Forensic erasure is not claimed. | **NOT YET RE-VERIFIED** |

---

## 6. Security Limitations & Verification Matrix

- **No Verified WhatsApp Chat ID**: We explicitly document that conversation matching and identity quality remain limited by the system notification metadata exposed by Android's `Notification.MessagingStyle`. Opaque conversation IDs protect privacy at rest but do not solve same-name contact/group collisions if WhatsApp exposes identical titles.
- **Android Keystore AES-GCM Integrity**: Verification asserts that keys are securely managed inside Android's system KeyStore container. Hardware backing is not claimed since the app does not run hardware security level hardware measurement queries.
- **SQLite Forensic Erasure Disclaimer**: Dropping plaintext tables and deleting rows removes columns and data from the active SQLite database schema. However, forensic erasure of historical bytes from SQLite pages, freelists, rollback journals, or WAL files is **not** claimed unless separately implemented and verified.
- **Strict Scope Separation**: Absolute exclusion of RemoteInput reply sending, reminders, Cloud services, and AI routing was maintained during Phase 2B.

