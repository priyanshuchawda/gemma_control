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

Manual testing on the connected handset must follow three specific milestones before integrating any AI or model components:

### Milestone 1: Notification Listener Authorization & Intercept
1. Install the baseline sandbox Compose app onto the handset using `android run`.
2. Launch the app and verify the Onboarding screen is displayed.
3. Click the "Grant Notification Access" button to navigate to the system Settings page.
4. Toggle "On" for the GemmaControl application.
5. Exit the app and trigger a real WhatsApp notification (either a direct or group chat).
6. Verify that the debug screen displays the parsed metadata correctly:
   - Exposes `"com.whatsapp"` package filter outcome.
   - Shows correct separation of group title from the sender's display name.
   - Confirms direct vs group classification.
   - Confirms deduplication hashes prevent duplicate rows on notification refreshes.

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
