# Phase 2A: Encrypted Canonical Local Inbox Persistence — Test Log

## Test Session

- **Date**: 2026-05-28
- **Handset**: Xiaomi Redmi 13 5G (`2406ERN9CI`)
- **Android Version**: Android 16 / API 36
- **ADB Serial**: `1431df87`
- **Branch**: `feat/3-encrypted-canonical-inbox`

---

## Device Telemetry (ADB Verified)

| Property | Value |
| :--- | :--- |
| Manufacturer | Xiaomi |
| Model | Redmi 13 5G (`2406ERN9CI`) |
| Android Version | Android 16 |
| API Level | 36 |
| ABI | arm64-v8a |
| Physical RAM | ~6 GB (`5,531,208 kB`) |
| `com.whatsapp` | Installed |
| `com.whatsapp.w4b` | Absent |

---

## Automated Tests (Host Machine)

### Local JVM Unit Tests
- **Command**: `.\gradlew test`
- **Outcome**: **PASS** (Run locally)

| Test Name | Result | Focus |
| :--- | :--- | :--- |
| `testSettingsDefaultToDisabled` | PASS | Verifies that storage default is OFF (`false`) |
| `testCoordinatorSkipsPersistenceWhenStorageDisabled` | PASS | Coordinator writes nothing to database when storage consent is OFF |
| `testCoordinatorPersistsCanonicalMessagingStyleEvent` | PASS | Successful persistence of canonical `MESSAGING_STYLE` events |
| `testIngestionPolicy_fallbackOnlyEventDoesNotPersist` | PASS | Confirms EXTRAS_FALLBACK events remain volatile-only in debug feed and do not persist in Room |
| `testIngestionPolicy_messagingStylePlusFallbackWithSameKeyPersistsOneRow` | PASS | Prevents duplicate messaging style + summary rollup rows with same key |
| `testIngestionPolicy_messagingStylePlusFallbackWithDifferentKeysPersistsOneRow` | PASS | Prevents duplicates even if messaging style and summary rollup have different keys |
| `testDeduplication_duplicateInsertPointsReferenceToExistingMessageId` | PASS | Asserts duplicate inserts leave references pointing to the existing valid message ID |
| `testConsentBoundary_ignoresMessagePredatingStorageEnabledAt` | PASS | Asserts message timestamp predating explicit consent is ignored |
| `testRepository_deleteAllDataPurgesAllTables` | PASS | Purge utility clears conversation, message, and references tables atomically |

### Android Runtime Instrumented Tests
- **Command**: `.\gradlew connectedDebugAndroidTest`
- **Outcome**: **PASS** (Run on physical handset)

| Test Name | Result | Focus |
| :--- | :--- | :--- |
| `testKeystoreAesGcmRoundTrip` | PASS | Android Keystore-backed AES-GCM-NoPadding encrypt/decrypt round trip |
| `testRoomDaoInsertAndReadEncryptedText` | PASS | Stored Room database column contains only ciphertext; decrypts on UI fetch |
| `testRoomUniqueDedupeConstraintPreventsDuplicateRows` | PASS | Unique dedupe index triggers SQLite constraint ignore and decrypts first message successfully |
| `testRoomDeleteAllDataClearsAllTables` | PASS | Purge utility wipes all rows across conversation, message, and references atomically inside a single transaction |

---

## Physical Handset Validation Checklist

The application was deployed via `./gradlew installDebug` on the Xiaomi Redmi 13 5G handset.

| Step | Action | Expected Behavior | Observed Result | Status |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Initial App Launch | Settings toggle "Store future WhatsApp message previews locally" starts **OFF** by default. Persisted inbox shows empty state. | Pending manual user confirmation. | **NOT YET VERIFIED** |
| 2 | Receive Message (Consent OFF) | Trigger harmless WhatsApp message. Volatile debug feed captures event; persisted inbox remains empty. | Pending manual user confirmation. | **NOT YET VERIFIED** |
| 3 | Enable Storage Toggle | Toggle switch triggers a confirmation dialog explaining local encryption. | Pending manual user confirmation. | **NOT YET VERIFIED** |
| 4 | Direct Message (Consent ON) | Trigger harmless direct WhatsApp message. Volatile debug feed captures cards; stored inbox displays **exactly one** canonical message. | Pending manual user confirmation. | **NOT YET VERIFIED** |
| 5 | Group Message (Consent ON) | Trigger harmless group WhatsApp message. Volatile debug feed captures cards; stored inbox displays **exactly one** canonical group message. | Pending manual user confirmation. | **NOT YET VERIFIED** |
| 6 | Repeated Update Message | Trigger subsequent WhatsApp message in same conversation. Deduplicated updates do not create duplicate historical message rows. | Pending manual user confirmation. | **NOT YET VERIFIED** |
| 7 | App Relaunch | Terminate and relaunch application. Stored inbox items remain accessible and decrypt dynamically in memory. | Pending manual user confirmation. | **NOT YET VERIFIED** |
| 8 | Delete All stored messages | Tap "Delete all stored messages" button, confirm dialog. Persisted inbox becomes empty. Room data is purged. | Pending manual user confirmation. | **NOT YET VERIFIED** |

---

## Privacy & Safety Validation

1. **Plaintext In Database**: Plaintext message bodies are encrypted at rest using AES-GCM backed by Android Keystore.
2. **Zero Sensitive Content in Logcat**: Logcat outputs only metadata (`packageName`, key suffixes, parsed counts, parse source). Plaintext bodies, sender names, and phone numbers are structurally blocked from logging.
3. **Opt-in Storage Default**: Persistence is strictly disabled until the physical user physically confirms consent.
4. **Local Cryptography boundary**: Cryptographic keys are confined to the secure system `AndroidKeyStore` container. No remote connections or cloud APIs.
5. **Auto-Backup Disabled**: Globally disabled via `android:allowBackup="false"` in the Manifest to prevent local encrypted DB backups into Google Cloud without active key restoration guarantees.

---

## Known Limitations

- **Dual-Notification Observation**: The observed dual-notification pattern (one `MESSAGING_STYLE` + one `EXTRAS_FALLBACK` summary rollup per message) was verified for the Xiaomi Redmi 13 5G and the tested WhatsApp version. It must not be generalized as guaranteed behavior across every Android version or WhatsApp build.
- **In-Memory Decryption Scale**: Message bodies are encrypted at rest. Keyword text search is not supported in Room SQLite due to encrypted columns. Dynamic in-memory decryption is utilized for the bounded visible inbox UI.
- **Consent Stack Gating**: Pre-consent stacked notifications are prevented from being persisted by the `storageEnabledAt` filter.

---

## Deferred Features (Future Slices)

- **AI Command Routing & Local Inference**: Gemma 270M / LiteRT-LM tool invocation is deferred to Phase 4.
- **WhatsApp Direct Reply Execution**: RemoteInput reply trigger via notification binder is deferred to Phase 3.
- **Scheduled Reminders**: WorkManager reminder queueing is deferred to Phase 3.
