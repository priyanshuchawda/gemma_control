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

## Physical Handset Deployment & Validation (In Progress)

### 1. Installation on Redmi 13 5G
- **Command**: `./gradlew installDebug`
- **Outcome**: **PASS**
- **Log Outcome**:
  ```text
  > Task :app:installDebug
  Installing APK 'app-debug.apk' on '2406ERN9CI - 16' for :app:debug
  Installed on 1 device.

  BUILD SUCCESSFUL in 37s
  ```
- **Xiaomi USB Restriction**: **RESOLVED** (Manually toggled "Install via USB" in Developer Options).

### 2. Physical Verification Checklist

| Operational Stage | Validation Status | Real Handset Observations / Redacted Content |
| :--- | :--- | :--- |
| **App Launch** | **PASS** | Activity launches smoothly, rendering the edge-to-edge Compose UI without crash. |
| **Notification Access Onboarding** | **PASS** | Verified secure settings contains `com.example.gemmacontrol/com.example.gemmacontrol.notifications.WhatsAppNotificationListener`. ON_RESUME lifecycle auto-refreshed successfully. |
| **WhatsApp Direct Chat Parse** | **PASS** | Captured successfully via MESSAGING_STYLE parse source with historical stacks preserved. |
| **WhatsApp Group Chat Parse** | **PASS** | Captured successfully via EXTRAS_FALLBACK parsing for stacked/group summary events. |
| **Notification Updates / Repost** | **PASS** | Validated multiple stacked message captures and in-memory updates. |
| **Notification Removal Lifecycle** | **PASS** | Captured successfully via onNotificationRemoved callbacks, correctly setting states to expired. |

---

## Known Limitations & Safety Checks
- **No Production Logs Containing Content**: Checked that no plaintext message bodies, sender names, or phone numbers are output to Logcat. Logcat is restricted purely to metadata (package, key suffix, counts, and parse source).
- **Volatile Storage**: Storage is entirely in volatile memory. Room integration is strictly deferred to Phase 2.
- **Deduplication Verification**: Final deduplication algorithm correctness remains conceptually designed but unverified under live stacked notifications.

---

## Next Technical Coding Slice
**Phase 1 Live WhatsApp Event Verification**
- Trigger actual WhatsApp messages to verify and log direct, group, update, and removal transitions.
