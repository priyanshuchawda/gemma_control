# Permission And Capability Matrix

This matrix defines what GemmaControl can actually do on the current Xiaomi Redmi 13 5G / Android 16 / API 36 device profile. ADB is development-only and must never be treated as production app capability.

## Capability Sources

| Capability | Permission / source | What works | What does not work | Safety level | User setup needed | Test method |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| Notification listener | Notification Listener Access | Capture future WhatsApp notifications, inspect active notifications, discover RemoteInput reply actions when WhatsApp exposes them. | Cannot read WhatsApp history, deleted messages, private databases, or notifications that never appeared. | Read-only capture; strict confirmation for replies. | User enables GemmaControl in Android Notification Access. | Settings status, secure setting inspection, privacy-safe listener tests. |
| Microphone | `android.permission.RECORD_AUDIO` | Capture user voice commands for speech recognition and routing. | Does not access phone calls or other app audio. | User-initiated input only. | User grants Microphone permission. | Voice UI permission state and local speech tests. |
| Reminder notifications | `android.permission.POST_NOTIFICATIONS` | Show local reminder/status notifications created by GemmaControl. | Does not read WhatsApp notifications or send WhatsApp messages. | Local notification output only. | User grants Reminder Notifications on Android 13+. | Runtime permission check and reminder worker tests. |
| Local encrypted storage | App-private Room + Android Keystore | Store encrypted captured message metadata, follow-ups, priorities, and reminders. | Does not access WhatsApp's private database or unencrypted chat history. | Local encrypted data boundary. | User enables storage consent for persisted inbox data. | Room, Keystore, migration, and raw-row tests. |
| Local reminder scheduler | WorkManager | Schedule local reminder workers linked to stored messages. | Does not create system calendar events. | Local scheduled work. | No separate setup; notification permission controls visible alerts. | WorkManager and reminder repository tests. |
| Capture preferences | App DataStore | Pause or resume GemmaControl's own capture behavior. | Does not change WhatsApp or Android notification settings. | Local app setting; confirm model-proposed changes. | None beyond app install. | Preferences and tool executor tests. |
| WhatsApp intents | WhatsApp package + Android intents | Open WhatsApp share and click-to-chat draft flows after confirmation. | Does not silently send or scrape chats. | External app launch after confirmation. | WhatsApp must be installed. | Package resolution and manual intent tests. |
| Active notification RemoteInput | Live WhatsApp notification reply action | Reply through an active notification after strict manual confirmation. | Cannot reply if notification expired, was cleared, or lacks RemoteInput. | Strict manual confirmation. | Notification must still be active. | Physical active-reply and expiry tests. |
| Package visibility | Android package visibility / intent resolution | Resolve installed apps and supported deep links visible to GemmaControl. | Does not grant hidden package access. | Intent resolution only. | Target app must be installed and visible. | PackageManager tests and physical launch validation. |
| Contacts | `READ_CONTACTS` future permission | Future name resolution for contacts such as Mom/Dad if approved. | Not used by current V1 tools. | Future sensitive read permission. | Not requested in current V1. | Future resolver tests. |
| Calendar | Calendar intents or permissions, future | Future event creation if approved. | Current reminders are local app reminders, not calendar events. | Future confirmation-required external write. | Not requested in current V1. | Future calendar tests. |
| Media picker | Android photo picker or scoped media permissions, future | Future user-selected media analysis only, as defined in [MEDIA_UNDERSTANDING_BOUNDARY.md](MEDIA_UNDERSTANDING_BOUNDARY.md). | No background gallery access and no analysis of notification placeholders. | Future user-selected media only. | Not requested in current V1. | Future picker contract and media-policy tests. |
| Accessibility service | Accessibility service, V2 evaluation only | Could read visible screen text or perform narrow UI actions only if explicitly approved later. | Does not read WhatsApp databases, recover hidden content, or allow silent sends/destructive actions. | High-risk assistive mode; no V1 production tools. | Not requested in current V1; future setup requires explicit disclosure and manual enablement. | [ACCESSIBILITY_SERVICE_EVALUATION.md](ACCESSIBILITY_SERVICE_EVALUATION.md), future policy review, and physical Xiaomi persistence tests. |
| ADB | USB debugging / developer bridge | Install, inspect, and test during development. | Never real-user app functionality. | Development-only, excluded from tools. | Developer-only USB debugging. | Development terminal commands only. |

Generic source planning is documented in [GENERIC_NOTIFICATION_SOURCE_ABSTRACTION.md](GENERIC_NOTIFICATION_SOURCE_ABSTRACTION.md). SMS, Gmail, Phone, Calendar, and Other sources are not enabled V1 production capabilities.

## Tool Capability Mapping

| Tool | Capability source(s) | Missing-capability response |
| :--- | :--- | :--- |
| `list_recent_whatsapp_messages` | Local encrypted storage, notification listener | Enable storage consent or Notification Listener Access. |
| `search_whatsapp_messages` | Local encrypted storage, notification listener | Enable storage consent or Notification Listener Access. |
| `get_whatsapp_message_details` | Local encrypted storage | Enable encrypted local inbox storage. |
| `get_actionable_inbox` | Local encrypted storage | Enable encrypted local inbox storage. |
| `create_follow_up_from_message` | Local encrypted storage | Enable encrypted local inbox storage. |
| `list_pending_follow_ups` | Local encrypted storage | Enable encrypted local inbox storage. |
| `mark_follow_up_completed` | Local encrypted storage | Enable encrypted local inbox storage. |
| `schedule_reminder_for_message` | Local encrypted storage, local reminder scheduler, reminder notifications | Enable storage and grant Reminder Notifications for visible alerts. |
| `mark_message_priority` | Local encrypted storage | Enable encrypted local inbox storage. |
| `draft_whatsapp_reply` | Local encrypted storage | Enable encrypted local inbox storage. |
| `open_whatsapp_share_draft` | Package visibility, WhatsApp intents | Install WhatsApp or fix app visibility before opening a draft. |
| `open_whatsapp_click_to_chat` | Package visibility, WhatsApp intents | Install WhatsApp or fix app visibility before opening a draft. |
| `send_reply_to_active_whatsapp_notification` | Notification listener, active notification RemoteInput | Enable Notification Listener Access, then keep the WhatsApp notification active. |
| `pause_whatsapp_capture` | Capture preferences, notification listener | Enable Notification Listener Access so capture controls are meaningful. |
| `resume_whatsapp_capture` | Capture preferences, notification listener | Enable Notification Listener Access so capture controls are meaningful. |
| `delete_local_whatsapp_data` | Local encrypted storage | Enable encrypted local inbox storage before deletion. |

## Product Rules

- ADB-only abilities are excluded from every production tool requirement.
- Non-WhatsApp notification sources are not production-enabled and must not receive reply/send actions accidentally.
- Missing capabilities must produce setup guidance instead of a hallucinated success message.
- FunctionGemma can propose tools, but Kotlin validates capability state, safety level, parameters, and confirmation before execution. The deterministic safety policy is documented in [ASSISTANT_SAFETY_POLICY.md](ASSISTANT_SAFETY_POLICY.md).
- Accessibility, contacts, calendar, media, EmbeddingGemma, and extra Gemma models remain future decisions and are not required for the current WhatsApp V1 path. Media is placeholder-only in V1 per [MEDIA_UNDERSTANDING_BOUNDARY.md](MEDIA_UNDERSTANDING_BOUNDARY.md). Accessibility is specifically deferred by [ACCESSIBILITY_SERVICE_EVALUATION.md](ACCESSIBILITY_SERVICE_EVALUATION.md).
