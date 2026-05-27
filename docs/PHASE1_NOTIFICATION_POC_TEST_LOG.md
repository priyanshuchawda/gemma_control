# Phase 1: WhatsApp Notification Proof of Concept — Test Log

## Test Session

- **Date**: 2026-05-28
- **Handset**: Xiaomi Redmi 13 5G (`2406ERN9CI`)
- **ADB Serial**: `1431df87`
- **Branch**: squash-merged into `master` via PR #2

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
| Notification Access granted | **PASS** | Secure settings component | Listener component confirmed |
| ON_RESUME permission refresh | **PASS** | UI observation | Status card auto-updates on return from Settings |
| Listener service connected | **PASS** | Logcat | `Notification listener connected successfully` |
| Real `com.whatsapp` notification received | **PASS** | Logcat | Package name observed in listener callbacks |
| `MESSAGING_STYLE` parse path | **PASS** | Logcat | `Parse success! Source: MESSAGING_STYLE` |
| `EXTRAS_FALLBACK` parse path | **PASS** | Logcat | `Parse success! Source: EXTRAS_FALLBACK` |
| `POSTED` → `UPDATED` same-key lifecycle | **PASS** | Logcat | Controlled test: same key emitted UPDATED, message count incremented 1→2→3→4 |
| `onNotificationRemoved` callback | **PASS** | Logcat | `Event Type: REMOVED` for both active keys on swipe |
| Direct-chat classification (`DIRECT`) | **PASS** | UI observation | Controlled direct test: MessagingStyle card showed `DIRECT` |
| Group-chat classification (`GROUP`) | **PASS** | UI observation | Controlled group test: MessagingStyle card showed `GROUP` |
| Dual-notification behavior understood | **PASS** | UI + Logcat | See note below |
| In-memory list bounded to 100 entries | **PASS** | Unit test | `testEventHistoryCappedTo100` |
| Room persistence | **NOT IMPLEMENTED** | — | Deferred to Phase 2 |
| Direct reply (RemoteInput) execution | **NOT IMPLEMENTED** | — | Deferred to manual-action phase |
| FunctionGemma AI routing | **NOT IMPLEMENTED** | — | Deferred to Phase 4 |

---

## Dual-Notification Behavior (Expected)

WhatsApp posts **two separate notifications** for every incoming message:

1. **Individual chat notification** — carries full `MessagingStyle` data. This is the primary notification the parser extracts `DIRECT` or `GROUP` classification from, along with message history stacks.
2. **Group summary / inbox notification** — a secondary rollup notification that aggregates all active conversations. This takes the `EXTRAS_FALLBACK` parse path and always shows `UNKNOWN` for conversation type because `isGroupConversation` is not available through extras alone.

This is expected Android notification architecture behaviour. The `UNKNOWN` card for any given message is always the summary notification, not a parse failure. Both cards correctly show the same notification key suffix.

**Example (direct message "hi"):**
- Card 1: `POSTED` · `DIRECT` · `MESSAGING_STYLE` · 1 message
- Card 2: `POSTED` · `UNKNOWN` · `EXTRAS_FALLBACK` · 1 message (summary)

**Example (group message):**
- Card 1: `POSTED` · `GROUP` · `MESSAGING_STYLE` · 1 message
- Card 2: `POSTED` · `UNKNOWN` · `EXTRAS_FALLBACK` · 1 message (summary)

---

## Known Limitations & Privacy Constraints

- **No sensitive content committed**: No message body text, sender names, group names, or phone numbers are logged or committed.
- **Logcat is restricted to**: package names, key suffixes (last 8 chars), parse source labels, and message counts.
- **Volatile in-memory only**: All captured events are held in a `MutableStateFlow` bounded to 100 entries. No disk persistence in Phase 1.
- **Dual-notification display**: Each real WhatsApp message produces two event cards in the debug UI (one primary, one summary). This is correct and expected.

---

## Phase 1 POC — COMPLETE ✅

All listener connectivity, parsing paths, conversation classification, lifecycle transitions, and removal callbacks have been verified on the physical Xiaomi Redmi 13 5G running Android 16.
