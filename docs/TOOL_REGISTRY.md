# Tool Registry Reference: FunctionGemma 16-Tool Schema

This document establishes the official specifications, parameters, safety constraints, and English schemas for the sixteen local tools registered inside the assistant's offline routing module.

All tool calling runs with **LiteRT-LM Automatic Tool Calling Disabled** (`automaticToolCalling = false`). The model is restricted to proposing tool calls, which the Kotlin controller validates and resolves against live system states.

---

## 1. Schema Specifications & Parameters

### Read Tools (No Confirmation Required)

#### 1. `list_recent_whatsapp_messages`
- **Purpose**: Return recently captured WhatsApp notification messages stored in the local SQLite database.
- **Parameters**:
  - `conversation_name` (optional string): Filter by conversation name (direct sender or group header).
  - `limit` (integer): Maximum number of entries to return.
  - `since_minutes` (optional integer): Filter messages received within this time window.

#### 2. `search_whatsapp_messages`
- **Purpose**: Search locally stored captured WhatsApp notification messages using keyword-based FTS/LIKE queries.
- **Parameters**:
  - `query` (string): The search keyword.
  - `conversation_name` (optional string): Filter by conversation name.
  - `from_timestamp` (optional string): Start time boundary.
  - `to_timestamp` (optional string): End time boundary.

#### 3. `get_whatsapp_message_details`
- **Purpose**: Retrieve the complete decrypted body and metadata for a single stored notification event.
- **Parameters**:
  - `message_event_id` (string): The row row ID in SQLite.

#### 4. `get_actionable_inbox`
- **Purpose**: Show unresolved messages requiring prompt attention, prioritizing notifications with uncompleted follow-ups or high-priority flags.
- **Parameters**:
  - `status` (optional string): Filter by task state (`"PENDING"`, `"COMPLETED"`).
  - `priority` (optional string): Filter by priority level (`"HIGH"`, `"NORMAL"`).
  - `limit` (integer): Maximum number of items.

---

### Productivity Tools (Local Execution After UI Presentation)

#### 5. `create_follow_up_from_message`
- **Purpose**: Save a WhatsApp message event as a task you must act on, creating a row in the follow-up table.
- **Parameters**:
  - `message_event_id` (string): Row ID of the reference message.
  - `follow_up_title` (string): Description of the actionable task.
  - `due_at` (optional string): Iso-timestamp for deadline alerts.
  - `priority` (optional string): Task priority (`"HIGH"`, `"NORMAL"`, `"LOW"`).

#### 6. `list_pending_follow_ups`
- **Purpose**: Show unresolved follow-up items stored locally.
- **Parameters**:
  - `limit` (integer): Maximum number of items.
  - `priority` (optional string): Filter by task priority.

#### 7. `mark_follow_up_completed`
- **Purpose**: Mark a local follow-up task finished.
- **Parameters**:
  - `follow_up_id` (string): Row ID of the target follow-up.

#### 8. `schedule_reminder_for_message`
- **Purpose**: Schedule a local notification reminder using Android system `WorkManager`.
- **Parameters**:
  - `message_event_id` (string): Reference message row ID.
  - `remind_at` (string): Target alert time (Iso-timestamp).
  - `reminder_note` (optional string): Custom text description to present in the reminder alert.

#### 9. `mark_message_priority`
- **Purpose**: Flag and pin an important notification message inside your local inbox view.
- **Parameters**:
  - `message_event_id` (string): Reference message row ID.
  - `priority` (string): Priority value (`"HIGH"`, `"NORMAL"`).

---

### Message Preparation Tools (Confirmation Required)

#### 10. `draft_whatsapp_reply`
- **Purpose**: Prepare reply draft text locally without sending anything. The confirmed result is shown inside GemmaControl; use the WhatsApp open-draft tools when the user explicitly wants to move prepared text into WhatsApp.
- **Parameters**:
  - `message_event_id` (optional string): Reference message row ID.
  - `conversation_name` (string): Target contact name.
  - `message_text` (string): Outgoing reply draft content.

#### 11. `open_whatsapp_share_draft`
- **Purpose**: Open WhatsApp with prepared draft text, forcing the user to manually choose the target recipient chat window inside WhatsApp.
- **Parameters**:
  - `message_text` (string): Prefilled message content.
- **Safety Gate**: Demands physical confirmation tap as it moves prepared text out of the app sandbox.

#### 12. `open_whatsapp_click_to_chat`
- **Purpose**: Open a specific WhatsApp chat window with prepared message text. This strictly requires a verified E.164 phone format and cannot guess based on name parameters.
- **Parameters**:
  - `phone_number_e164` (string): Verified target phone number.
  - `message_text` (string): Prefilled message content.
- **Safety Gate**: Demands physical confirmation tap. Only allowed if number is verified or explicitly provided in the command.

---

### Message Execution Tool (Strict Manual Confirmation Required)

#### 13. `send_reply_to_active_whatsapp_notification`
- **Purpose**: Send a reply through a live, active WhatsApp notification using Android's `RemoteInput` API.
- **Parameters**:
  - `notification_key` (string): The active system notification key tag.
  - `message_text` (string): Raw response body text.
- **Safety Gate & Verification Lifecycle**:
  1. The tool choice is returned as an execution proposal.
  2. The UI pops up a blocking confirmation sheet containing recipient name, message text, and safety warnings.
  3. User physically taps "Confirm Send".
  4. The system queries active notification objects via `NotificationListenerService.getActiveNotifications(arrayOf(notificationKey))`.
  5. The service validates that the notification is active, from WhatsApp, and contains valid `RemoteInputs`.
  6. The `PendingIntent` is fired to commit the direct reply inline.
  7. If expired, fails safely and prompts the user to open click-to-chat or share chooser drafts.

---

### Privacy Tools (Confirmation Required)

#### 14. `pause_whatsapp_capture`
- **Purpose**: Set the capture toggle status to "OFF", stopping the notification listener service from parsing and caching new messages.

#### 15. `resume_whatsapp_capture`
- **Purpose**: Set the capture toggle status to "ON", resuming notification intercepts.

#### 16. `delete_local_whatsapp_data`
- **Purpose**: Wipe local database entries to enforce absolute privacy.
- **Parameters**:
  - `delete_all` (boolean): Wipe the entire Room database.
  - `conversation_name` (optional string): Limit deletion to messages and follow-ups of a specific contact.

---

## 2. Interaction Examples Mapping (English-Only)

All inputs parsed by FunctionGemma are strictly English commands:

- **Command**: *"Reply to the latest message from Mom saying I am coming home now."*
  → Proposes: `send_reply_to_active_whatsapp_notification` with extracted parameters.
- **Command**: *"Remind me tomorrow at 9 AM to check Rahul's project update."*
  → Proposes: `schedule_reminder_for_message` resolving time values to tomorrow 09:00.
- **Command**: *"Mark the message about deadline as high priority."*
  → Proposes: `mark_message_priority` targeting the matching event ID.
- **Command**: *"Show all pending tasks."*
  → Proposes: `list_pending_follow_ups`.
