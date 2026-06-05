# Local Summarization Model Decision

Snapshot date: 2026-06-05

Related issues: #117, #109, #112, #113, #120

## Decision

For V1, keep summarization **rules-first and FunctionGemma-assisted only for routing**. Do not add Gemma 4, Gemma 3n, or any second generative model to the app yet.

The current path is:

```text
Captured WhatsApp facts
 -> Kotlin adaptive read/summarize behavior
 -> FunctionGemma only when a natural-language command needs tool routing
 -> optional EmbeddingGemma retrieval after approval
 -> second generative model only if benchmark evidence proves it is needed
```

This keeps the assistant useful on the Redmi 13 5G now while avoiding model-load, storage, battery, and thermal risk before real measurements exist.

## Official Model Facts

Gemma 4 and Gemma 3n are strong candidates for future local summarization, but both are much heavier than the current FunctionGemma tool router.

| Candidate | Official facts relevant to GemmaControl | Planning impact |
| :--- | :--- | :--- |
| Gemma 4 E2B | Gemma 4 models are multimodal, support text/image, support audio on E2B/E4B/12B, include long context, thinking mode, and native function calling. The model card lists E2B as `2.3B` effective parameters and `5.1B` total parameters with embeddings, with 128K context and text/image/audio support. | Attractive future summarizer/reasoner, but far larger than FunctionGemma and not justified for V1 without device measurements. |
| Gemma 4 E4B | Same Gemma 4 family benefits with higher parameter count: `4.5B` effective and `8B` total parameters with embeddings. | Too large for the current daily-assistant baseline unless future benchmarks are unexpectedly strong. |
| Gemma 3n E2B | Optimized for everyday devices, supports text, visual, and audio input, has 32K context, PLE caching, MatFormer selective activation, and conditional loading. Official docs describe an E2B effective memory load of `1.91B` parameters with parameter skipping and PLE caching. | More device-oriented than a conventional larger model, but still much heavier than FunctionGemma and should remain a future evaluation candidate. |

Source links:

- Gemma 4 model card: https://ai.google.dev/gemma/docs/core/model_card_4
- Gemma 3n overview: https://ai.google.dev/gemma/docs/gemma-3n
- Gemma 3n model card: https://ai.google.dev/gemma/docs/gemma-3n/model_card

## Current App Baseline

GemmaControl already handles the summary cases that matter for V1 without a second generative model:

- `VoiceReadAloudBuilder` reads one to three messages directly.
- Larger sets are summarized by chat instead of reading everything.
- Multi-chat sets list chat counts and ask for a more specific continuation.
- `continue`, chat-specific reads, summarize mode, and important-only reads are deterministic.
- Media and hidden content use `WhatsAppContentKind` text helpers so the app says it cannot inspect photo/video/audio/document contents from notifications.
- FunctionGemma routes commands into typed tools; Kotlin executes and validates.

This baseline is intentionally conservative. It does not produce rich prose summaries, but it is reliable, private, and truthful.

## Candidate Paths

| Path | V1 decision | Why |
| :--- | :--- | :--- |
| Kotlin deterministic summaries | Use now | Fast, predictable, no model load, covered by JVM tests, truthful for media/hidden notifications. |
| FunctionGemma result text | Use only around tool routing | FunctionGemma is the current local model, but it is optimized for tool calls, not rich long-form summarization. |
| FunctionGemma plus EmbeddingGemma retrieval | Evaluate next after #120/#113 gates | Retrieval solves "which messages matter?" before generation. This is lower risk than adding a second generative model. |
| Gemma 3n E2B summarizer | Defer | Device-oriented and multimodal, but still needs explicit artifact approval, load-time/memory/thermal data, and a single-import lifecycle manager. |
| Gemma 4 E2B summarizer | Defer | Newer reasoning and long context, but total parameters/storage/runtime footprint are too high to assume safe on this phone. |
| Gemma 4 E4B or larger | Reject for current phone baseline | Too heavy for a practical V1 assistant on the Redmi 13 5G. |

## Summary Benchmark Matrix

Before any second model is approved, benchmark these scenarios against the current Kotlin baseline, then compare with any proposed model only after explicit approval.

| ID | Scenario | Input shape | Current expected V1 behavior | Second-model gate |
| :--- | :--- | :--- | :--- | :--- |
| SUM-001 | Few direct messages | One to three text messages from one chat | Read concise message list directly. | No second model needed. |
| SUM-002 | Many messages, one chat | Four or more text messages from one chat | Summarize count/latest items and offer continue/read more. | Consider model only if users cannot act from the deterministic summary. |
| SUM-003 | Many messages, multiple chats | Multiple chats with mixed counts | List chats/counts and ask which chat to read. | Prefer EmbeddingGemma/context selection before a generative summarizer. |
| SUM-004 | Important-only summary | Mixed priority messages | Include only high-priority rows. | No second model needed unless priority classification becomes semantic later. |
| SUM-005 | Media placeholders | Photo/video/sticker/audio/document notifications | State attachment type and that contents were not inspected. | A generative model must not invent media contents; actual media bytes require #118 first. |
| SUM-006 | Hidden/redacted content | Hidden or blank notification content | Say content is hidden/unavailable. | A generative model must not infer hidden content. |
| SUM-007 | Semantic request | "What was that payment message?" with many stored rows | Keyword search first; EmbeddingGemma retrieval is the next model path if misses are frequent. | Do not use Gemma 4/3n until retrieval is evaluated. |
| SUM-008 | Voice-friendly wording | Long deterministic summary is awkward in TTS | Trim, group, and ask continuation. | Consider second model only if TTS quality remains poor after UI/TTS wording improvements. |
| SUM-009 | Reply rephrase | "Tell Mom I am in a meeting, make it polite" | Draft only with confirmation; current V1 can keep user text as-is. | Rephrase model only after reply safety and active-target flow are validated. |
| SUM-010 | Battery/thermal stress | Repeated summarize/read commands | No new model load; use current FunctionGemma lifecycle only when routing is needed. | Reject model if repeated summaries heat the phone, stall UI, or force frequent model reloads. |

## Required Metrics

Record these before changing the architecture:

- Summary usefulness score from manual review.
- TTS length and clarity.
- User correction rate after summaries.
- Malformed or unsafe content rate.
- Model file size and runtime cache size.
- Cold load, warm load, and first-token latency.
- End-to-end summarize latency.
- Java heap, native heap, PSS/RSS before and after load.
- Battery percentage and battery temperature before/after repeated summaries.
- Android thermal status before/after repeated summaries.
- Whether any logs contain private names, message text, group names, phone numbers, prompts, or model outputs.

## Approval Gate

A second generative summarizer can be proposed only when all are true:

1. #120 captures current FunctionGemma runtime numbers on the Redmi 13 5G.
2. #113 retrieval design is complete and any EmbeddingGemma spike has been approved separately.
3. The benchmark matrix above shows deterministic summaries are not good enough for real use.
4. The candidate model has an Android runtime artifact with known storage, cache, load-time, and memory behavior.
5. The app can import/load the model through one shared manager, not per tool call.
6. The app can release FunctionGemma before loading the summarizer, or prove both are safe in memory.
7. The model is used only for summaries/rephrasing, never for direct phone execution.
8. The user explicitly approves the model download/import.

## Future Architecture If Approved

If a second model is later approved, keep the execution boundary:

```text
Stored message facts / retrieved facts
 -> SummarizationModelManager
 -> candidate summary or reply rewrite
 -> Kotlin truth/safety validation
 -> UI/TTS/confirmation
```

The model output must be checked for:

- Claims about unseen media contents.
- Claims about hidden/redacted content.
- Unsupported action execution.
- Private data leakage into logs.
- Reply text that changes user intent.

## Recommendation

Close #117 with this decision: **no second generative model for V1**.

The app should first improve deterministic summaries, local search/follow-ups, the FunctionGemma routing prompt, and optional EmbeddingGemma retrieval. Revisit Gemma 3n E2B or Gemma 4 E2B only after real benchmark data shows the current path is inadequate and the user explicitly approves the model artifact.
