# Gemma Model And Runtime Reference

This document summarizes the Gemma-related model and runtime choices relevant to GemmaControl. It is based on official Gemma and LiteRT-LM documentation plus the current app implementation.

Snapshot date: 2026-06-04

## Decision Summary

GemmaControl should use a layered model architecture:

```text
FunctionGemma 270M
 -> natural language to tool-call routing

Kotlin / Android
 -> context, permissions, execution, safety, confirmations, storage

EmbeddingGemma
 -> optional semantic retrieval and classification over captured local messages

Gemma 4 E2B or Gemma 3n E2B
 -> optional later summarizer / multimodal reasoner only after benchmarks
```

Do not treat one generative model as if it can "access the phone". The app must build truthful Android context, the model can propose a tool, and Kotlin must validate and execute.

## Model Fit Table

| Component | What it is | Best use in GemmaControl | Current decision |
| :--- | :--- | :--- | :--- |
| FunctionGemma | Gemma 3 270M variant specialized for function calling | Route user voice/text commands to typed local tools | Required first model |
| EmbeddingGemma | 300M/308M text embedding model | Semantic search, retrieval, clustering, classification over captured messages | Evaluate after #120/#113 |
| LiteRT-LM | Android/JVM runtime for `.litertlm` LLMs with tools and multimodality support | Load FunctionGemma, create conversations, stream output, expose tool calls | Current runtime |
| Gemma 4 E2B | Small effective-parameter Gemma 4 model | Later summarization/reasoning/audio/image if benchmarks pass | Evaluation only |
| Gemma 3n E2B | Device-optimized multimodal model | Later summarization/audio/image if Gemma 4 path is not practical | Evaluation only |
| PaliGemma | Vision-language model | Image understanding when actual image bytes are available | Not for WhatsApp notification placeholders |
| ShieldGemma 2 | Image safety classifier | Future image/media safety gate | Not needed for v1 |
| FunctionGemma fine-tune | Custom routing model trained on app-specific examples | Improve routing if prompts/tool descriptions are insufficient | Later only |

## FunctionGemma

Official docs describe FunctionGemma as a specialized Gemma 3 270M model tuned for function calling. It is intended to translate natural language into executable API actions for local, private agents.

### What it does

- Takes a user request plus tool definitions/context.
- Produces structured tool calls or tool-call-like output.
- Works well for apps with a defined action surface.
- Is small enough for local-first deployment and low-latency edge agents.

### What it does not do by itself

- It does not read WhatsApp directly.
- It does not safely send messages by itself.
- It does not own Android permissions, notification access, app state, or execution.
- It is not the best model for open-ended chat or rich summarization.

### Relevant limitations

Official FunctionGemma formatting docs say it is explicitly trained for:

- Single-turn tool calls.
- Parallel independent tool calls.

The docs also note weaker or unsupported out-of-box areas:

- Multi-step chained workflows.
- Long multi-turn slot filling.
- Abstract/indirect semantic mapping when tool descriptions are too narrow.

### GemmaControl usage

Use FunctionGemma as a proposal/router only:

```text
User command
 -> compact phone context
 -> enriched tool registry
 -> FunctionGemma proposal
 -> Kotlin parse/validate
 -> Kotlin confirmation and execution
```

This matches the current implementation:

- `FunctionGemmaModelCatalog` defines `MobileActions-270M`.
- `GemmaPromptBuilder` builds bounded prompts with selected local WhatsApp context.
- `WhatsAppToolRegistry` defines the supported local tool surface.
- `LiteRtGemmaEngine` creates a LiteRT-LM conversation with `WhatsAppTools`.
- `GemmaModelManager` handles lifecycle, streaming, cancellation, and release.
- `ToolSafetyRouter` and executors keep execution inside Kotlin boundaries.

### Current app model

| Field | Value |
| :--- | :--- |
| Name | `MobileActions-270M` |
| Model id | `litert-community/functiongemma-270m-ft-mobile-actions` |
| File name | `mobile_actions_q8_ekv1024.litertlm` |
| Expected size | `288,964,608` bytes |
| Backend | CPU |
| Max tokens | `1024` |
| Sampling | `topK=64`, `topP=0.95`, `temperature=0.0` |

### Prompt design requirements

For GemmaControl, FunctionGemma prompts should include:

- Current date/time and day.
- Only selected local WhatsApp facts, not full raw history.
- Active notification keys and reply availability where relevant.
- Hidden-content and media-placeholder truth.
- Tool definitions with semantic phrases users actually say.
- Safety labels and explicit "Kotlin executes, model only proposes" wording.

Do not send:

- Unbounded message history.
- Raw private logs.
- Full app database dumps.
- Media descriptions when only placeholders exist.

## EmbeddingGemma

Official docs describe EmbeddingGemma as a 308M parameter multilingual text embedding model based on Gemma 3, optimized for everyday devices. The model card describes it as a 300M open embedding model that produces numerical vector representations of text.

### What it does

EmbeddingGemma maps text into vectors for downstream tasks:

- Information retrieval.
- Semantic similarity search.
- Classification.
- Clustering.
- Question/document matching.
- Fact verification style retrieval.

Official docs call out:

- 100+ language support.
- 2K input context.
- Output dimensions from 768 down to 128 using Matryoshka Representation Learning.
- Quantized storage/memory efficiency.
- Offline operation on local hardware.
- Query and document prompt formats for retrieval, including `task: search result | query:` and `title: ... | text:`.

### What it should do in GemmaControl

Use it as a local retrieval layer:

```text
Captured message text
 -> embedding vector
 -> local vector index
 -> top-k relevant message facts
 -> compact text context for FunctionGemma
```

Use cases:

- "What was that payment message?"
- "Anything urgent from office?"
- "What did Mom ask me to do?"
- "Find the address he sent."
- "Summarize family messages about tomorrow."
- Topic grouping: work, family, finance, urgent, travel.
- Deduplication of repeated notification updates.

### What it should not do

- Do not feed vectors directly to FunctionGemma.
- Do not treat embeddings as non-sensitive. They are derived from private messages and must be stored as sensitive local data.
- Do not add the model until #120 records current device performance and #113 defines storage/index tradeoffs.

### Recommended first spike

The first #113 spike is documented in [EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md](EMBEDDING_GEMMA_SEMANTIC_MEMORY_PLAN.md). It intentionally adds no model artifact and no Room migration.

Start future approved model work with:

- `128d` vectors for minimum storage.
- Then compare `256d`.
- Store vectors per message row.
- Optionally store vectors per conversation/day summary.
- Use exact cosine similarity first if message count is small.
- Add approximate nearest neighbor indexing only if counts/latency justify it.
- Keep vector storage encrypted or protected with the same privacy policy as message metadata.

## LiteRT-LM

LiteRT-LM is the runtime currently used for local `.litertlm` models. Its Kotlin API supports Android/JVM model loading, conversations, streaming, tools, and multimodal content for models that support it.

### Runtime responsibilities

For GemmaControl, LiteRT-LM should:

- Load the `.litertlm` model from app-private storage.
- Initialize in a background coroutine.
- Use app-private cache directory for runtime cache.
- Create short-lived conversations for tool proposals.
- Stream partial model text to the UI.
- Surface generated tool calls for Kotlin validation.
- Close/release engine resources on background or memory pressure.

### Current implementation notes

Current code follows the safer pattern:

- App stores model at `files/models/`.
- Runtime cache uses `cache/litertlm`.
- `GemmaModelManager` prevents duplicate initialization.
- `LiteRtGemmaEngine` initializes on `Dispatchers.IO`.
- The app uses CPU backend today.
- Conversations are closed after each proposal.
- The manager releases the model for background or memory pressure.

### Tool calling mode

LiteRT-LM supports automatic and manual tool execution. Current code enables automatic callbacks for the native `WhatsAppTools` proposal surface, but automatic execution is not acceptable for sensitive actions.

Required rule:

```text
The model may request a tool.
Kotlin decides whether and how to execute it.
```

For WhatsApp sends, data deletion, capture toggles, reminders, and app opening, Kotlin must validate parameters and obtain user confirmation where required. Native callbacks may gather proposals or read-only context, not bypass the safety router.

### Backend notes

Official LiteRT-LM docs expose CPU, GPU, and NPU backend options. The current connected phone has Adreno/Vulkan signals, but current app catalog uses CPU.

Do not switch backend blindly. #120 must benchmark:

- CPU cold start.
- CPU warm start.
- Tool routing latency.
- Memory and cache size.
- Thermal state.
- Any future GPU/NPU backend separately.

## Gemma 4

Gemma 4 is the newer general Gemma generation family. Official docs describe it as multimodal, with text and image input across the family, audio on E2B/E4B/12B, long context, thinking modes, and native function-calling support.

### Why it is attractive

- Better general reasoning and summarization than FunctionGemma.
- Multimodal support.
- Native function calling.
- Long context.
- Smaller E2B/E4B variants target edge/mobile deployment.
- Multi-Token Prediction support may improve generation speed where supported.

### Why it is not first

The official memory estimates for Gemma 4 E2B are still far heavier than FunctionGemma, even with quantization. On the connected Xiaomi/Redmi device, a second generative model can create load-time, memory, cache, and thermal problems.

Use only after:

- #120 benchmark exists.
- #109 proves deterministic/Kotlin summaries are not enough.
- #117 compares Gemma 4 E2B against Gemma 3n E2B and current FunctionGemma/Kotlin behavior.

### Possible role if approved later

Use as summarizer/rephraser, not executor:

```text
Retrieved message facts
 -> Gemma 4 E2B summary/rewrite
 -> Kotlin safety validation
 -> TTS/UI
```

Do not let a larger model silently execute phone actions.

## Gemma 3n

Gemma 3n is a device-optimized multimodal model family for phones, laptops, and tablets. Official docs emphasize parameter-efficient processing, audio input, visual/text input, PLE caching, MatFormer architecture, conditional parameter loading, wide language support, and 32K context.

### Why it is relevant

- Designed for everyday devices.
- Supports text, visual, and audio inputs.
- Can skip some visual/audio parameters to reduce memory.
- E2B effective mode is the realistic candidate for this phone class.

### Possible role

Gemma 3n E2B may be useful later for:

- Voice-friendly summaries.
- Audio understanding if Android speech recognition is insufficient.
- Image understanding when user-selected image bytes are available.
- Multimodal assistant responses.

### Current decision

Evaluation only. It should not be added before:

- FunctionGemma routing is benchmarked.
- EmbeddingGemma retrieval is evaluated.
- Device memory/thermal data supports another model.

## PaliGemma

PaliGemma and PaliGemma 2 are vision-language models for image+text inputs. Official docs say they can answer image questions, caption images/short videos, detect objects, and read text embedded in images.

### Use in GemmaControl

Do not use for WhatsApp notifications unless actual image bytes are available.

Notification text like "Photo", "Sticker", or "Image" is only a placeholder. The app must not invent image contents from that placeholder.

Possible future role:

- User manually selects an image.
- App obtains scoped media access.
- A vision model analyzes the actual bytes.
- Safety and confirmation policies apply.

## ShieldGemma

ShieldGemma 2 is an image safety classifier built on Gemma 3. Official docs describe it as taking an image and policy instruction and outputting safety scores/labels.

### Use in GemmaControl

Not needed for v1 WhatsApp notification reading/replying.

Potential future use:

- Image/media analysis safety.
- Filtering generated or received images if the app later handles actual media.

Prefer deterministic Kotlin safety first:

- Tool-level safety labels.
- Explicit confirmations.
- Bounded reply text.
- No destructive actions without review.

## Fine-Tuning

FunctionGemma docs and the `gemma-dev` skill both treat fine-tuning as appropriate when a domain needs more reliable tool routing than prompting/tool descriptions can provide.

For GemmaControl, fine-tuning is not first. It becomes relevant only if #115 shows:

- Tool descriptions and prompt context cannot reach acceptable routing accuracy.
- Malformed output remains high.
- Indirect natural language frequently maps to the wrong tool.
- Clarification handling remains weak despite Kotlin state.

If needed later, training data should be synthetic/sanitized:

- User command.
- Compact phone context.
- Expected tool call.
- Unsupported/negative examples.
- Confirmation-required examples.
- No private real WhatsApp text unless explicitly sanitized and approved.

## Module Recommendations

Current modules to keep:

- `FunctionGemmaModelCatalog`
- `ModelDownloadManager`
- `ModelDownloadWorker`
- `GemmaModelManager`
- `LiteRtGemmaEngine`
- `GemmaPromptBuilder`
- `WhatsAppToolRegistry`
- `ToolSchemaExporter`
- `ToolCallParser`
- `ToolSafetyRouter`
- `WhatsAppLocalToolExecutor`
- `ActiveNotificationReplyExecutor`

New modules to add in future issues:

| Module | Issue | Purpose |
| :--- | :--- | :--- |
| `PhoneContextSnapshot` | #114 | Typed compact phone context for model prompts. |
| `PhoneContextBuilder` | #114 | Builds bounded truth from notifications, stored messages, active replies, and permission state. |
| `AssistantCapabilityMatrix` | #123 | Maps tools to Android permissions/capabilities. |
| `DeviceBenchmarkReporter` | #120 | Captures model/device metrics without private content. |
| `EmbeddingGemmaPromptFormatter` | #113 | Formats query/document prompts for semantic retrieval. |
| `MessageEmbeddingProvider` | #113 | Keeps the embedding runtime behind one reusable provider boundary. |
| `ExactMessageEmbeddingIndex` | #113 | Retrieves top-k relevant message facts with exact cosine ranking. |
| `EmbeddingGemmaModelCatalog` | future #113 follow-up | Defines allowed embedding model artifacts after approval. |
| `MessageEmbeddingStore` | future #113 follow-up | Stores sensitive local embeddings per message/summary. |
| `SemanticContextSelector` | #113/#114 | Mixes recency and embedding relevance into compact prompt context. |
| `ModelDecisionReport` | #109/#117 | Records whether a bigger model is justified. |

## Source Links

- FunctionGemma overview: https://ai.google.dev/gemma/docs/functiongemma
- FunctionGemma model card: https://ai.google.dev/gemma/docs/functiongemma/model_card
- FunctionGemma formatting and best practices: https://ai.google.dev/gemma/docs/functiongemma/formatting-and-best-practices
- EmbeddingGemma overview: https://ai.google.dev/gemma/docs/embeddinggemma
- EmbeddingGemma model card: https://ai.google.dev/gemma/docs/embeddinggemma/model_card
- EmbeddingGemma embeddings tutorial: https://ai.google.dev/gemma/docs/embeddinggemma/inference-embeddinggemma-with-sentence-transformers
- Gemma 4 overview: https://ai.google.dev/gemma/docs/core
- Gemma 4 model card: https://ai.google.dev/gemma/docs/core/model_card_4
- Gemma 3 overview: https://ai.google.dev/gemma/docs/core
- Gemma 3 model card: https://ai.google.dev/gemma/docs/core/model_card_3
- Gemma 3n overview: https://ai.google.dev/gemma/docs/gemma-3n
- Gemma 3n model card: https://ai.google.dev/gemma/docs/gemma-3n/model_card
- PaliGemma overview: https://ai.google.dev/gemma/docs/paligemma
- ShieldGemma 2 model card: https://ai.google.dev/gemma/docs/shieldgemma/model_card_2
- LiteRT-LM Kotlin API: https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/kotlin/getting_started.md
