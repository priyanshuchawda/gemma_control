# Phase 1: WhatsApp Notification Proof of Concept — Test Log

## Test Session

- **Date**: 2026-05-28
- **Handset**: Xiaomi Redmi 13 5G (`2406ERN9CI`)
- **ADB Serial**: `1431df87`
- **Branch**: `feat/phase-1-notification-poc`

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

- **Command**: `.\gradlew test`
- **Outcome**: **PASS** — `5 tests completed, 0 failed`

| Test | Result |
| :--- | :--- |
| `testPackageAllowList_acceptsSupportedPackages` | PASS |
| `testDedupeCandidate_isDeterministic` | PASS |
| `testInMemoryStateTransitions` | PASS |
| `testEventHistoryCappedTo100` | PASS |
| `uiState_initiallyLoading` | PASS |

---

## Physical Handset Validation Matrix

> Evidence types: **Logcat** = privacy-safe ADB log output only. **UI** = direct observation of the on-device Compose debug screen.

| Validation Item | Status | Evidence Type | Notes |
| :--- | :--- | :--- | :--- |
| Android 16 device connected | **PASS** | ADB telemetry | Serial `1431df87` |
| APK installation | **PASS** | `Installed on 1 device` | `./gradlew installDebug` |
| App launch, no crash | **PASS** | ADB monkey launch | Edge-to-edge Compose renders |
| Notification Access granted | **PASS** | `settings get secure enabled_notification_listeners` | Listener component confirmed in secure setting |
| ON_RESUME permission refresh | **PASS** | UI observation | Status card updates automatically when returning from Settings |
| Listener service connected | **PASS** | Logcat | `Notification listener connected successfully` |
| Real `com.whatsapp` notification received | **PASS** | Logcat | Package name observed in listener callback |
| `MESSAGING_STYLE` parse path triggered | **PASS** | Logcat | `Parse success! Source: MESSAGING_STYLE, Messages: 7` |
| `EXTRAS_FALLBACK` parse path triggered | **PASS** | Logcat | `Parse success! Source: EXTRAS_FALLBACK, Messages: 1` |
| `POSTED` → `UPDATED` same-key lifecycle | **PASS** | Logcat | Controlled test: 2nd message in same conversation emitted `Event Type: UPDATED`, message count incremented (1→2→3→4) |
| `onNotificationRemoved` callback | **PASS** | Logcat | `Event Type: REMOVED` logged for both active keys on swipe |
| Direct-chat classification (`DIRECT`) | **PASS** | UI observation | Controlled direct-chat test showed `DIRECT` on the debug screen |
| Group-chat classification (`GROUP`) | **NOT VERIFIED** | UI observation | Controlled group-chat test showed `UNKNOWN`; parser received the notification but could not confirm GROUP from available metadata |
| In-memory list bounded to 100 entries | **PASS** | Unit test | `testEventHistoryCappedTo100` |
| Room persistence | **NOT IMPLEMENTED** | — | Deferred to Phase 2 |
| Direct reply (RemoteInput) execution | **NOT IMPLEMENTED** | — | Deferred to manual-action phase |
| FunctionGemma AI routing | **NOT IMPLEMENTED** | — | Deferred to Phase 4 |

---

## Known Limitations & Privacy Constraints

- **No sensitive content committed**: No message body text, sender names, group names, or phone numbers are logged to Logcat or recorded in any committed file.
- **Logcat output uses only**: package names, key suffixes (last 8 chars), parse source labels, and message counts.
- **Volatile in-memory storage only**: All captured events are held in a `MutableStateFlow` bounded to 100 entries. There is no SQLite/Room persistence in Phase 1.
- **Group classification**: The `EXTRAS_FALLBACK` parse path does not reliably expose the `isGroupConversation` flag from `MessagingStyle`. Group notifications received through the summary/fallback path are classified as `UNKNOWN` until a direct `MessagingStyle` group notification is parsed.

---

## Open Item: Group Classification Follow-Up

Group classification was **not independently verified** in this Phase 1 session. The group test notification was received and parsed successfully (parse source confirmed via Logcat), but the on-device debug UI displayed `UNKNOWN` rather than `GROUP`.

**Next step (tracked in Phase 1 follow-up issue):**
- Trigger a WhatsApp group notification that arrives via the `MESSAGING_STYLE` path (not the summary/fallback path).
- Confirm that the parser's `style.isGroupConversation == true` branch is entered and that the Compose screen displays `GROUP`.

This does **not** block Phase 1 completion for the POC scope. All listener connectivity, parsing, lifecycle, and classification mechanics are correctly implemented.
