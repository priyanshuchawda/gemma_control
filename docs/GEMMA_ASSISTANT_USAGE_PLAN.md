# GemmaControl Gemma Usage Plan

This document maps Gemma and LiteRT-LM capabilities into the GemmaControl Android assistant roadmap.

Snapshot date: 2026-06-05

Primary device reference: [DEVICE_INFO.md](DEVICE_INFO.md)

Model reference: [GEMMA_MODEL_REFERENCE.md](GEMMA_MODEL_REFERENCE.md)

Offline model decision: [OFFLINE_MODEL_STRATEGY_DECISION.md](OFFLINE_MODEL_STRATEGY_DECISION.md)

Embedding memory plan: [EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md](EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md)

Local summarization decision: [LOCAL_SUMMARIZATION_MODEL_DECISION.md](LOCAL_SUMMARIZATION_MODEL_DECISION.md)

Accessibility decision: [ACCESSIBILITY_SERVICE_EVALUATION.md](ACCESSIBILITY_SERVICE_EVALUATION.md)

FunctionGemma fine-tuning decision: [FUNCTION_GEMMA_FINE_TUNING_DECISION.md](FUNCTION_GEMMA_FINE_TUNING_DECISION.md)

Media understanding boundary: [MEDIA_UNDERSTANDING_BOUNDARY.md](MEDIA_UNDERSTANDING_BOUNDARY.md)

## #112 Roadmap Status

The #112 P1 model architecture roadmap is now defined for the current device:

| Area | Status | Decision |
| :--- | :--- | :--- |
| FunctionGemma routing | Complete through #114/#115 | Keep FunctionGemma as the only required generative/router model for V1. Kotlin owns state, safety, and execution. |
| Runtime/device baseline | Complete locally through #120 | Use the Settings benchmark dashboard and physical device matrix before approving any new model. |
| Semantic retrieval | Designed/prototyped through #113 | EmbeddingGemma remains optional and requires explicit model approval before download/import. |
| Larger summarizer | Decided through #117 | No Gemma 4/Gemma 3n summarizer in V1. Revisit only after benchmark evidence and user approval. |
| Accessibility expansion | Decided through #122 | No Accessibility service in V1. Possible V2 assistive mode only after separate policy/safety review. |
| Fine-tuning | Complete through #116 | Do not fine-tune now; use the synthetic dataset template only if routing benchmarks later prove prompt/context tuning is insufficient. |
| Media understanding | Complete through #118 | Notification placeholders stay placeholder-only; future analysis requires user-selected media or a scoped URI and explicit model approval. |
| Safety gates | P2 follow-up #119 | Keep open for deterministic-vs-ShieldGemma policy evaluation; it does not block the V1 WhatsApp notification assistant. |

This closes the P1 architecture definition while preserving the remaining P2 research issues.

## Product Goal

GemmaControl should make one Android phone easier to use through natural voice/text commands while keeping private data local.

The near-term target is not a generic all-powerful assistant. The practical target is:

```text
Reliable WhatsApp notification assistant
 -> truthful capture
 -> adaptive reading
 -> safe reply
 -> search/follow-up
 -> semantic retrieval
 -> optional broader phone control after capability review
```

## Required Architecture

```text
Android capability layer
  - Notification listener
  - Microphone and speech recognizer
  - TTS
  - App intents
  - Future: contacts, calendar, media picker, accessibility

Data truth layer
  - Captured WhatsApp notifications
  - Stored encrypted message rows
  - Active notification references
  - Reply action availability
  - Hidden content and media placeholders

Context layer
  - PhoneContextSnapshot
  - Compact prompt context
  - Recency selector
  - Future semantic retriever

Model layer
  - FunctionGemma for tool routing
  - Future EmbeddingGemma for retrieval
  - Future Gemma 4 / Gemma 3n for summarization only if needed

Safety and execution layer
  - Tool parser
  - Tool safety router
  - Confirmation UI
  - Kotlin executors
  - Android APIs/intents/notification replies

Result layer
  - TTS
  - UI result cards
  - Clarification prompts
  - Privacy-safe logs and benchmarks
```

## Current Device Assumption

The connected phone is Xiaomi/Redmi `2406ERN9CI`, Android 16/API 36, Qualcomm `SM4450`, about `5.5GB` physical RAM, Adreno 613, with FunctionGemma already installed under app-private storage.

That makes the model order:

1. FunctionGemma only.
2. Benchmark current model/runtime.
3. Add structured phone context.
4. Evaluate EmbeddingGemma.
5. Evaluate bigger models only if needed.

## Phase 0: Baseline And Capability Truth

Issues:

- #120 Create model/runtime performance dashboard.
- #123 Define permission and capability matrix.
- #121 Harden Xiaomi/HyperOS notification reliability.

### Purpose

Before adding models or new features, capture what the current phone and app can do.

### Data to capture

- Device model, OS, API, security patch.
- RAM available before/after model load.
- App PSS/RSS before/after model load.
- FunctionGemma load time.
- First tool proposal latency.
- Warm proposal latency.
- Battery level, charging state, thermal status.
- Model file size and runtime cache size.
- Notification listener enabled/live state.
- Microphone, notification, and future permission states.

### Output

The app should produce a privacy-safe benchmark report. No WhatsApp body text, names, phone numbers, or notification contents should be logged.

### Why this comes first

The phone can run FunctionGemma now, but bigger model feasibility is unknown. Model selection must be based on device evidence.

## Phase 1: Truthful WhatsApp Context

Issues:

- #102 Classify WhatsApp notification content.
- #114 Design compact phone context builder.
- #115 Benchmark FunctionGemma routing.

### Notification classification

Classify every captured WhatsApp event into facts:

- App package.
- Direct chat vs group.
- Conversation display name.
- Sender display name when available.
- Message count.
- Text message vs media placeholder.
- Hidden/redacted content.
- System/status notification.
- Reply action available.
- Active notification key.
- Timestamp.
- Source parse path.

### Compact context snapshot

Create a typed `PhoneContextSnapshot`:

```kotlin
data class PhoneContextSnapshot(
    val generatedAtMs: Long,
    val notificationListenerEnabled: Boolean,
    val microphoneGranted: Boolean,
    val activeWhatsAppNotifications: List<ActiveWhatsAppNotificationFact>,
    val recentStoredMessages: List<StoredMessageFact>,
    val activeReplyTargets: List<ReplyTargetFact>,
    val capabilityWarnings: List<CapabilityWarning>
)
```

Example prompt facts:

```text
Active WhatsApp notifications:
1. chat="Mom", type=direct, unread=2, latest_text="Call me when free", reply_available=true
2. chat="Office Group", type=group, unread=8, latest_media=photo, reply_available=true

Stored relevant messages:
- Office Group: 3 recent messages about tomorrow meeting, latest 10 minutes ago

Warnings:
- Accessibility is not enabled; full WhatsApp chat screen control is unavailable.
```

### FunctionGemma prompt requirements

Prompt must be:

- Bounded.
- Sorted by recency/relevance.
- Explicit about current capabilities.
- Explicit about hidden/media placeholders.
- Explicit that Kotlin executes tools.

FunctionGemma should never receive a raw notification dump.

## Phase 2: Tool Routing And Execution

Issues:

- #104 Unified assistant planner pipeline.
- #105 Expand FunctionGemma native tool surface.
- #106 Tool safety and permission UX.
- #115 Routing benchmark.

### Tool surface

The current registry already supports local WhatsApp tools including:

- List recent captured messages.
- Search captured messages.
- Get message details.
- Get actionable inbox.
- Create/list/complete follow-ups.
- Schedule reminders.
- Mark message priority.
- Draft WhatsApp replies.
- Open WhatsApp share/click-to-chat drafts.
- Send reply to active notification.
- Pause/resume capture.
- Delete local WhatsApp data.

### Tool safety levels

Keep these execution boundaries:

| Safety level | Meaning | Examples |
| :--- | :--- | :--- |
| Read-only | Can run locally without mutation | list/search/get details |
| Local write | Mutates local app data | follow-up, reminder, priority |
| Confirmation required | Opens app, drafts, changes capture, deletes data | share draft, pause capture, delete |
| Strict manual confirmation | Sends external message through live notification | active WhatsApp reply |
| Rejected | Impossible/unsafe/stale | no active reply target, hidden content claims |

### Planner behavior

All voice and typed commands should flow through one planner:

```text
transcript or typed command
 -> deterministic shortcut if exact/simple
 -> compact context
 -> FunctionGemma proposal if needed
 -> parse and safety route
 -> UI clarification/confirmation/execution
```

The deterministic shortcut is allowed for common commands, but it should still map into the same typed proposal/execution path.

## Phase 3: Adaptive WhatsApp Reading And Replying

Issues:

- #103 Adaptive read-aloud.
- #107 Reply workflow.
- #108 Search/follow-up workflows.

### Reading behavior

Replace fixed "read 3 messages" behavior with adaptive behavior:

| Situation | Assistant response |
| :--- | :--- |
| No messages | Say no captured unread/recent WhatsApp messages are available. |
| One message | Read it directly. |
| Few messages | Read concise list. |
| Many messages, one chat | Summarize count and ask whether to continue/read all. |
| Many messages, multiple chats | List chats/counts and ask which chat to read. |
| Media placeholder | Say "photo/sticker/document received", not image contents. |
| Hidden content | Say content is hidden/redacted and cannot be read. |

### Reply behavior

Reply flow:

```text
User: "Reply to Mom that I am in a meeting"
 -> resolve target from active notification or stored context
 -> if exactly one live reply action exists, show confirmation
 -> if multiple possible targets, ask which chat
 -> if no live reply action, offer draft/share/click-to-chat alternatives
```

Never claim a reply was sent until Android/Kotlin reports success.

## Phase 4: EmbeddingGemma Semantic Memory

Issue:

- #113 Evaluate EmbeddingGemma semantic memory.

Detailed spike: [EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md](EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md)

### Why add it

Keyword search is not enough for natural phone commands. Users ask:

- "What was that payment message?"
- "Anything important from office?"
- "Find the address he sent."
- "What did Mom say about the appointment?"

EmbeddingGemma helps retrieve relevant local rows without sending all message history to FunctionGemma.

### Proposed local pipeline

```text
On message capture:
  decrypted message text in memory
  -> normalize short text
  -> EmbeddingGemma document embedding
  -> store vector + message id locally

On query:
  user command
  -> EmbeddingGemma query embedding
  -> vector search top-k
  -> retrieve message facts
  -> compact context
  -> FunctionGemma route/read/summarize tool
```

### Storage choices

Start simple:

- `MessageEmbeddingEntity(messageEventId, vectorDim, encryptedVectorBytes, modelId, promptVersion, createdAt)`
- `ConversationEmbeddingSummaryEntity(conversationId, summaryWindow, vectorBytes, modelId)`

Start with:

- `128d` vectors.
- Exact cosine similarity if row count is small.
- Upgrade to `256d` only if retrieval quality needs it.
- Approximate index only if exact search becomes slow.

### Privacy

Embeddings are sensitive because they encode private message meaning.

Required:

- Store locally only.
- Treat as protected data.
- Encrypt persisted vector bytes or protect them under the same local-sensitive-data policy as message bodies.
- Delete embeddings when message rows are deleted.
- Include embeddings in local data purge.
- Do not log query vectors or retrieved private message text.

## Phase 5: Larger Model Evaluation

Issues:

- #109 Offline model strategy.
- #117 Gemma 4 or Gemma 3n optional summarization.

Decision note: [LOCAL_SUMMARIZATION_MODEL_DECISION.md](LOCAL_SUMMARIZATION_MODEL_DECISION.md)

### When larger model is justified

For V1, do not add Gemma 4 or Gemma 3n. Only reconsider Gemma 4 E2B or Gemma 3n E2B later if all are true:

- #120 shows enough memory/thermal/load-time headroom.
- #103 summaries are too weak with deterministic Kotlin summaries.
- #113 retrieval works but still needs better natural language synthesis.
- User approves model download/storage cost.

### Preferred role

Use a bigger model only for:

- Summarization.
- Rephrasing reply drafts.
- Multimodal interpretation after actual media access exists.

Do not use it for direct action execution.

### Candidate comparison

| Candidate | Pros | Risks | Current status |
| :--- | :--- | :--- | :--- |
| Gemma 4 E2B | Newer, reasoning, native function calling, multimodal/audio support | Memory/load cost on 5.5GB device; runtime artifact availability must be checked | Evaluation only |
| Gemma 3n E2B | Designed for everyday devices; audio/vision/text; PLE and parameter skipping | Still much heavier than FunctionGemma; integration complexity | Evaluation only |
| Gemma 4 E4B or larger | Better capability | Too heavy for this phone as a daily assistant baseline | Do not prioritize |

## Phase 6: Media And Accessibility

Issues:

- #118 Media understanding.
- #119 ShieldGemma-style safety.
- #122 Accessibility evaluation.

Accessibility decision note: [ACCESSIBILITY_SERVICE_EVALUATION.md](ACCESSIBILITY_SERVICE_EVALUATION.md)

Media decision note: [MEDIA_UNDERSTANDING_BOUNDARY.md](MEDIA_UNDERSTANDING_BOUNDARY.md)

### Media

Notification placeholders are not media bytes. If WhatsApp says "Photo", the assistant can only say a photo was received.

Future media analysis requires:

- User-selected image/file through Android Photo Picker, or
- A scoped/persisted content URI grant, or
- Another approved Android access path.

Only then evaluate:

- Gemma 4/Gemma 3n image understanding.
- PaliGemma.
- ShieldGemma safety gate.

### Accessibility

Accessibility can unlock broader phone control, but it is sensitive and brittle.

Potential capabilities:

- Read visible screen text.
- Tap buttons.
- Type into focused fields.
- Navigate app screens.
- Open chats manually.

Risks:

- Permission is sensitive.
- Xiaomi/HyperOS may restrict persistence.
- UI automation can break across app versions.
- Sending/deleting/changing settings needs strict confirmations.

Current decision:

- No Accessibility service in V1.
- Keep #122 as a V2-only assistive-mode evaluation.
- Do not mix Accessibility into the WhatsApp notification assistant v1.
- Never use Accessibility for silent sends, destructive actions, permission toggles, or restricted-settings bypass.

## Implementation Modules

### Current modules

| Module | Current role |
| :--- | :--- |
| `FunctionGemmaModelCatalog` | Allowlisted model metadata. |
| `ModelDownloadManager` / `ModelDownloadWorker` | Verified model download lifecycle. |
| `FunctionGemmaModelResolver` | Resolves app-private installed model path. |
| `GemmaModelManager` | Engine lifecycle, streaming state, cancellation, release. |
| `LiteRtGemmaEngine` | LiteRT-LM engine and conversation wrapper. |
| `GemmaPromptBuilder` | Bounded prompt with selected WhatsApp context. |
| `WhatsAppToolRegistry` | Typed tool definitions and safety labels. |
| `WhatsAppTools` | LiteRT-LM `ToolSet` adapter. |
| `ToolCallParser` | Parses text/envelope proposals. |
| `ToolSafetyRouter` | Converts proposal into execution decision. |
| `WhatsAppLocalToolExecutor` | Executes safe local app operations. |
| `ActiveNotificationReplyExecutor` | Executes live notification replies. |

### New modules by issue

| Issue | Module | Purpose |
| :--- | :--- | :--- |
| #120 | `ModelRuntimeBenchmarkRunner`, `AndroidRuntimeBenchmarkSnapshotProvider`, `ModelRuntimeBenchmarkCard` | Capture model/device benchmark metrics without downloading another model. |
| #123 | `AssistantCapabilityMatrix` | Map tools to permissions and setup state. |
| #121 | `NotificationListenerHealthMonitor` | Surface listener enabled/live/recent-event state. |
| #102 | `WhatsAppNotificationClassifier` | Classify text/media/hidden/system/group/reply facts. |
| #114 | `PhoneContextSnapshot` | Typed compact context model. |
| #114 | `PhoneContextBuilder` | Builds context from notifications, storage, permissions. |
| #115 | `FunctionGemmaRoutingBenchmark` | Prompt corpus and routing accuracy report. |
| #113 | `EmbeddingGemmaPromptFormatter` | Formats query/document prompts for future EmbeddingGemma retrieval. |
| #113 | `MessageEmbeddingProvider` | Model-agnostic interface so the real embedding runtime can be imported once and reused later. |
| #113 | `ExactMessageEmbeddingIndex` | Exact cosine top-k retrieval scaffold for the first no-model prototype and later small local indexes. |
| #113 | `MessageEmbeddingStore` | Future sensitive local embedding persistence after explicit model approval. |
| #113 | `SemanticContextSelector` | Combines recency and embedding relevance. |
| #117 | `LocalSummarizationModelDecision` | Documents that V1 stays rules-first and no second generative model is approved. |

## Benchmark Requirements

For every model/runtime decision, measure:

- Model file size.
- Runtime cache size.
- Cold load time.
- Warm load time.
- First proposal latency.
- End-to-end command latency.
- Java heap.
- Native heap/PSS/RSS.
- Battery before/after.
- Thermal before/after.
- Error/crash rate.
- Malformed output rate.
- Routing accuracy.

Benchmark prompts should include:

- "Show my latest WhatsApp messages."
- "Read messages from Mom."
- "Anything urgent from office?"
- "What was that payment message?"
- "Reply to Mom that I am in a meeting."
- "Summarize unread WhatsApp messages."
- "What image did they send?"
- "Delete all local WhatsApp data."

## Final Recommendation

Use this order. Completed P1/P0 architecture items are marked as done; P2 research remains explicit:

```text
done: #120 device/model baseline
done: #123 permission/capability matrix
done: #121 Xiaomi notification reliability
done: #102 notification truth classifier
done: #114 compact phone context
done: #115 FunctionGemma routing benchmark
done: #103 adaptive reading
done: #107 safe reply flow
done: #108 practical search/follow-up
done: #113 EmbeddingGemma semantic memory design/prototype, no model download
done: #109/#117 bigger model decision, no second generative model in V1
done: #122 Accessibility decision, no Accessibility service in V1
done: #116 FunctionGemma fine-tuning evaluation, no training/model import now
done: #118 media understanding boundary, placeholder-only in V1
P2:  #119 ShieldGemma-style safety gate assessment
```

FunctionGemma remains the only required model until measured evidence says otherwise.
