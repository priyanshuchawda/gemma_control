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
- `WhatsAppLocalToolExecutor` executes only confirmed local-safe operations that already have Kotlin repository boundaries, currently capture pause/resume and full local data deletion. Active notification replies are intentionally rejected here and must use the dedicated notification reply executor.
- `VoiceCommandToolProposalMapper` bridges today's deterministic voice parser into the same proposal path used by future FunctionGemma output.
- `GemmaPromptBuilder` creates bounded prompts with only selected local WhatsApp context, sorted by recency and truncated per message.
- `GemmaModelManager` centralizes FunctionGemma lifecycle state, duplicate-initialization protection, release, and low-memory cleanup.
- `GemmaEngine` defines the runtime contract. `LiteRtGemmaEngine` now contains the isolated Android LiteRT-LM engine/conversation wrapper, while `UnavailableGemmaEngine` remains available for explicit blocked states when no model path/runtime is configured.

## Safety Boundary

FunctionGemma is a proposal engine only. Kotlin must validate:
- supported tool name
- required parameters
- parameter types
- reply text length and non-blank content
- E.164 phone numbers for click-to-chat
- whether a tool requires manual confirmation
- bounded prompt context size before any future model call
- model lifecycle readiness before prompt submission
- model release on memory pressure
- final execution decision before any local repository write or Android system action

Do not pretend model binaries are loaded until a `.litertlm` model path is configured and physical device runtime behavior is verified.

Local JVM unit tests run on Java 17. The current LiteRT-LM ToolSet-capable artifacts are Java 21 class files, so tests cover the handler boundary while `assembleDebug` verifies that the annotated adapter compiles and dexes for Android.
