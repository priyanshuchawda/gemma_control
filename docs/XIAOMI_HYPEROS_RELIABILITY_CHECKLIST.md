# Xiaomi / HyperOS Reliability Checklist

This checklist is for the current Xiaomi Redmi 13 5G / Android 16 / HyperOS test device. It avoids ADB-only fixes and describes what a normal user must be able to do through app and system settings.

## Required User Setup

1. Notification Listener Access
   - Open GemmaControl Settings.
   - Tap `Notification Access`.
   - Toggle GemmaControl off and on if capture stopped after reinstall.
   - Return to GemmaControl and tap `Refresh` in the Xiaomi reliability card.

2. Autostart
   - Open GemmaControl Settings.
   - Tap `Autostart`.
   - Enable GemmaControl.
   - This is required so HyperOS can restart capture after boot.

3. Battery
   - Open GemmaControl Settings.
   - Tap `Battery`.
   - Set GemmaControl to `No restrictions`.
   - Disable app-specific battery saver if HyperOS offers that option.

4. Reminder Notifications
   - Grant Reminder Notifications if local reminders should show alerts.
   - This is separate from Notification Listener Access.

5. Microphone
   - Grant Microphone permission for voice commands.
   - This does not affect notification capture.

## In-App Diagnostic Expectations

The Settings screen now has a `Xiaomi / HyperOS Background Settings` diagnostic card.

| Signal | Healthy state | Warning state |
| :--- | :--- | :--- |
| Notification Listener Access | Enabled | Disabled; capture and active reply actions are blocked. |
| Recent listener event | Latest WhatsApp event observed within 120 minutes | No event in this session, or last event older than 120 minutes. |
| Reminder Notifications | Granted | Local reminder alerts may not appear. |
| Microphone | Granted | Voice commands cannot be captured. |

If no recent event is shown, send a test WhatsApp message to the device and refresh the diagnostic card. If it still does not update, toggle Notification Listener Access off/on and re-check Autostart plus Battery settings.

## Manual Physical Test Matrix

| Test | Steps | Pass condition |
| :--- | :--- | :--- |
| Listener toggle recovery | Toggle Notification Listener Access off, return to app, refresh. Toggle on, send a WhatsApp test message, refresh. | Diagnostic moves from blocked to recent-event healthy after the test message. |
| Reboot survival | Reboot phone without ADB fixes. Unlock phone, open GemmaControl, refresh diagnostic, then send WhatsApp test message. | Listener is enabled and new event appears without reinstalling the app. |
| Idle survival | Leave phone idle for at least 2 hours with HyperOS battery rules active. Send WhatsApp test message. | Diagnostic shows recent event after refresh. |
| Swipe-away behavior | Swipe GemmaControl from recents, wait 5 minutes, send WhatsApp test message. | Listener captures the event or diagnostic gives a clear warning. |
| Battery saver stress | Enable device battery saver, wait 10 minutes, send WhatsApp test message. | Result is recorded; if capture fails, app must instruct user to use Battery No restrictions. |
| Notification summary behavior | Send direct and group WhatsApp test messages. | MessagingStyle events continue to parse; summary fallback remains non-canonical. |

## Foreground Service / Health Check Decision

Do not add a foreground service blindly. First capture the manual test results above.

- If listener survives reboot, idle, and swipe-away after Autostart plus Battery No restrictions, keep the current lightweight design.
- If listener dies only under battery saver, improve the Settings warning and documentation first.
- If listener repeatedly dies under normal setup, evaluate a visible foreground service or periodic health check in a new issue with battery impact measured by #120.

## Product Boundary

- No ADB command is required for a real user.
- No hidden background setting is changed programmatically.
- No model download is part of reliability testing.
- The app can guide the user into Xiaomi settings, but the user must manually approve system-level permissions.
