# GemmaControl: Private On-Device WhatsApp AI Productivity Agent

GemmaControl is an English-only, private, on-device Android productivity agent for WhatsApp notification workflows. The app captures new WhatsApp notifications after user permission, organises them into a local actionable inbox, lets FunctionGemma propose approved tool calls, saves local follow-ups, priority flags, encrypted local reminders, and supports safe user-confirmed WhatsApp replies.

Built entirely as a native application for **Android 16 (API Level 36)**, GemmaControl keeps WhatsApp capture, storage, voice handling, and model inference local on a **Xiaomi Redmi 13 5G** handset, utilizing Google's on-device **LiteRT-LM SDK** and the FunctionGemma MobileActions 270M model.

---

## 🚀 Key Functional Scope

### 1. Notification Ingestion & Actionable Inbox
- **MessagingStyle Parsing**: Extracts chat history lists and sender authors dynamically from bundled notification structures.
- **Group Separation**: Differentiates group conversation headers from the individual message authors.
- **Content Classification**: Tags captured notifications as text, media placeholders, missed calls, hidden content, system notifications, or unknown content before local reads and storage.
- **Event Deduplication**: Uses deterministic parser candidates that are transformed into keyed HMAC `dedupeToken` values before Room storage.
- **Privacy Constraints**: Captured alerts are parsed in memory, and stored locally *only* under explicit storage consent toggles.

### 2. Local Task Management & Productivity
- **Follow-Ups**: Save notification events as unresolved local tasks (`create_follow_up_from_message`), list pending tasks, and mark follow-ups complete.
- **Prioritization**: Mark captured WhatsApp messages as high or normal priority inside the local inbox.
- **Reminders**: Schedule encrypted local reminder rows and WorkManager notification delivery (`schedule_reminder_for_message`).
- **Priorities**: Flag and pin important messages inside the local Compose inbox.
- **Inbox Cleanup**: Dismiss and hide noise from the local inbox without clearing notifications inside WhatsApp.

### 3. Safe Execution Boundaries (Android 16 Target)
- **Edge-to-Edge Jetpack Compose**: Mandatory edge-to-edge UI display with full support for window insets and IME (keyboard) overlays.
- **Predictive Back Navigation**: Built using predictive-back-compatible AndroidX navigation frameworks and custom `BackHandler` hooks.
- **Verified Click-to-Chat Intents**: Securely launches click-to-chat `ACTION_VIEW` intents utilizing E.164 phone formats.
- **Live RemoteInput Reply**: Fetches active system notifications by key in real-time, verifying active status and reply parameters before execution.
- **UI Confirmation Sheets**: Requires a physical user tap on Compose modals before firing direct reply intents. Auto-sending is structurally blocked.

---

## 🛠️ Architecture & Security Design

- **Clean MVVM Architecture**: Separates data structures (Room SQLite database), notification handlers, LiteRT engines, and Jetpack Compose views.
- **Encryption at Rest**: Encrypts sensitive message texts and drafted responses inside Room SQLite utilizing **AES-GCM 256-bit encryption** keys protected by the **Android Keystore**.
- **Truthful Media Handling**: WhatsApp photo/video/audio/document notifications are treated as placeholders only. The app never claims to inspect media contents unless actual media bytes become a future explicit feature.
- **Scoped Network Access**: `android.permission.INTERNET` is declared only for explicit FunctionGemma `.litertlm` model binary downloads. WhatsApp notification data, prompts, tool calls, and replies stay local.
- **Local Reminder Notifications**: `android.permission.POST_NOTIFICATIONS` is declared only for user-confirmed local reminder alerts on Android 13+.
- **LiteRT-LM Native Tool Boundary**: Registers a deliberately small native `ToolSet` with automatic LiteRT tool callbacks enabled. Tool callbacks only capture typed proposals/results; Kotlin still owns permission checks, safety routing, and all user-confirmed execution.

---

## 📂 Project Structure

```text
├── docs/                     # Comprehensive technical documentation
│   ├── ARCHITECTURE.md       # On-device data flow, boundaries, and OS limits
│   ├── CAPABILITY_MATRIX.md  # Android permissions, capability sources, and tool mapping
│   ├── XIAOMI_HYPEROS_RELIABILITY_CHECKLIST.md # Physical reliability checklist for HyperOS capture
│   ├── GEMMA_MODEL_REFERENCE.md # Gemma model/runtime selection reference
│   ├── GEMMA_ASSISTANT_USAGE_PLAN.md # App-specific model usage roadmap
│   ├── DEVICE_INFO.md        # Current connected handset capability snapshot
│   ├── PRODUCT_SCOPE.md      # V1 functional capabilities and non-goals
│   ├── TOOL_REGISTRY.md      # Parameter type schemas for the 16 registry tools
│   ├── NOTIFICATION_PARSING.md # MessagingStyle parsers and keyed dedupe tokens
│   ├── SECURITY_AND_PRIVACY.md # Key cryptography and search memory trade-offs
│   └── DEVICE_VALIDATION.md  # Physical telemetry parameters and test milestones
├── GemmaControl/             # Native Android 16 Kotlin Application
│   ├── app/                  # Main application module
│   │   ├── build.gradle.kts  # Target and compile configurations (API 36)
│   │   └── src/main/         # Kotlin Compose sources, listener services, and manifest
│   └── build.gradle.kts      # Project root Gradle declaration
├── website/                  # Static TypeScript download website
├── plan.md                   # High-level product roadmap
├── detailed_plan.md          # Actionable phase-by-phase implementation blueprints
└── README.md                 # Project cockpit reference
```

---

## 🌐 Download Website

The `website/` directory contains a static TypeScript site for distributing GemmaControl without committing app binaries.

```powershell
cd website
npm run verify
npm run serve
```

The primary APK button points to the canonical GitHub Releases artifact configured in `website/src/release-links.ts`. APK/AAB/EXE/MSI files remain ignored and must not be committed.

---

## 📱 Hardware Telemetry (Discovered via ADB)
- **Manufacturer/Model**: `Xiaomi 2406ERN9CI` (Redmi 13 5G)
- **OS Environment**: `Android 16` (API Level 36)
- **Processor (ABI)**: `arm64-v8a`
- **Total Physical RAM**: approximately 6 GB (`5,531,208 kB`)
- **Connection Port**: Connected via USB (Serial: `1431df87`)
- **Verified Package**: `package:com.whatsapp` is confirmed present on the profile.
