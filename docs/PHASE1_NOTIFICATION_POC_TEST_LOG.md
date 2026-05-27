# Phase 1: WhatsApp Notification Proof of Concept - Test Log

## Date and Device
- **Verification Date**: May 28, 2026
- **Test Handset**: Xiaomi Redmi 13 5G (`2406ERN9CI`)
- **ADB Connection Serial**: `1431df87`

---

## Verified Handset Telemetry Facts
The following parameters were successfully retrieved and verified over the active ADB bridge:
- **Manufacturer**: `Xiaomi`
- **Model**: `2406ERN9CI` (Redmi 13 5G)
- **Android Version**: `Android 16`
- **API Level**: `36`
- **Processor Architecture (ABI)**: `arm64-v8a`
- **Total Physical RAM**: `5,531,208 kB` (~6.0 GB hardware capacity)
- **WhatsApp Package (`com.whatsapp`)**: Confirmed Installed and Present on the handset.
- **WhatsApp Business Package (`com.whatsapp.w4b`)**: Confirmed Absent.

---

## Verified by Build & Automated Unit Tests
The following outcomes have been fully validated on the host machine:

### 1. Local Unit Tests
- **Command**: `.\gradlew test`
- **Outcome**: **PASS** (`5 tests completed, 0 failed`)
- **Verified Tests**:
  - `testPackageAllowList_acceptsSupportedPackages`: Package allowlist accepts `"com.whatsapp"` and `"com.whatsapp.w4b"`, rejecting other apps.
  - `testDedupeCandidate_isDeterministic`: Generates deterministic SHA-256 dedupe hashes that differ when notification identity values (text, key, timestamp) vary.
  - `testInMemoryStateTransitions`: Validates state flow transitions from `POSTED` (first time), to `UPDATED` (subsequent message with matching active key), to `REMOVED` (notification swiped/cleared), updating active indicators.
  - `testEventHistoryCappedTo100`: Asserts in-memory history bounds remain capped at the latest 100 entries.
  - `uiState_initiallyLoading`: ViewModel initial state flows correctly.

### 2. Application Debug Build
- **Command**: `.\gradlew assembleDebug`
- **Outcome**: **PASS** (`BUILD SUCCESSFUL` with up-to-date configuration caches)
- **Built APK Location**: `GemmaControl/app/build/outputs/apk/debug/app-debug.apk`

---

## Physical Handset Deployment & Validation (Currently Blocked)

### 1. Installation on Redmi 13 5G
- **Command**: `.\gradlew installDebug`
- **Outcome**: **FAILED / BLOCKED**
- **Error log**:
  ```text
  Execution failed for task ':app:installDebug'.
  > java.util.concurrent.ExecutionException: com.android.builder.testing.api.DeviceException: com.android.ddmlib.InstallException: INSTALL_FAILED_USER_RESTRICTED: Install canceled by user
  ```
- **Reason**: Xiaomi's HyperOS / MIUI Developer options restrict ADB installations by default unless the user has manually authorized the setting.
- **Required Action to Unblock**:
  On the physical Redmi 13 5G, go to **Settings > Additional Settings > Developer Options**, enable **Install via USB**, and accept the screen prompts when the app installs.

### 2. Physical Verification Checklist

| Operational Stage | Validation Status | Real Handset Observations / Redacted Content |
| :--- | :--- | :--- |
| **App Launch** | **NOT YET VERIFIED** | Blocked by physical installation restriction. |
| **Notification Access Onboarding** | **NOT YET VERIFIED** | Blocked by physical installation restriction. |
| **WhatsApp Direct Chat Parse** | **NOT YET VERIFIED** | No live capture observed on handset yet. |
| **WhatsApp Group Chat Parse** | **NOT YET VERIFIED** | No live capture observed on handset yet. |
| **Notification Updates / Repost** | **NOT YET VERIFIED** | No live repost events observed on handset yet. |
| **Notification Removal Lifecycle** | **NOT YET VERIFIED** | No live removal events observed on handset yet. |

---

## Known Limitations & Safety Checks
- **No Production Logs Containing Content**: Checked that no plaintext message bodies, sender names, or phone numbers are output to Logcat. Logcat is restricted purely to metadata (package, key suffix, counts, and parse source).
- **Volatile Storage**: Storage is entirely in volatile memory. Room integration is strictly deferred to Phase 2.
- **Deduplication Verification**: Final deduplication algorithm correctness remains conceptually designed but unverified under live stacked notifications.

---

## Next Technical Coding Slice
**Phase 1 Physical Validation (Unblocking Handset Install & Intercepting Messages)**
- Toggle **Install via USB** on the handset.
- Install and launch **GemmaControl** successfully.
- Trigger actual WhatsApp messages to verify and log direct, group, and update transitions.
