# Offline Model Strategy Decision

Snapshot date: 2026-06-05

Related issues: #109, #112, #113, #117, #120

Detailed #117 summarization decision: [LOCAL_SUMMARIZATION_MODEL_DECISION.md](LOCAL_SUMMARIZATION_MODEL_DECISION.md)

## Decision

Keep the V1 runtime on **FunctionGemma-only generation/routing** plus deterministic Kotlin logic. Do not add, download, or bundle a second model yet.

The next model to evaluate, after #120 device metrics and #113 design work, is **EmbeddingGemma** for semantic retrieval over locally stored notification rows. #117 keeps a second generative model, such as Gemma 4 E2B or Gemma 3n E2B, out of V1 unless measured summary/rewrite quality is still not good enough after deterministic summaries, FunctionGemma routing, and semantic retrieval.

## Why

FunctionGemma fits the current app because the app has a defined local tool surface and needs private, low-latency natural-language-to-tool routing. Official docs describe FunctionGemma as a Gemma 3 270M model tuned for function calling and local agents that translate user language into executable API actions:

- FunctionGemma overview: https://ai.google.dev/gemma/docs/functiongemma
- Formatting and limitations: https://ai.google.dev/gemma/docs/functiongemma/formatting-and-best-practices

The same official docs say FunctionGemma is trained for single-turn and parallel function calls, not long multi-turn slot filling or multi-step chained workflows out of the box. That means GemmaControl should keep clarification state, workflow state, and execution in Kotlin instead of expecting FunctionGemma to behave like a full assistant brain.

This matches the current implementation:

- deterministic parser paths cover common read/search/follow-up/reply commands without model dependency
- `PhoneContextSnapshotBuilder` gives FunctionGemma bounded truthful phone context
- `WhatsAppToolRegistry` defines the tool surface
- `ToolCallParser`, `ToolSafetyRouter`, and UI confirmations keep execution under Kotlin control
- no model is allowed to send WhatsApp messages, delete data, change capture state, or invent media contents

## Device Constraint

The target Xiaomi/Redmi `2406ERN9CI` has enough storage for experiments, but it is not a high-end model-hosting baseline:

- physical RAM captured: `5,531,208 kB`
- available RAM captured: `1,690,236 kB`
- installed FunctionGemma model: about `276M`
- LiteRT/XNNPACK cache for that model: about `260M`
- total app-private data at device snapshot: about `537M`

Source: [DEVICE_INFO.md](DEVICE_INFO.md)

This is enough for FunctionGemma routing and likely enough to evaluate a quantized embedding model. It is not enough evidence to justify keeping a second generative model resident or adding a larger model cache before #120 measures cold load, warm latency, native memory, battery, and thermal behavior.

## Candidate Architecture Options

| Option | Decision | Reason |
| :--- | :--- | :--- |
| FunctionGemma only | Use now | Best current fit for local tool routing; smallest generation path; already installed and integrated. |
| FunctionGemma + EmbeddingGemma | Evaluate next | Solves semantic retrieval without making prompts huge or adding a second generative responder. |
| FunctionGemma + second generative model | Defer | Could improve summary/rewrite/audio/image quality, but memory/load/thermal cost is too high without evidence. |

## EmbeddingGemma Gate

Official docs describe EmbeddingGemma as a 308M multilingual text embedding model for retrieval, semantic similarity, classification, and clustering. It supports flexible output dimensions from 768 down to 128, a 2K token context, quantized memory under 200MB, and offline local generation:

- EmbeddingGemma overview: https://ai.google.dev/gemma/docs/embeddinggemma
- EmbeddingGemma model card: https://ai.google.dev/gemma/docs/embeddinggemma/model_card

Approve an EmbeddingGemma spike only when all are true:

1. #120 records current FunctionGemma load, routing latency, memory, battery, and thermal numbers.
2. Keyword/local filters from #108 are insufficient for semantic queries such as "the payment message", "what did Mom ask me to do", or "anything urgent from office".
3. The first index design uses `128d` vectors, exact cosine search, local-only storage, and purge linkage to stored message deletion.
4. Embeddings are treated as sensitive local data because they encode private message meaning.
5. No raw message text, vector values, sender names, or group names are logged.

## Second Generative Model Gate

Gemma 4 and Gemma 3n are attractive but not approved for V1.

Official Gemma 4 docs describe E2B/E4B as on-device oriented, multimodal, long-context models with native function-calling support. The Gemma 4 model card lists E2B as 2.3B effective parameters and 5.1B total parameters with embeddings, with 128K context and text/image/audio support:

- Gemma 4 model card: https://ai.google.dev/gemma/docs/core/model_card_4

Official Gemma 3n docs describe an everyday-device multimodal family with audio, visual, and text input, PLE caching, MatFormer selective activation, conditional parameter loading, 32K context, and an E2B effective memory load just under 2B parameters when using the parameter-efficient techniques:

- Gemma 3n overview: https://ai.google.dev/gemma/docs/gemma-3n

Approve a second generative model only when all are true:

1. #120 shows enough device headroom after FunctionGemma and normal app usage.
2. Deterministic Kotlin summaries and read-aloud behavior fail documented quality targets.
3. EmbeddingGemma retrieval does not solve the missing semantic context.
4. The model has an Android runtime path that does not force duplicate model imports or persistent duplicate initialization.
5. The app can release one model before loading another, or otherwise prove memory safety.
6. The user explicitly approves the model artifact download/import.

Until then, Gemma 4/Gemma 3n remain evaluation candidates, not implementation dependencies.

## Benchmark Prompt Suite

The #109 benchmark prompt set should be run against the current deterministic parser plus FunctionGemma routing corpus before any model decision:

| Category | Prompt examples | Expected route |
| :--- | :--- | :--- |
| Single chat read | "Read messages from Mom", "What did Peter say?" | local recent/chat read or model proposal into `list_recent_whatsapp_messages` |
| Multiple chats | "What did I miss?", "Any unread WhatsApp chats?" | concise grouped summary or `list_recent_whatsapp_messages` |
| Media placeholder | "What image did they send?", "Describe the photo" | truthful media-placeholder response; no invented image contents |
| Hidden content | "Read the hidden message" | refusal/clarification that content is unavailable |
| Semantic search | "Find the payment message", "What address did he send?" | current keyword search first; EmbeddingGemma candidate if misses are frequent |
| Ambiguous reply | "Reply ok", "Tell Mom I am late" with multiple active chats | clarification, named active reply, or safe draft confirmation |
| Follow-up workflows | "Create follow up for message-1: Call back", "Show pending follow ups" | local tool confirmation/list result |
| Summary quality | "Summarize WhatsApp", "Summarize office messages" | deterministic summary first; second generative model candidate only if quality target fails |

## Metrics

Record these before changing architecture:

- routing accuracy by category
- malformed or unsupported tool-call rate
- clarification quality for ambiguous replies
- summary usefulness score from manual review
- p50/p95 cold and warm tool-routing latency
- model load time
- app PSS/RSS and native heap before/after model load
- app-private model/cache storage size
- battery level and thermal status before/after benchmark
- whether any prompt or log contains private names/message text

## Current Recommendation

For the next implementation issues:

1. Finish #111/#120 style benchmark capture before adding model code.
2. Use [EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md](EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md) and the #113 scaffold to design an EmbeddingGemma semantic index, but do not download the model yet.
3. Use [LOCAL_SUMMARIZATION_MODEL_DECISION.md](LOCAL_SUMMARIZATION_MODEL_DECISION.md) as the #117 gate: no Gemma 4/Gemma 3n summarizer for V1; later on-device evaluation only after explicit approval.
4. Keep improving Kotlin summaries, deterministic command parsing, and FunctionGemma tool descriptions first.

This keeps the app useful on the current phone now, while preserving a clear path to semantic retrieval and richer summaries when evidence justifies the extra model cost.
