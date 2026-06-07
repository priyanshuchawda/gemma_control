# Phone Testing Toolkit

This document captures the current laptop and Android phone test setup for GemmaControl.

Snapshot date: 2026-06-07  
Workspace: `C:\Users\Admin\Desktop\gemma_control`  
Android project: `C:\Users\Admin\Desktop\gemma_control\GemmaControl`  
Test device serial: `1431df87`  
Test device: Redmi/Xiaomi `2406ERN9CI`, HyperOS `OS3.0`, Android `16`, API `36`

## Readiness Summary

The workstation and phone are ready for full GemmaControl physical-device testing.

Confirmed working:

| Area | Status |
| :--- | :--- |
| ADB connection | Connected as `1431df87`, state `device`. |
| ADB input | `adb shell input tap` and key events work. |
| scrcpy | Installed and launched successfully. |
| Appium | Appium server starts and returns `/status` ready. |
| Appium UiAutomator2 | Driver installed and a session successfully opened against GemmaControl. |
| Maestro | Installed as `maestro 2.6.0`. |
| Gradle wrapper | Project wrapper works with Gradle `9.1.0`. |
| Debug APK install | `adb install -r -d` works on the connected phone. |
| GemmaControl permissions | Microphone and notifications granted for active test profile. |
| Notification listener | GemmaControl listener enabled in Android notification listener settings. |
| FunctionGemma model | App reports FunctionGemma ready on the phone. |

Still manual or optional:

| Area | Status |
| :--- | :--- |
| Xiaomi Autostart | Must be confirmed manually in app settings. |
| Xiaomi battery mode | Must be confirmed manually as no restrictions for GemmaControl. |
| Disable permission monitoring | Must be confirmed manually if visible in Developer options. |
| Appium Inspector | Optional. Appium automation works without it. |
| Charles/Proxyman | Optional. Not needed for current offline/on-device WhatsApp workflows. |

## Laptop Tools

| Tool | Current state | Notes |
| :--- | :--- | :--- |
| Android Studio | Installed at `C:\Program Files\Android\Android Studio\bin\studio64.exe` | Use for IDE, profiler, Network Inspector, Layout Inspector, and device file explorer. |
| Android SDK root | `C:\Android` | `ANDROID_HOME=C:\Android`. |
| Platform-Tools | Installed | `adb` and `fastboot` resolve from `C:\Android\platform-tools`. |
| ADB | `37.0.0-14910828` | Primary install/log/device-state tool. |
| SDK Command-line Tools | Installed | `sdkmanager` and `avdmanager` resolve from `C:\Android\cmdline-tools\latest\bin`. |
| Android Emulator | Installed | `emulator.exe` resolves from `C:\Android\emulator`. |
| Android SDK Platforms | `android-34`, `android-35`, `android-36`, `android-36.1` | Physical target is API 36. |
| Android Build-Tools | `34.0.0`, `35.0.0`, `36.0.0`, `36.1.0`, `37.0.0` | Multiple versions are available. |
| JDK | Temurin JDK `17.0.15` | Required for the Android build. |
| Gradle | Project wrapper `9.1.0` | System `gradle` is not required. Use `.\gradlew.bat`. |
| Android Gradle Plugin | `9.0.1` | Defined in `GemmaControl/gradle/libs.versions.toml`. |
| Node.js | `v24.11.0`, LTS `Krypton` | Useful for website tooling and Appium. |
| npm | `11.14.1` | Appium was installed through npm. |
| Appium | `3.5.0` | Server validated with `/status`. |
| UiAutomator2 driver | `uiautomator2@7.6.0` | Installed with Appium. |
| scrcpy | `4.0` | Phone mirror/control. |
| Maestro | `2.6.0` | YAML mobile flow runner. |

User PATH entries added:

```text
C:\Android\cmdline-tools\latest\bin
C:\Android\emulator
C:\Android\platform-tools
```

New terminals should resolve:

```powershell
adb --version
sdkmanager --version
avdmanager --help
emulator -version
scrcpy --version
appium --version
maestro --version
```

## Android Phone Test Settings

Confirmed through ADB:

| Setting | Value | Command/check |
| :--- | :--- | :--- |
| Developer options | Enabled | `settings get global development_settings_enabled` -> `1` |
| USB debugging | Enabled | `settings get global adb_enabled` -> `1` |
| Install from unknown sources | Enabled | `settings get secure install_non_market_apps` -> `1` |
| Stay awake while charging | Enabled | `settings get global stay_on_while_plugged_in` -> `7` |
| Window animation scale | Disabled | `settings get global window_animation_scale` -> `0` |
| Transition animation scale | Disabled | `settings get global transition_animation_scale` -> `0` |
| Animator duration scale | Disabled | `settings get global animator_duration_scale` -> `0` |
| Show touches | Enabled | `settings get system show_touches` -> `1` |
| GemmaControl notification listener | Enabled | `enabled_notification_listeners` includes GemmaControl. |
| GemmaControl microphone permission | Granted | `appops get com.example.gemmacontrol` shows `RECORD_AUDIO: allow`. |
| GemmaControl notification permission | Granted | `appops get com.example.gemmacontrol` shows `POST_NOTIFICATION: allow`. |
| GemmaControl background appops | Allowed | `RUN_IN_BACKGROUND` and `RUN_ANY_IN_BACKGROUND` are `allow`. |
| Standard doze whitelist | Added | `dumpsys deviceidle whitelist` includes `com.example.gemmacontrol`. |

Commands used to enable safe test settings:

```powershell
adb shell settings put global stay_on_while_plugged_in 7
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell settings put system show_touches 1
adb shell pm grant com.example.gemmacontrol android.permission.RECORD_AUDIO
adb shell pm grant com.example.gemmacontrol android.permission.POST_NOTIFICATIONS
adb shell dumpsys deviceidle whitelist +com.example.gemmacontrol
adb shell cmd appops set com.example.gemmacontrol RUN_IN_BACKGROUND allow
adb shell cmd appops set com.example.gemmacontrol RUN_ANY_IN_BACKGROUND allow
```

## Manual Xiaomi Checklist

Some HyperOS settings are Xiaomi-private and should be confirmed by hand.

Developer options:

1. Enable `USB debugging`.
2. Enable `Install via USB`.
3. Enable `USB debugging (Security settings)`.
4. Disable `Permission monitoring`, if visible.
5. Keep animation scales at `0x`.
6. Keep `Stay awake` enabled while charging.

GemmaControl app settings:

1. Open `Settings > Apps > Manage apps > GemmaControl`.
2. Set battery saver to `No restrictions`.
3. Enable `Autostart`.
4. Allow microphone.
5. Allow notifications.

Notification access:

1. Open `Settings > Notifications & status bar > Notification access`.
2. Ensure GemmaControl is enabled.

WhatsApp:

1. Keep WhatsApp notifications enabled.
2. Keep notification previews enabled for useful text capture.
3. Use a real test chat/account for live notification validation.

## Standard Test Commands

Build and install:

```powershell
cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
adb install -r -d .\app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.example.gemmacontrol/.MainActivity
```

Mirror/control the phone:

```powershell
scrcpy --serial 1431df87 --stay-awake --turn-screen-on
```

Read current phone/app state:

```powershell
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell dumpsys battery
adb shell dumpsys thermalservice
adb shell dumpsys package com.example.gemmacontrol
adb shell appops get com.example.gemmacontrol
adb shell settings get secure enabled_notification_listeners
adb shell dumpsys notification
```

Capture UI state without screenshots:

```powershell
adb shell uiautomator dump /sdcard/gemma_ui.xml
adb pull /sdcard/gemma_ui.xml .device-validation\gemma_ui.xml
```

Capture app logs:

```powershell
adb logcat -c
adb logcat -v time GemmaControl:D VoiceAssistantVM:D WhatsAppNotificationListener:D FunctionGemma:D LiteRt:D AndroidRuntime:E *:S
```

Read the debug-only spoken output transcript from the app-private cache:

```powershell
.\scripts\android\read-debug-spoken-output.ps1
```

This is local test evidence only. It may contain private WhatsApp text because it mirrors what TTS just spoke, so do not paste it into docs, issues, PRs, or final summaries.

Privacy-safe database count check:

```powershell
New-Item -ItemType Directory -Force .device-validation\db-snapshot | Out-Null
adb exec-out run-as com.example.gemmacontrol cat databases/gemma_control_database > .device-validation\db-snapshot\gemma_control_database
adb exec-out run-as com.example.gemmacontrol cat databases/gemma_control_database-wal > .device-validation\db-snapshot\gemma_control_database-wal
adb exec-out run-as com.example.gemmacontrol cat databases/gemma_control_database-shm > .device-validation\db-snapshot\gemma_control_database-shm
```

Then query counts locally without printing private message text:

```powershell
@'
import sqlite3
from pathlib import Path

p = Path(r".device-validation/db-snapshot/gemma_control_database")
conn = sqlite3.connect(p)
for table in [
    "message_events",
    "conversations",
    "active_notification_references",
    "follow_ups",
    "reminders",
]:
    print(table, conn.execute(f"select count(*) from {table}").fetchone()[0])
conn.close()
'@ | python -
```

## Appium

Start server:

```powershell
appium --address 127.0.0.1 --port 4723
```

Validated capabilities:

```json
{
  "platformName": "Android",
  "appium:automationName": "UiAutomator2",
  "appium:udid": "1431df87",
  "appium:appPackage": "com.example.gemmacontrol",
  "appium:appActivity": ".MainActivity",
  "appium:noReset": true,
  "appium:autoGrantPermissions": true,
  "appium:newCommandTimeout": 60
}
```

Useful Appium coverage:

- Navigate tabs by visible text.
- Type commands into the voice command field.
- Press icon buttons by content description.
- Assert that safe phrases do not show FunctionGemma fallback.
- Extract page source for accessibility and state assertions.
- Validate reply confirmation sheets without sending unless explicitly confirmed.

Appium boundaries:

- It cannot create a real WhatsApp incoming notification by itself.
- It cannot speak into the microphone.
- It should not bypass the app's user-confirmed reply safety flow.

## Maestro

Maestro is available as:

```powershell
maestro --version
```

Expected result:

```text
2.6.0
```

Recommended use:

- Fast repeatable tap/type smoke flows.
- Navigation checks.
- Setup screen regression checks.
- Basic read-command command-surface checks.

Maestro should not be used for private message text assertions. Prefer state labels, empty-state copy, and counts.

## scrcpy

Use scrcpy when manual assistance or visual inspection is needed:

```powershell
scrcpy --serial 1431df87 --stay-awake --turn-screen-on
```

Recommended use:

- Watch what Appium/ADB is doing.
- Manually speak voice commands.
- Manually approve Xiaomi permission prompts.
- Manually send/receive WhatsApp test messages.

## Android Studio

Useful Android Studio tools:

- Logcat.
- Layout Inspector.
- App Inspection / Database Inspector for debug builds.
- Profiler for CPU, memory, and energy.
- Network Inspector if future network features are added.
- Device Manager and Emulator.

Current app is intentionally local/offline for WhatsApp data, so Network Inspector should normally show no WhatsApp message data leaving the device.

## End-To-End Testing Flow

Use this flow for issue validation:

1. Confirm `main` or the feature branch state:

   ```powershell
   git status --short --branch
   ```

2. Run tests and build:

   ```powershell
   cd C:\Users\Admin\Desktop\gemma_control\GemmaControl
   .\gradlew.bat :app:testDebugUnitTest
   .\gradlew.bat :app:assembleDebug
   ```

3. Install and launch:

   ```powershell
   adb install -r -d .\app\build\outputs\apk\debug\app-debug.apk
   adb shell am start -n com.example.gemmacontrol/.MainActivity
   ```

4. Start visual mirror:

   ```powershell
   scrcpy --serial 1431df87 --stay-awake --turn-screen-on
   ```

5. Start Appium when automation is needed:

   ```powershell
   appium --address 127.0.0.1 --port 4723
   ```

6. Run manual WhatsApp test:

   - Send one or more WhatsApp messages to this phone.
   - Keep notification visible.
   - Ask GemmaControl: `Read my latest WhatsApp messages`.
   - Ask: `Summarize WhatsApp messages`.
   - Ask: `Continue`.
   - If testing replies, use a safe test text and confirm manually before sending.

7. Record only privacy-safe evidence:

   - App state labels.
   - Counts.
   - Error text.
   - Tool/action type.
   - Whether reply confirmation appeared.
   - No private WhatsApp contents in docs, issues, PRs, or final summaries.

## Automation Capability Boundary

What automation can do:

- Install and launch builds.
- Tap, type, swipe, and inspect UI.
- Start and stop Appium sessions.
- Mirror the phone with scrcpy.
- Read app logs.
- Pull debug app-private databases with `run-as`.
- Check permissions, appops, battery, thermal, package, and notification-listener state.

What still needs the user:

- Speak real voice prompts.
- Send real incoming WhatsApp messages from another device/account.
- Confirm Xiaomi private permission prompts that automation cannot bypass.
- Confirm direct WhatsApp replies before sending.
- Decide whether to enable broad future capabilities such as Accessibility service access.

Product boundary:

- ADB, Appium, Maestro, and scrcpy are development/test tools only.
- Normal users will not have these tools connected.
- Product behavior must continue to rely on Android permissions, notification listener access, explicit intents, user-selected files, and user-confirmed actions.

## Known Environment Notes

- `sdkmanager --version` works but reports SDK XML warnings due to duplicate/inconsistent SDK folder entries such as `latest-2`, `emulator-4`, and `android-36.1-2`. The SDK is usable, but cleanup would reduce warnings later.
- Appium helper APKs may not remain visible in `pm list packages` after sessions; this is normal.
- `run-as` works because GemmaControl is a debug/development build.
- Raw coordinate taps can be fragile. Prefer Appium selectors for repeatable checks.
- Avoid running Gradle unit tests and APK assembly in parallel in this repo because KSP generated output/cache packing can race. Sequential verification is more reliable.
