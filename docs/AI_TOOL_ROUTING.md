# AI Tool Routing Notes

GemmaControl follows the Google AI Edge Gallery mobile-actions pattern at the architecture boundary, but keeps execution stricter for WhatsApp safety.

## Gallery Pattern Referenced

Gallery's Mobile Actions task uses:
- a typed action model (`Action`, `FunctionCallDetails`)
- a `ToolSet` with annotated tool functions
- a task/viewmodel boundary that records function calls separately from UI rendering
- model prompts that include current date/time context

Current LiteRT-LM Kotlin docs also support manual tool execution by creating a conversation with `automaticToolCalling = false`. That is the required mode for this app because WhatsApp sends, data deletion, and capture changes must remain Kotlin/user-confirmed actions.

## GemmaControl Adaptation

The app now has a typed local tool contract in `ai/tools`:
- `WhatsAppToolRegistry` mirrors all 16 documented WhatsApp tools.
- `ToolSchemaExporter` exports each registry entry as LiteRT/OpenAPI-style JSON (`name`, `description`, `parameters`, `required`) so the same registry can feed a future `OpenApiTool`/`ToolProvider` adapter without duplicating schemas.
- `WhatsAppToolAction` and `WhatsAppToolActionHandler` provide the Gallery-style callback boundary for high-level model actions: reply to latest notification, read latest notifications, and get notifications from a sender.
- `WhatsAppTools` is the LiteRT-LM `ToolSet` adapter with `@Tool` / `@ToolParam` annotations. It delegates behavior to the dependency-free handler so JVM tests do not load LiteRT-LM classes directly.
- `ToolCallParser` accepts FunctionGemma-style JSON proposals and Gallery-style `functionCall` envelopes.
- `ToolProposal` carries typed parameters and the safety level into UI/controller code.
- `ToolSafetyRouter` converts a parsed proposal into an execution decision: read-only local execution, local data write, user confirmation, strict manual confirmation, or rejection.
- `WhatsAppLocalToolExecutor` executes only operations that already have Kotlin repository, preference, or Android intent boundaries, currently capture pause/resume, recent local message reads, local message search/details, actionable inbox reads, follow-up create/list/complete, message priority updates, encrypted reminder scheduling, user-confirmed WhatsApp draft/share/click-to-chat preparation, plus all-data or conversation-scoped local deletion. Active notification replies are intentionally rejected here and must use the dedicated notification reply executor.
- `VoiceCommandToolProposalMapper` bridges today's deterministic voice parser into the same proposal path used by future FunctionGemma output.
- `FunctionGemmaVoiceProposalHandler` maps validated FunctionGemma proposal results back into existing voice UI states. Read-latest, local message search/details, actionable inbox reads, active-notification replies, message-preparation drafts, follow-up actions, reminders, priority updates, pause/resume capture, and local deletion are wired; unsupported or stale proposals become safe failures.
- Model-proposed local mutations still require a Compose confirmation card before `WhatsAppLocalToolExecutor` runs.
- The confirmation UI exposes a Gallery-style function-call details panel before execution: tool name, safety label, Kotlin local boundary text, and sorted bounded arguments. This makes the model proposal visible without giving the model an execution path.
- `GemmaPromptBuilder` creates bounded prompts with only selected local WhatsApp context, sorted by recency, with both message bodies and the user command truncated before model submission.
- `GemmaModelManager` centralizes FunctionGemma lifecycle state, duplicate-initialization protection, streaming partial text emission, stop-response cancellation, idle background release, and low-memory cleanup.
- `GemmaEngine` defines the runtime contract. `LiteRtGemmaEngine` now contains the isolated Android LiteRT-LM engine/conversation wrapper, while `UnavailableGemmaEngine` remains available for explicit blocked states when no model path/runtime is configured.
- `FunctionGemmaModelCatalog` mirrors the Gallery MobileActions allowlist entry for `mobile_actions_q8_ekv1024.litertlm`: CPU backend, `topK=64`, `topP=0.95`, `temperature=0.0`, and `maxTokens=1024`.
- `FunctionGemmaModelResolver` checks the app-private `filesDir/models/` install location and produces a `GemmaEngineConfig` only when the verified `.litertlm` file is present.
- `FunctionGemmaModelCard` in Settings keeps the model lifecycle explicit: missing/installed/downloading/failed/canceled states, WorkManager cancel support, progress/rate/ETA display, and collapsed manual URL/SHA-256 inputs. The catalog can derive the official Hugging Face resolve URL, but the app still requires the user-provided SHA-256 before any download.
- `GemmaModelManager` creates engines lazily at initialization time, preventing the LiteRT Java-21 classes from loading during JVM tests or before a model path exists.

## Safety Boundary

FunctionGemma is a proposal engine only. Kotlin must validate:
- supported tool name
- required parameters
- parameter types
- reply text length and non-blank content
- E.164 phone numbers for click-to-chat
- whether a tool requires manual confirmation
- bounded prompt context size before any model call
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
