# Technical Implementation Plan: WhatsApp On-Device AI Assistant

This document outlines the detailed engineering roadmap for the WhatsApp AI Assistant. The application is a self-contained, offline native Android mobile productivity agent targeting **Android 16 (API Level 36)**. It captures and indexes WhatsApp message notifications, organizes them locally, interprets English commands via offline **FunctionGemma 270M**, and executes safe local actions under strict user confirmation.

---

## Phase 0A: Verified Device Documentation

The primary physical development and runtime environment is verified as:
- **Device Manufacturer**: `Xiaomi`
- **Device Model**: `2406ERN9CI` (Redmi 13 5G)
- **Target OS Version**: `Android 16`
- **Target API Level**: `36`
- **ABI (Processor Architecture)**: `arm64-v8a`
- **Total Physical RAM**: approximately 6 GB (`5,531,208 kB`)
- **Connection Status**: Connected via USB (Serial: `1431df87`)
- **Software Dependencies**: WhatsApp (`com.whatsapp`) is confirmed present and active on the test profile of the physically connected Redmi 13 5G. WhatsApp Business (`com.whatsapp.w4b`) is not present. Real notification capture and replies can be fully tested on the connected handset after manual notification access authorization.

The roadmap is scoped to this Xiaomi Redmi 13 5G / Android 16 / API 36 device profile unless a separate test matrix explicitly adds another handset.

---

## Phase 0B: Diagnostics & Model Feasibility

### 1. FunctionGemma 270M Benchmarking
Before developing app features, execute a standalone latency, memory pressure, and thermal benchmark of FunctionGemma 270M on the Redmi 13 5G:
- Run the FunctionGemma model using an official Android example or the Google AI Edge Gallery benchmark on the handset.
- **Metrics to Record**:
  - **Model Load Latency**: Time required to load the `.litertlm` binary into physical RAM.
  - **Inference Latency**: Time elapsed from submitting an English command to receiving a structured JSON tool proposal.
  - **RAM Footprint**: Memory consumption peaks and baseline delta under model execution.
  - **Thermal Profile**: Device temperature changes over prolonged inference trials.
- **Goal**: Confirm whether FunctionGemma 270M provides acceptable latency and stays within safe background execution limits on the Redmi 13 5G.

---

## Phase 1: WhatsApp Notification Proof-of-Concept

### 1. Android 16 Compose Project Scaffold
- **Initialization**: Create the native Compose skeleton in the workspace sub-folder:
  ```bash
  android create empty-activity --name="GemmaControl" --output=./GemmaControl
  ```
- **Android 16 Compilation Targets**: Set `compileSdk = 36` and `targetSdk = 36` in `GemmaControl/app/build.gradle`.
- **Edge-to-Edge Handling**:
  - Android 16 enforces edge-to-edge layout display by default for apps targeting API 36; opt-outs are disabled.
  - In `MainActivity.onCreate()`, call `enableEdgeToEdge()` before `setContent`.
  - In Compose views, use `Modifier.safeDrawingPadding()`, `Modifier.imePadding()`, and proper `Scaffold` inner padding properties to ensure UI components never overlap status or navigation bars.
- **Predictive Back Navigation**:
  - Predictive back gestures are enabled by default for API 36 targeting.
  - Plan navigation using predictive-back-compatible Navigation Compose libraries (`androidx.navigation:navigation-compose:2.8.0` or higher).
  - Use `BackHandler` inside sub-views to intercept back operations (e.g. closing confirmation modals) instead of legacy `onBackPressed()` overrides.

### 2. NotificationListenerService Setup
- **Onboarding Flow**: Build a Compose onboarding view to request Notification Access, directing users to the system Settings page:
  ```kotlin
  val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
  context.startActivity(intent)
  ```
- **Parsing Subsystem (`WhatsAppNotificationParser`)**:
  - Code `WhatsAppNotificationListener` extending `NotificationListenerService` and filter for package `"com.whatsapp"` or `"com.whatsapp.w4b"`.
  - Decode notifications utilizing `Notification.MessagingStyle` to parse historical unread message chains, separating group conversation titles from message author display names.
  - Fall back gracefully to notification extras if `MessagingStyle` is missing.
  - Generate deterministic dedupe candidates from notification fields to prevent duplicated rows on banner refreshes. The repository converts candidates into keyed HMAC `dedupeToken` values before Room storage.
  - Monitor `onNotificationRemoved` to update active status.
- **Debug View Compose Screen**: Build a simple debug screen displaying raw parsed notification parameters on the physical handset to verify basic ingestion mechanisms operate correctly without any AI components.

---

## Phase 2: Local Actionable Inbox Foundation

### 1. Room SQLite Database Schema
Current Room Version 4 stores five local entities:
- **`ConversationEntity`**: Tracks opaque conversation IDs, encrypted display names, types (DIRECT or GROUP), and optional encrypted E.164 phone mappings.
- **`MessageEventEntity`**: Caches notification keys, parse source, keyed dedupe tokens, priorities, encrypted sender names, and encrypted message text.
- **`ActiveNotificationReferenceEntity`**: Caches the live system `notification_key` to query state at reply confirmation time.
- **`FollowUpEntity`**: Models unresolved tasks, priorities, and completed timestamps.
- **`ReminderEntity`**: Manages encrypted local reminder notes and scheduled delivery metadata powered by standard system `WorkManager`.

### 2. Encryption Strategy
- **AES-GCM Key Encryption**: Implement AES-GCM (256-bit key) generated inside the Android Keystore container.
- **Plaintext Search Trade-off**:
  - Encrypted database columns cannot be searched natively via SQLite `LIKE` queries.
  - **Current Design**: Human-readable conversation names, sender names, phone values, message bodies, and reminder notes are encrypted at rest. SQLite may filter by non-sensitive operational metadata such as timestamps, source package, parse source, priority, and keyed dedupe tokens. Keyword searches decrypt bounded candidates in memory and filter locally.

### 3. Privacy Control States
- **Storage Toggle**: If set to "OFF", notification items are held in-memory and parsed dynamically, writing nothing to Room.
- **Pause/Resume Flags**: Evaluated in the listener before writing.
- **Data Purge Utility**: Wipes all database tables concurrently under a single cascading Room database transaction.

---

## Phase 3: Non-AI WhatsApp Action Tools

### 1. Drafting and Shared Intents
- **`open_whatsapp_share_draft`**: Launches share chooser intents:
  ```kotlin
  val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_TEXT, messageText)
  }
  context.startActivity(Intent.createChooser(intent, "Share WhatsApp Draft"))
  ```
- **`open_whatsapp_click_to_chat`**: Opens chats via E.164 phone formats:
  ```kotlin
  val formattedNumber = phoneNumberE164.replace("+", "")
  val intent = Intent(Intent.ACTION_VIEW).apply {
      data = Uri.parse("https://wa.me/$formattedNumber?text=${Uri.encode(messageText)}")
      setPackage("com.whatsapp")
  }
  context.startActivity(intent)
  ```
  *Constraint*: Rejects arbitrary contact names. Requires a verified phone number mapping.

### 2. Live Notification Retrieval and RemoteInput Reply
- **Direct RemoteInput Execution**:
  1. Retrieve active notifications dynamically by key using `NotificationListenerService.getActiveNotifications(arrayOf(notificationKey))`.
  2. Validate that the notification is active, from WhatsApp, and contains valid `RemoteInputs`.
  3. Prompt the user for manual physical confirmation via the Jetpack Compose card.
  4. Fire the reply `PendingIntent` after confirmation.
  5. **Fallback**: If the notification is expired (swiped or cleared), display a fallback dialog: *"Direct reply is no longer available. Open WhatsApp with this drafted message instead?"*, and route to the share chooser.

---

## Phase 4: FunctionGemma Local Tool Routing

### 1. LiteRT-LM & Configuration Setup
- Add stable Maven dependency inside the Gradle configuration (Note: the exact official Maven artifact and version MUST be verified during Phase 4):
  ```kotlin
  // Future Verification Required: Confirm official Maven artifact path and version
  // implementation("com.google.ai.edge.litertlm:litertlm-android:1.0.0")
  ```
- Configure the local engine using a small native `ToolSet` callback surface. LiteRT-LM may call these callbacks automatically, but the callbacks only capture proposal/result state and do not directly execute sensitive Android actions:
  ```kotlin
  val conversationConfig = ConversationConfig(
      tools = listOf(tool(WhatsAppTools(onFunctionCalled = ::recordAction))),
      automaticToolCalling = true
  )
  ```

### 2. Command Translation & Schema Validation
- **Model Role**: FunctionGemma parses the user's English command and selects the correct tool and parameters (e.g., extracting exact target string payloads, dates, or contact identities). It performs **no** creative or summarizing activities.
- **Safety Parser**: Once the model generates a proposal or native callback action, the Kotlin application validates it against type schemas and resolves the arguments. Sensitive execution items such as WhatsApp sends, draft opening, capture toggles, and local deletion are put behind Compose confirmation cards before executing.

---

## Phase 5: Safety and Usability Evaluation

- **English-Only Benchmark Suite**: Create a mock dataset consisting of 100+ English test cases.
- **Edge Case Coverage**: Test and log specific safety boundaries:
  - **No-Notification Case**: Ensure the model handles requested actions when the target contact has no cached notifications.
  - **Ambiguity Case**: Verify that requesting an action with two contacts named "Amit" prompts a choice menu instead of executing a guess.
  - **Auto-Reply Refusal**: Verify that requests to auto-reply are rejected by the system.
  - **Bulk Block**: Ensure bulk message requests are blocked.
  - **Data Purging Check**: Confirm that calling `delete_local_whatsapp_data` purges the Room tables completely and cancels indexing queues.

---

## Phase 6: Fine-Tuning (Future Phase)
- **Strict English Focus**: Only English examples covering correct mapping outcomes, ambiguous contacts, missing notifications, and task prioritizations. Fine-tuning is strictly deferred until a baseline app exists and is thoroughly evaluated.

---

## Phase 7: Future Semantic Search (Future Phase)
- **EmbeddingGemma**: Defer this build entirely. Introduce it inside Phase 7 only after baseline tool routing is verified functional on the physical Android 16 handset.
