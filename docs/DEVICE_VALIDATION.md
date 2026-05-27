# Device Validation Reference

This document outlines the testing, telemetry logging, and manual verification procedures to validate the application directly on the physically connected Android 16 handset.

---

## 1. Verified Handset Telemetry (Phase 0A Diagnostics Completed)

The following telemetry values were successfully retrieved and verified over the active ADB bridge for the physically connected device:
- **Device Manufacturer**: `Xiaomi`
- **Device Model**: `2406ERN9CI` (Redmi 13 5G)
- **Target OS Version**: `Android 16`
- **Target API Level**: `36`
- **Processor Architecture (ABI)**: `arm64-v8a`
- **Total Physical RAM**: approximately 6 GB (`5,531,208 kB` total hardware capacity)
- **WhatsApp Package (`com.whatsapp`)**: **Confirmed Present and Installed** on the active user profile. Real WhatsApp notification captures, text indexing, and inline `RemoteInput` replies can be fully tested on the connected handset.

---

## 2. On-Device Validation Milestones

> [!WARNING]
> Manual testing on the connected handset is currently **BLOCKED at Milestone 1** due to Xiaomi's USB installation security restriction (`INSTALL_FAILED_USER_RESTRICTED`). The physical device verification remains uncompleted until this setting is toggled ON.

### Milestone 1: Notification Listener Authorization & Intercept (Blocked)
1. Unblock installation by turning ON **Install via USB** in **Settings > Additional Settings > Developer Options**.
2. Run `.\gradlew installDebug` and authorize the prompt on the screen.
3. Launch GemmaControl on the phone and verify that the onboarding edge-to-edge layout is rendering correctly.
4. Click the "Grant Permission" button and toggle GemmaControl to "ON" in the system settings page.
5. Observe permission status card transition automatically to "Active" when returning to the app (ON_RESUME lifecycle callback).
6. Exit the app and trigger a real WhatsApp message to the phone (both direct and group messages).
7. Verify that the debug screen displays the parsed metadata parameters correctly:
   - Event Type (POSTED, UPDATED)
   - Shortened safe key suffix
   - Correct classification (DIRECT, GROUP, or UNKNOWN)
   - Current vs. historic message counts
   - In-app preview of text
   - Inactive/expired lifecycle updates when swiped away (REMOVED)
   - Bounded list size (capped at 100 history entries)

### Milestone 2: Manual Action Testing
1. Trigger a WhatsApp message to generate an active notification on the handset.
2. Open the application debug interface.
3. Select the captured notification and draft a text response manually in the text input area.
4. Tap the "Confirm Direct Reply" button.
5. Verify:
   - The app makes a live call to `NotificationListenerService.getActiveNotifications()` using the specific `notification_key`.
   - The app verifies that the notification is active and exposes direct-reply options.
   - The inline reply is executed via the system `RemoteInput` API.
   - Check the physical WhatsApp application to confirm the reply was successfully transmitted.
6. Swipe away the notification banner on the physical phone, and tap the reply button again. Verify:
   - The active notification lookup fails.
   - The fallback dialog is displayed: *"Direct reply is no longer available. Open WhatsApp with this drafted message instead?"*.
   - Confirming the fallback launches `open_whatsapp_share_draft` or `open_whatsapp_click_to_chat` safely.

### Milestone 3: Model Latency, RAM, & Thermal Benchmark
- Benchmark FunctionGemma 270M performance inside the official Android example or the Google AI Edge Gallery benchmark on the physical handset:
  - **Load Latency**: Time required to load the `.litertlm` binary file into device memory.
  - **Inference Latency**: Time elapsed from English command submission to structured JSON output generation.
  - **RAM Footprint**: Monitor system memory usage to confirm the app stays within safe background execution limits.
  - **Thermal Performance**: Monitor battery temperature variables over prolonged usage.
