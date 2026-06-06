# AI Tool Routing Notes

GemmaControl follows the Google AI Edge Gallery mobile-actions pattern at the architecture boundary, but keeps execution stricter for WhatsApp safety.

## Gallery Pattern Referenced

Gallery's Mobile Actions task uses:
- a typed action model (`Action`, `FunctionCallDetails`)
- a `ToolSet` with annotated tool functions
- a task/viewmodel boundary that records function calls separately from UI rendering
- model prompts that include current date/time context

Current LiteRT-LM Kotlin docs support native tool execution through annotated `ToolSet` callbacks. The app now uses that Gallery-style path with `automaticToolCalling = true`, but keeps the native tool surface side-effect free. Tool callbacks capture typed proposals/results only; WhatsApp sends, data deletion, capture changes, and other high-risk actions still remain Kotlin/user-confirmed actions.

## GemmaControl Adaptation

- The app now has a two-layer typed local tool contract in `ai/tools`:
- `WhatsAppTools` is the active native LiteRT-LM `ToolSet`. It exposes side-effect-free callbacks for latest reads, unread-chat listing, message reads/summaries/search, named-chat reads, drafts, active notification replies, follow-up creation, priority marking, and capture pause proposals.
- `WhatsAppToolRegistry` mirrors all 16 documented app-level WhatsApp actions used by the parser, safety router, deterministic fallback path, UI confirmations, and local executor.
- `ToolSchemaExporter` exports each registry entry as LiteRT/OpenAPI-style JSON (`name`, `description`, `parameters`, `required`) so the same registry can feed a future `OpenApiTool`/`ToolProvider` adapter without duplicating schemas.
- `WhatsAppToolAction` and `WhatsAppToolActionHandler` provide the Gallery-style callback boundary for high-level model actions. They normalize typed callback arguments, return model-safe success/error maps, and pass typed proposals into Kotlin validation.
- `WhatsAppTools` delegates behavior to the dependency-free handler so JVM tests do not load LiteRT-LM classes directly.
- `ToolCallParser` accepts FunctionGemma-style JSON proposals and Gallery-style `functionCall` envelopes.
- `ToolProposal` carries typed parameters and the safety level into UI/controller code.
- `ToolSafetyRouter` converts a parsed proposal into an execution decision: read-only local execution, local data write, standard user confirmation, external app open confirmation, strict send confirmation, strict delete confirmation, or rejection.
- `WhatsAppLocalToolExecutor` executes only operations that already have Kotlin repository, preference, or Android intent boundaries, currently capture pause/resume, recent local message reads, local message search/details, actionable inbox reads, follow-up create/list/complete, message priority updates, encrypted reminder scheduling, user-confirmed WhatsApp draft/share/click-to-chat preparation, plus all-data or conversation-scoped local deletion. Active notification replies are intentionally rejected here and must use the dedicated notification reply executor.
- Local WhatsApp message output is content-kind aware. Media placeholders are rendered as attachments whose contents were not inspected, hidden notifications are rendered as unavailable, and system notifications are filtered before canonical storage.
- `AssistantPlanner` is the shared voice/typed command entry point. It converts text input into an explicit plan: deterministic read command, latest-reply command, named-reply command, deterministic local-tool command, FunctionGemma proposal request with a clarification fallback, or immediate clarification for incomplete commands.
- `VoiceReplyTargetResolver` prevents silent wrong-chat replies. Generic replies use one active target, ask for clarification when multiple chats are active, allow explicit "latest" replies to pick the newest target, and route named replies to a matching active notification or a safe draft confirmation.
- Deterministic voice reads are adaptive: "read latest WhatsApp messages" is active-notification scoped and does not fall back to old stored rows, while explicit "read stored messages" uses the encrypted local history index. 1-3 selected messages are read directly, larger sets are summarized by chat, and follow-up phrases such as "continue", "read more", "read messages from Mom", "summarize messages from Mom", "summarize WhatsApp", and "only important" stay on the local TTS path.
- Deterministic local-tool commands cover common no-model workflows: "search WhatsApp for payment", "find messages from Mom about dinner", "search last 30 minutes for invoice", "show pending follow ups", "show pending important items", "mark message-1 important", "create follow up for message-1: Call back", and "remind me about message-1 at 2026-06-06T09:00:00+05:30". These commands map into the same proposal/safety/confirmation path as FunctionGemma output.
- `VoiceCommandToolProposalMapper` bridges today's deterministic voice parser into the same proposal path used by future FunctionGemma output.
- `FunctionGemmaVoiceProposalHandler` maps validated FunctionGemma proposal results back into existing voice UI states. Read-latest active notification reads, stored/chat-specific reads and summaries, local message search/details, actionable inbox reads, active-notification replies, message-preparation drafts, follow-up actions, reminders, priority updates, pause/resume capture, and local deletion are wired; unsupported or stale proposals become safe clarifications or failures.
- Model-proposed local mutations still require a Compose confirmation card before `WhatsAppLocalToolExecutor` runs.
- The confirmation UI exposes a Gallery-style function-call details panel before execution: tool name, safety label, Kotlin local boundary text, and sorted bounded arguments. This makes the model proposal visible without giving the model an execution path.
- `PhoneContextSnapshotBuilder` and `GemmaPromptBuilder` create bounded prompts with compact phone context: active WhatsApp notifications, unread chat summaries, and relevant stored messages sorted by recency. Context carries content kind, priority, reply availability, active reply target, and bounded snippets instead of raw notification dumps.
- `docs/FUNCTION_GEMMA_ROUTING_BENCHMARK.md` records the current offline routing corpus and baseline. This benchmark does not execute or download a model; it verifies enriched tool descriptions and Kotlin fallback behavior before native tool expansion.
- `docs/FUNCTION_GEMMA_FINE_TUNING_DECISION.md` records the #116 decision: no FunctionGemma fine-tuning now. The synthetic JSONL template exists only as a future benchmark/training contract and must not contain private real WhatsApp text.
- `GemmaModelManager` centralizes FunctionGemma lifecycle state, duplicate-initialization protection, streaming partial text emission, stop-response cancellation, idle background release, and low-memory cleanup.
- `GemmaEngine` defines the runtime contract. `LiteRtGemmaEngine` now contains the isolated Android LiteRT-LM engine/conversation wrapper, while `UnavailableGemmaEngine` remains available for explicit blocked states when no model path/runtime is configured.
- `FunctionGemmaModelCatalog` mirrors the Gallery MobileActions allowlist entry for `mobile_actions_q8_ekv1024.litertlm`: CPU backend, `topK=64`, `topP=0.95`, `temperature=0.0`, and `maxTokens=1024`.
- `FunctionGemmaModelResolver` checks the app-private `filesDir/models/` install location and produces a `GemmaEngineConfig` only when the verified `.litertlm` file is present.
- `FunctionGemmaModelCard` in Settings keeps the model lifecycle explicit: missing/installed/downloading/failed/canceled states, WorkManager cancel support, progress/rate/ETA display, and collapsed manual URL/SHA-256 inputs. The catalog can derive the official Hugging Face resolve URL, but the app still requires the user-provided SHA-256 before any download.
- `GemmaModelManager` creates engines lazily at initialization time, preventing the LiteRT Java-21 classes from loading during JVM tests or before a model path exists.

## Safety Boundary

FunctionGemma is still treated as a proposal engine from the product perspective. Native LiteRT callbacks can return model-visible maps, but Kotlin must validate:
- supported tool name
- required parameters
- parameter types
- reply text length and non-blank content
- E.164 phone numbers for click-to-chat
- whether a tool requires manual confirmation
- bounded compact phone-context size before any model call
- content-kind truthfulness before any read/summarize output; media notification placeholders are not image/video/audio bytes
- model lifecycle readiness before prompt submission
- installed model resolution before LiteRT initialization
- active notification liveness before a model-proposed reply reaches confirmation
- user confirmation before model-proposed capture pause/resume or local data deletion
- user review of model-proposed function-call details before any confirmation button
- model release on memory pressure
- final execution decision before any local repository write or Android system action

Do not pretend model binaries are loaded until a verified `.litertlm` model exists in the app-private model path and physical device runtime behavior is validated.

## Local Artifact Boundary

The repository should contain Kotlin sources, tests, docs, and metadata only. Do not stage or commit raw Gradle logs, APK/AAB outputs, `.litertlm` or `.tflite` model binaries, screenshots containing private WhatsApp data, credentials, tokens, or SHA values that have not been verified against the downloaded model artifact.

Local JVM unit tests run on Java 17. The current LiteRT-LM ToolSet-capable artifacts are Java 21 class files, so tests cover the handler boundary while `assembleDebug` verifies that the annotated adapter compiles and dexes for Android.
