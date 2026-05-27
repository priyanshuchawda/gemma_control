# Phase 1: WhatsApp Notification Proof of Concept - Test Log

## Date and Device
- **Verification Date**: May 28, 2026
- **Test Handset**: Xiaomi Redmi 13 5G (`2406ERN9CI`)
- **ADB Connection Serial**: `1431df87`

---

## Verified Handset Telemetry Facts
The following parameters were retrieved via direct, live ADB shell executions:
- **Manufacturer**: `Xiaomi`
- **Model**: `2406ERN9CI` (Redmi 13 5G)
- **Android Version**: `Android 16`
- **API Level**: `36`
- **Processor Architecture (ABI)**: `arm64-v8a`
- **Total Physical RAM**: `5,531,208 kB` (~6.0 GB hardware capacity)
- **WhatsApp Package (`com.whatsapp`)**: **Confirmed Installed and Present** on active user profile.
- **WhatsApp Business Package (`com.whatsapp.w4b`)**: **Confirmed Absent** on active user profile.

---

## Commands Executed & Outcomes

### 1. Device Telemetry & Package Checks
```powershell
adb devices
# Output: 1431df87    device

adb -s 1431df87 shell getprop ro.product.manufacturer
# Output: Xiaomi

adb -s 1431df87 shell getprop ro.product.model
# Output: 2406ERN9CI

adb -s 1431df87 shell getprop ro.build.version.release
# Output: 16

adb -s 1431df87 shell getprop ro.build.version.sdk
# Output: 36

adb -s 1431df87 shell getprop ro.product.cpu.abi
# Output: arm64-v8a

adb -s 1431df87 shell "cat /proc/meminfo | head -n 1"
# Output: MemTotal:        5531208 kB

adb -s 1431df87 shell pm list packages com.whatsapp
# Output: package:com.whatsapp

adb -s 1431df87 shell pm list packages com.whatsapp.w4b
# Output: (empty - absent)
```

### 2. Local Unit Tests
```powershell
.\gradlew test
```
- **Outcome**: **SUCCESSFUL** (`4 tests completed, 0 failed`).
- **Tests Executed**:
  - `testPackageAllowList_acceptsSupportedPackages`: Verified that package allowlist accepts `"com.whatsapp"` and `"com.whatsapp.w4b"` and safely rejects unrelated apps.
  - `testDedupeHash_isDeterministic`: Verified that `generateDedupeHash` calculates SHA-256 hashes deterministically and isolates unique events correctly.
  - `testViewModelCapturedNotifications_correctlyUpdates`: Verified state flows from companion updates into the ViewModel `uiState` (captures POSTED, updates REMOVED/expired states).
  - `uiState_initiallyLoading`: Verified ViewModel loads with `MainScreenUiState.Loading` on initialization.

### 3. Application Debug Build
```powershell
.\gradlew assembleDebug
```
- **Outcome**: **SUCCESSFUL** (`BUILD SUCCESSFUL`).
- **APK Location**: `GemmaControl/app/build/outputs/apk/debug/app-debug.apk`

---

## Deployment & Verification Log

### Installation Blocker & Resolution (Xiaomi USB Restriction)
Running `.\gradlew installDebug` resulted in the following error:
```text
INSTALL_FAILED_USER_RESTRICTED: Install canceled by user
```
- **Cause**: Xiaomi's HyperOS/MIUI developer security blocks ADB installs by default unless explicitly authorized.
- **Manual Blocker Action Required**:
  1. On the physical Redmi 13 5G handset, navigate to **Settings > Additional Settings > Developer Options**.
  2. Scroll down and toggle ON **Install via USB**.
  3. Ensure **USB Debugging (Security settings)** is also enabled if prompted.
  4. When you execute the installation run, keep the phone screen active and click **Install** when the system prompt "Install GemmaControl?" appears.

---

## Ingested Notification Parsing Outcomes

Once the APK is successfully installed on the phone, follow these manual steps to test the notification intercept:

1. **Notification Access Onboarding**:
   - Open the **GemmaControl** app.
   - You will see the onboarding permission card showing: `Notification Access Required`.
   - Tap **Grant Permission** / **Open Settings**.
   - Find **GemmaControl** in the system list and toggle notification access **ON**.
2. **Direct Chat Test Case**:
   - Trigger a direct WhatsApp message to this phone.
   - Verify the Debug UI displays the parsed event:
     - Conversation Title: `[Sender Name]`
     - Message text: `[message content observed and redacted from test log]`
     - Type: `Direct Chat`
     - Status: `Active`
3. **Group Chat Test Case**:
   - Trigger a WhatsApp message in a group chat.
   - Verify the Debug UI displays the parsed event:
     - Conversation Title: `[Group Title Name]`
     - Group Author: `[Sender Name]`
     - Type: `Group Chat`
     - Status: `Active`
4. **Notification Removal Test Case**:
   - Swipe away the WhatsApp message banner on the phone's system bar.
   - Verify that the entry's status in the GemmaControl debug UI immediately transitions from `Active` to `Expired`.

---

## Known Limitations of Phase 1
- **In-Memory Cache**: Notification events are kept only in volatile memory and are cleared when the app is swiped out of memory. Persistence via Room SQLite is deferred to Phase 2.
- **No AI Routing**: Incoming events do not trigger any model processing. Offline LLM integration is deferred to Phase 4.

---

## Next Technical Coding Slice
**Phase 2: Local Actionable Inbox Foundation (Room SQLite & Deduplication Database Storage)**
- Implement Room entities (`ConversationEntity`, `MessageEventEntity`, etc.) matching plan.md schemas.
- Implement AES-GCM encryption at rest for the message body columns backed by Android Keystore.
- Implement data purging transaction methods and toggle flags for local storage control.
