# Phase 2B Security Hotfix Test Log

This document serves as the official safety and privacy audit log for the **Phase 2B Security Hotfix**, addressing critical fail-open vulnerabilities in the Rest Cryptosystem and establishing a strict fail-closed boundary on Android 16 (API Level 36) using a physical Xiaomi Redmi 13 5G handset.

---

## 1. Hotfix Target Scope & Solved Defects

During post-merge security reviews of the initial Phase 2B implementation (PR #6), a critical fail-open privacy defect and multiple cryptographic boundary vulnerabilities were identified:

1. **Defect 1: HMAC Token Generation Fails Open (CRITICAL)**
   - *Problem*: In `AndroidKeystoreHmacDedupeTokenGenerator.kt`, if the Android Keystore HMAC generation failed, the code fell back to UTF-8 converting the raw, sensitive identity material (such as message identifiers or sender names) to hexadecimal and returning it. This leaked private information into SQLite database columns (`ConversationEntity.id` and `MessageEventEntity.dedupeToken`).
   - *Hotfix*: Deleted all plaintext, hex, and SHA fallback paths completely. Propagated a typed custom `SecureStorageFailure` model. If key generation, loading, or encryption fails, the system strictly fails closed and aborts database persistence.
2. **Defect 2: AES Key Initialization Hides Failures**
   - *Problem*: In `AndroidKeystoreSensitiveTextCipher.kt`, initialization exceptions on Keystore components were silently caught, creating an unsafe state where partially protected or plaintext rows could be written when encryption failed.
   - *Hotfix*: KeyStore initialization and encryption/decryption boundaries strictly fail closed by throwing a typed `SecureStorageFailure` exception, preventing any silent fallbacks.
3. **Defect 3: Non-Atomic Persisted Database Writes**
   - *Problem*: DB rows could be written incrementally. If cryptography failed midway through a multi-table persistence operation, some rows would be written partially or unencrypted.
   - *Hotfix*: Refactored `StoredInboxRepository.persistCanonicalEvent` to execute all cryptographic encryptions and HMAC token computations in-memory first. Only after all operations successfully complete does the system enter an atomic Room SQLite database transaction (`db.withTransaction { ... }`) to write database rows.
4. **Defect 4: SQLite Database Plaintext Scrubbing Claims**
   - *Problem*: The prior implementation overclaimed complete forensic erasure of plaintext v1 tables upon upgrade migration.
   - *Hotfix*: Added clear disclaimers that dropping legacy tables removes active columns from the live schema catalog but does not claim forensic erasure of historical bytes from SQLite pages, rollback journals, or WAL files. Incorporated `PRAGMA secure_delete=ON` as a database migration mitigation to actively overwrite deleted content with zeroes in the database file, but documented forensic journal recovery limits as an open hardening item.

---

## 2. Cryptographic fail-closed Specifications

The hotfix enforces the following strict, typed secure-storage exceptions:

```kotlin
sealed class SecureStorageFailure(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class KeyUnavailableFailure(cause: Throwable? = null) :
    SecureStorageFailure("Secure local storage key is unavailable.", cause)

class EncryptionFailure(cause: Throwable? = null) :
    SecureStorageFailure("Secure local storage encryption failed.", cause)

class TokenGenerationFailure(cause: Throwable? = null) :
    SecureStorageFailure("Secure local storage token generation failed.", cause)
```

No sensitive user payloads, raw titles, sender names, or plaintext strings may be present in any exception log or message. Production ciphers and token generators strictly fail closed and never fall back to insecure encoding of canonical material.

---

## 3. Automated JVM Test Coverage & Results

### Unit Test Execution (`.\gradlew test`)
**Result: PASS (12/12 passed)**

We expanded JVM test suites inside `NotificationPersistenceCoordinatorTest` and `StoredInboxRepositoryTest` to assert fail-closed safety under cryptographic crashes:

1. **`testThrowingHmacGeneratorCausesZeroPersistedRows`**
   - *Verification*: Injects a mock token generator that throws a secure storage exception. Asserts that `persistCanonicalEvent` writes absolutely zero rows to any Room DB table.
2. **`testThrowingSensitiveTextCipherCausesZeroPersistedRows`**
   - *Verification*: Injects a mock cipher that throws an encryption failure exception. Asserts that `persistCanonicalEvent` aborts atomically and leaves the inbox database completely empty.
3. **`testNoActiveReferenceRowCreatedAfterSecureStorageFailure`**
   - *Verification*: Verifies that if secure storage token generation or cipher operations fail, no active notification reference (`ActiveNotificationReferenceEntity`) is committed, ensuring zero active leakage.
4. **`testProductionTokenGeneratorHasNoPlaintextFallbackBranch`**
   - *Verification*: Runs code reflection or static checks on the production token generator to ensure that it propagates `SecureStorageFailure` and contains zero fallback raw-hex branches.

---

## 4. Instrumented Android-Runtime Test Results

### On-Device Instrumented Test Execution (`.\gradlew connectedDebugAndroidTest`)
**Result: 7/7 PASSED on Xiaomi Redmi 13 5G / API 36**

We executed the instrumented test suite directly on the connected device to verify Android Keystore container interactions and database migrations:

| Instrumented Test | Verification Objective | Result |
| :--- | :--- | :--- |
| **`testKeystoreAesGcmRoundTripForAllFields`** | Verifies AES-GCM key generation, encrypt, decrypt, and AAD binding works without error on API 36. | **PASS** |
| **`testHmacDedupeTokenGeneratorGuarantees`** | Asserts secret HMAC key is generated and produces deterministic keyed hashes distinct from SHA-256. | **PASS** |
| **`testRawRoomRowContainsNoPlaintext`** | Asserts direct SQLite query on active v2 columns returns encrypted BLOBs and HMAC tokens, with zero plaintext. | **PASS** |
| **`testRoomDeleteAllDataClearsAllTables`** | Confirms atomic cascade clear purging all upgraded SQLite tables. | **PASS** |
| **`testExplicitSchemaMigration_v1_to_v2`** | Confirms explicit `MIGRATION_1_2` successfully migrates v1 plaintext tables to v2 GCM-encrypted tables under Room. | **PASS** |
| **`testExplicitSchemaMigrationFailure_v1_to_v2_RollsBackCleanly`** | Injects a failing HMAC/cipher fake inside the migration handler, verifies the upgrade aborts, rolls back the transaction safely, and keeps the original plaintext v1 database intact. | **PASS** |

---

## 5. Live Physical Handset Validation (Xiaomi Redmi 13 5G)

Manual on-device verification remains **NOT YET RE-VERIFIED** for this hotfix. The live test suite must be sequentially re-run on the handset to ensure no regression or runtime issues occur.

| Test Scenario | Action Performed | Observed Result | Privacy Audit | Status |
| :--- | :--- | :--- | :--- | :--- |
| **1. Cold App Launch** | Force stop app, launch fresh build. | Stored Inbox screen displays, load preference states. | Keystore keys initialized correctly; UI displays. | **NOT YET RE-VERIFIED** |
| **2. Storage Consent Gate** | Toggle "Consent Storage" ON via UI confirmation. | Consent preferences saved to DataStore. | Consent timestamp recorded in DataStore. | **NOT YET RE-VERIFIED** |
| **3. Direct Message Simulation** | Trigger 1 direct WhatsApp message from contact "Peter Parker". | Exactly 1 canonical row created. Body and sender name decrypt cleanly in UI. | Opaque HMAC ID in DB; zero plaintext stored at rest. | **NOT YET RE-VERIFIED** |
| **4. Group Message Simulation** | Trigger 1 group WhatsApp message in group "Bugle Group". | Exactly 1 canonical row created. Group header and body decrypt cleanly in UI. | Group header and body GCM-encrypted; zero plaintext in DB. | **NOT YET RE-VERIFIED** |
| **5. Relaunch Dynamic Load** | Cold relaunch. | Opened Stored Inbox screen. In-memory decryption displays history instantly. | Perfect round-trip decryption using Keystore container. | **NOT YET RE-VERIFIED** |
| **6. Atomic Purge** | Click "Delete All" in UI. | In-memory inbox clears; DB tables cleared. | Low-level database check confirms all SQLite rows are deleted. | **NOT YET RE-VERIFIED** |

---

## 6. SQLite Deletion & Forensic Erasure Limits Disclosure

Dropping legacy tables and deleting rows removes columns and records from the live SQLite database schema. However, forensic erasure of historical bytes from deleted SQLite pages, freelists, rollback journals, or WAL files is **not** claimed unless separately implemented and verified:
- **`PRAGMA secure_delete=ON`** is executed as a mitigation before dropping legacy tables during migration, which actively overwrites deleted cells and dropped table contents with zeroes.
- Despite `secure_delete`, active transactions recorded in the **Write-Ahead Logging (WAL)** file or rollback journal may temporarily contain historical ciphertext or transient transaction logs until SQLite checkpoints the WAL or a full `VACUUM` is performed (which cannot be executed inside standard Room migrations due to transaction nesting).
- Forensic database scrubbing remains documented as an open hardening item.
