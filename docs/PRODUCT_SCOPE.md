# Product Scope: WhatsApp On-Device AI Assistant

This document outlines the core functional scope, V1 capability maps, strict product boundaries, and deferred future items for the WhatsApp AI Assistant.

---

## 1. Product Definition
An English-only, private, on-device Android productivity agent for WhatsApp notification workflows. The app captures new WhatsApp notifications after user permission, organises them into a local actionable inbox, lets FunctionGemma propose approved tool calls, persists local follow-ups, priority flags, encrypted reminders, and enables safe user-confirmed WhatsApp replies.

This implementation acts as a secure, daily-use tool-calling manager on your physical **Xiaomi Android 16 handset** rather than a simple alert reader demo.

---

## 2. Core V1 Capabilities & Tools

The functional scope is governed entirely by an English-only interaction paradigm and is mapped to sixteen on-device Kotlin tools.

### A. Notification Ingestion & Actionable Inbox
- **Android 16 Environment**: Deployed on **API Level 36**. Exposes permission dialogs and onboarding flows that comply with Android 16 user permission structures.
- **WhatsApp Package Presence**: WhatsApp (`com.whatsapp`) is confirmed present and active on the physical handset's test profile. WhatsApp Business (`com.whatsapp.w4b`) is absent. Real notification intercepts, direct replies, and intent launching can be fully tested on the connected handset after manual notification access permission is granted.
- **MessagingStyle Extraction**: Uses Android `Notification.MessagingStyle` to parse historical message chains and sender details from bundled notifications.
- **Separation of Titles**: Distinguishes group titles from message sender display names.
- **Deduplication Engine**: Uses deterministic parser candidates that are converted into keyed HMAC `dedupeToken` values before Room storage, blocking duplicate insertions without storing plaintext-derived hashes.
- **Actionable Inbox View**: Allows the user to view recent unresolved, pending, and prioritized items.

### B. Productivity Actions
- **Follow-Ups**: Save specific notification events as local actionable tasks (`create_follow_up_from_message`), list pending follow-ups, and mark them completed.
- **Reminders**: Schedule local encrypted reminder rows and WorkManager notification delivery (`schedule_reminder_for_message`).
- **Prioritization**: Flag important captured messages with a local high/normal priority value.
- **Inbox Cleanup**: Dismiss and hide noise from the local inbox without clearing notifications inside WhatsApp.

### C. Safe Action Executors (Android 16 UI Architecture)
- **Edge-to-Edge Jetpack Compose UI**: Since Android 16 mandates edge-to-edge layouts, the interface uses `enableEdgeToEdge()`, `Modifier.safeDrawingPadding()`, `Modifier.imePadding()`, and proper `Scaffold` inner padding values to prevent UI elements from overlapping status or navigation bars.
- **Predictive Back Navigation**: Built using predictive-back-compatible Navigation Compose libraries (`androidx.navigation:navigation-compose:2.8.0` or higher) and `BackHandler` inside sub-views to handle user gesture transitions smoothly.
- **`open_whatsapp_share_draft`**: Share prepared draft messages with WhatsApp, letting the user manually specify the recipient inside WhatsApp.
- **`open_whatsapp_click_to_chat`**: Open chat windows directly using E.164 verified phone formats.
- **`send_reply_to_active_whatsapp_notification`**: Executes direct system inline replies using `RemoteInput` and `PendingIntent`. This requires the app to fetch active notification instances in real-time, verifying they are from WhatsApp and currently expose direct-reply capabilities. If expired, it defaults to a draft fallback.
- **Blocking Confirmation Sheet**: Exposes recipient, text, and active statuses inside a high-contrast Compose sheet. Requires a physical tap from the user to execute the final send intent.

---

## 3. Scope Exclusions (Strict Non-Goals for V1)

- **No Multi-App Scope (V1)**: The ingestion pipeline targets `"com.whatsapp"` or `"com.whatsapp.w4b"` exclusively in V1.
- **No Python/Streamlit Elements**: Zero Python servers, desktop apps, or Streamlit dashboards.
- **No Auto-Sending**: Headless automated sends are structurally blocked.
- **No Automatic High-Risk Execution**: LiteRT-LM may call the native `WhatsAppTools` callback surface, but those callbacks do not send WhatsApp messages, delete data, or change capture state. Kotlin validates and routes any high-risk action through explicit user confirmation.
- **Limited Speech Input in V1**: English voice commands are supported for reading recent captured messages, searching locally captured messages, listing pending follow-ups/actionable items, preparing a reply to the latest active WhatsApp notification, and creating local follow-up/reminder/priority proposals by message id. Sending and local writes still require manual confirmation.
- **No Multilingual Examples**: Strictly English-only inputs, training sets, and user interfaces. No Hindi or Hinglish commands.
- **No Unofficial APIs**: No Baileys, whatsapp-web.js, or AccessibilityService UI scripting.
- **No Vector Embeddings in V1**: EmbeddingGemma integration is deferred to V2.
- **No Second Generative Summarizer in V1**: Gemma 4 and Gemma 3n summarization are deferred by [LOCAL_SUMMARIZATION_MODEL_DECISION.md](LOCAL_SUMMARIZATION_MODEL_DECISION.md) until benchmark evidence and explicit model approval exist.

---

## 4. Operational Boundaries

- **Database Sandbox**: The assistant cannot access WhatsApp's private database. It reads *only* notifications displayed while the service is BINDed and active.
- **Internet Permission**: The application declares `android.permission.INTERNET` only for explicit FunctionGemma `.litertlm` model binary downloads. WhatsApp notification content, prompts, tool calls, and replies stay private and local. Network delivery of messages is delegated to WhatsApp.
- **Notification Permission**: The application declares `android.permission.POST_NOTIFICATIONS` only for user-confirmed local reminders on Android 13+.
- **Model Importation**: The offline `.litertlm` model binary can be imported via local storage/ADB during development or downloaded through the scoped WorkManager model download flow.
