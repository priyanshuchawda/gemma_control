# Real-Device Assistant Test Matrix

Snapshot date: 2026-06-05

Target device: Xiaomi/Redmi `2406ERN9CI`, Android 16/API 36, HyperOS `OS3.0`

This matrix is the physical-device validation gate for assistant, notification, and tool-execution changes. It is intentionally privacy-safe: do not commit screenshots, raw logcat dumps, WhatsApp message bodies, sender names, group names, phone numbers, APKs, or model binaries.

## Required Evidence Format

For each manual run, record a short local note in `.device-validation/` or in the PR body:

```text
Date:
Commit:
Device:
Android/API:
App version/build:
Tester:
Phone connected: yes/no
Model download/import performed: no

Cases run:
- RD-001 Install and launch: PASS/FAIL/NA - note
- RD-002 Permission setup: PASS/FAIL/NA - note

Private data handling:
- No screenshots committed: yes/no
- No raw logcat committed: yes/no
- No WhatsApp names/message text included: yes/no
```

The `.device-validation/` folder is ignored by git. Keep detailed local notes there, then paste only privacy-safe summaries into PRs.

## Privacy-Safe ADB Helpers

Use these scripts from the repository root:

```powershell
.\scripts\android\collect-device-safety-snapshot.ps1
.\scripts\android\tail-privacy-safe-logcat.ps1
```

The snapshot helper collects device/app readiness facts only: build properties, package presence, permission/app-op summaries, notification-listener setting, battery/thermal status, and app memory if running. It does not capture screenshots or notification content.

The logcat helper filters to known GemmaControl tags. Even filtered logs must be treated as local-only unless manually reviewed and redacted.

## Manual Test Matrix

| ID | Area | Setup | Steps | Pass condition | Safe evidence |
| :--- | :--- | :--- | :--- | :--- | :--- |
| RD-001 | Install and launch | Phone connected, USB install manually allowed on HyperOS | Run `.\gradlew.bat :app:installDebug`, then launch app. | App opens without crash and shows setup/home flow. | Install result, app process present, no screenshot required. |
| RD-002 | Permission setup | Fresh or upgraded install | Grant Notification Access, Microphone, and Reminder Notifications when prompted. | Settings/Home reflect granted state after refresh. | `collect-device-safety-snapshot.ps1` output summary. |
| RD-003 | Xiaomi setup | HyperOS device | Enable Autostart and Battery `No restrictions` manually from app Settings links. | Xiaomi diagnostic card no longer warns for these setup items after refresh. | Local note; no private screen capture. |
| RD-004 | Notification listener connection | Notification Access enabled | Toggle listener off/on manually, return to app, refresh. | App detects disabled/enabled states and reconnects after enabled. | Filtered logcat tag event, settings state. |
| RD-005 | Direct WhatsApp capture | WhatsApp installed; storage opt-in as needed | Ask tester/contact to send one direct text message. | App records one canonical direct chat event, not duplicate summary noise. | Message count/classification only; no message body/name. |
| RD-006 | Group WhatsApp capture | Test group available | Ask tester/contact to send one group text message. | App records group conversation and sender separation correctly. | Classification only; no group/member names. |
| RD-007 | Updated notification dedupe | Active WhatsApp thread | Receive two messages in same thread while notification remains active. | POSTED/UPDATED lifecycle does not duplicate stored message rows incorrectly. | Counts and parse source labels only. |
| RD-008 | Removal lifecycle | Captured notification visible | Swipe WhatsApp notification away. | App marks notification reference removed and no longer offers active reply to that key. | Filtered logcat key suffix only. |
| RD-009 | Active latest read | One live WhatsApp notification visible | Use `Read my latest WhatsApp messages`. | Reads only active notification rows. If no active notification exists, it says no active WhatsApp notifications are available and does not read old stored history. | Active/stored count only; no private text in PR. |
| RD-009B | Stored inbox read | Storage enabled with safe test messages | Use `Read my latest stored messages`. | Reads latest locally stored captured messages, not the user's outgoing reply echo. | Spoken/UI count and content-kind labels, no private text in PR. |
| RD-010 | Adaptive read/summarize/continue | Multiple stored safe test messages | Try `Summarize WhatsApp`, `continue`, and `read messages from <test chat>`. | 1-3 messages read directly; larger sets summarize/group; continuation advances. | Counts and command names. |
| RD-011 | Important-only read | At least one message marked high priority | Try `read only important WhatsApp messages`. | Only high-priority local rows are read/summarized. | Count and priority labels. |
| RD-012 | Search and filters | Stored safe test messages | Try `search WhatsApp for payment`, `find messages from <test chat> about dinner`, and `search last 30 minutes for invoice`. | Search returns local captured rows with sender/time filters applied. | Query category and result count only. |
| RD-013 | Pending follow-ups | Stored message id visible in app | Create follow-up for a test message id, then `show pending follow ups`. | Local confirmation appears before write; pending list shows the task after confirmation. | Tool name, confirmation shown, task count. |
| RD-014 | Reminder scheduling | Reminder notification permission granted | Schedule reminder for a test message id with a future time. | Local confirmation appears; reminder row/work scheduled; notification fires near due time. | Tool name, scheduled/notified status only. |
| RD-015 | Priority update | Stored message id visible | Mark test message important, then normal. | Local confirmation appears; inbox/read filters reflect priority changes. | Message id suffix or test id only, priority label. |
| RD-016 | Active reply, single target | Exactly one live reply-capable WhatsApp notification | Say `reply to the latest message: test reply`. | Confirmation sheet shows target; send succeeds only after physical tap. | PASS/FAIL and target count, no reply content if private. |
| RD-017 | Active reply ambiguity | Two live reply-capable WhatsApp notifications | Say generic `reply ok`. | App asks which chat instead of choosing silently. | Clarification text category. |
| RD-018 | Named active reply | One live target matches a test chat name | Say `reply to <test chat>: test reply`. | Matching active notification is used; otherwise draft confirmation appears. | Match/draft status only. |
| RD-019 | No active reply | No live reply-capable notification | Say `reply to the latest message: test reply`. | App does not send; shows no-active-target guidance or safe draft path. | Failure/clarification category. |
| RD-020 | Media placeholder | Receive photo/sticker/document notification | Read/summarize/search around that event. | App says attachment/media placeholder and does not invent image/audio contents. | Content-kind label only. |
| RD-021 | Hidden content | WhatsApp content hidden/redacted by system settings | Read/summarize that event. | App says content is hidden/unavailable and does not invent text. | Content-kind label only. |
| RD-022 | Share draft intent | App installed; WhatsApp installed | Confirm `open_whatsapp_share_draft` proposal from controlled command/model path. | WhatsApp opens draft/share UI; user still sends manually. | Intent launched status only. |
| RD-023 | Click-to-chat draft | Verified test E.164 number | Confirm click-to-chat draft proposal. | WhatsApp opens target draft; user still sends manually. | Intent launched status only, redact phone number. |
| RD-024 | Local delete controls | Storage contains safe test messages | Delete one conversation and delete all local stored data from UI. | Confirmation appears; local rows are removed; WhatsApp itself is untouched. | Counts before/after only. |
| RD-025 | Reboot survival | Autostart/battery setup completed | Reboot phone manually, unlock, open app, send test WhatsApp message. | Listener remains enabled or app gives clear recovery instructions. | PASS/FAIL plus diagnostic state. |
| RD-026 | Idle survival | Battery rules active | Leave phone idle for at least 2 hours, then send test message. | Listener captures or diagnostic gives clear warning. | PASS/FAIL plus diagnostic state. |
| RD-027 | Swipe-away survival | App in recents | Swipe app from recents, wait 5 minutes, send test message. | Listener captures or diagnostic gives clear warning. | PASS/FAIL plus diagnostic state. |
| RD-028 | Battery saver stress | Enable device battery saver | Wait 10 minutes, send test message. | Result recorded; if capture fails, Settings guidance points to Battery No restrictions. | PASS/FAIL plus diagnostic state. |
| RD-029 | Speech recognition fallback | On-device speech unavailable or denied | Deny/disable speech path, then use typed command. | Typed command path still works; voice denial gives recoverable UI. | State/category only. |
| RD-030 | TTS behavior | Device volume audible | Trigger read/summarize. | TTS starts/stops correctly and does not overlap incoherently with UI state. | Count and stop/resume status. |
| RD-031 | Model missing path | No new model download/import | Remove/avoid model only if tester intentionally uses missing-model environment. | Unsupported model commands fall back to deterministic clarification without downloading. | Model state only. |
| RD-032 | Installed FunctionGemma smoke | Existing model already installed; no download | Run one no-private-data model proposal command. | Model initializes once, proposes a valid/clarified tool, and can be stopped/released. | Tool name/status, latency if dashboard available. |

## PR Regression Checklist

Use this table in every future PR that touches the listed area.

| PR touches | Minimum device checks to mention |
| :--- | :--- |
| Notification parsing/listener | RD-004, RD-005, RD-006, RD-007, RD-008 |
| Stored inbox/storage/delete | RD-009, RD-012, RD-024 |
| Voice parser/planner/read aloud | RD-009, RD-010, RD-011, RD-029, RD-030 |
| Reply execution | RD-016, RD-017, RD-018, RD-019 |
| Local tool execution | RD-012, RD-013, RD-014, RD-015, RD-022, RD-023 |
| Media/hidden-content behavior | RD-020, RD-021 |
| Xiaomi reliability/settings | RD-002, RD-003, RD-025, RD-026, RD-027, RD-028 |
| Model runtime/proposal flow | RD-031, RD-032 plus #120 benchmark output when available |

If no phone is connected for a PR, state `Device validation deferred: phone unavailable` and explain which RD cases are still pending.

## ADB Command Rules

Allowed for privacy-safe validation:

- `adb devices -l`
- `adb shell getprop ...`
- `adb shell settings get secure enabled_notification_listeners`
- `adb shell cmd appops get com.example.gemmacontrol`
- `adb shell dumpsys battery`
- `adb shell dumpsys thermalservice`
- `adb shell dumpsys meminfo com.example.gemmacontrol`
- `adb shell pidof com.example.gemmacontrol`
- `adb shell monkey -p com.example.gemmacontrol 1`

Avoid unless explicitly needed and manually reviewed:

- full `adb logcat` without tag filters
- `adb bugreport`
- screenshots or screen recordings
- dumping notification records or app databases
- commands that include real WhatsApp text, phone numbers, sender names, or group names

Xiaomi/HyperOS note: do not rely on ADB input injection for real-user setup. Notification Access, Autostart, Battery, and install restrictions should be changed manually by the user/tester.
