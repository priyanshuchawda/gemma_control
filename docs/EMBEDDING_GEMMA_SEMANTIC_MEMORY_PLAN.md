# EmbeddingGemma Semantic Memory Plan

Snapshot date: 2026-06-05

Related issues: #113, #112, #101, #108, #109, #120

## Decision

Evaluate EmbeddingGemma as the next optional model after the current FunctionGemma-only assistant is benchmarked. Do not download, bundle, or import the EmbeddingGemma model yet.

The #113 implementation adds a model-agnostic semantic retrieval scaffold and a fake-embedder unit-test prototype only. This proves prompt formatting, exact-vector ranking, media-placeholder safety, and the future integration boundary without changing the installed model set.

## Official Model Facts

Official Google docs describe EmbeddingGemma as a 308M parameter multilingual text embedding model based on Gemma 3, optimized for phones, laptops, and tablets. It produces numerical text representations for retrieval, semantic similarity search, classification, and clustering.

Useful properties for GemmaControl:

| Property | Value for planning | Source |
| :--- | :--- | :--- |
| Model role | Text embeddings, not chat generation | EmbeddingGemma overview |
| Parameters | 308M in overview; 307,581,696 shown in the Sentence Transformers tutorial | Overview and tutorial |
| Default vector size | 768 dimensions | Tutorial output |
| Flexible dimensions | 768 down to 128 via Matryoshka Representation Learning | Overview and tutorial |
| Context | 2K token input context | Overview |
| Quantized memory claim | Less than 200MB RAM with quantization | Overview |
| Retrieval prompts | Query: `task: search result | query:`; document: `title: ... | text:` | Model card/tutorial |
| Offline fit | Designed for local hardware without internet at inference | Overview/tutorial |

Source links:

- EmbeddingGemma overview: https://ai.google.dev/gemma/docs/embeddinggemma
- EmbeddingGemma model card: https://ai.google.dev/gemma/docs/embeddinggemma/model_card
- Generate embeddings tutorial: https://ai.google.dev/gemma/docs/embeddinggemma/inference-embeddinggemma-with-sentence-transformers

## Why This Helps

Current keyword search is useful for exact words, but real voice commands often refer to meaning:

- "Show me that payment message."
- "What did office say about the meeting?"
- "Find the address he sent."
- "Anything urgent from family?"
- "Summarize important school messages."

The right architecture is not to give FunctionGemma the whole message history. The right architecture is:

```text
User query
 -> EmbeddingGemma query vector
 -> local top-k message retrieval
 -> compact text facts
 -> FunctionGemma tool routing or Kotlin read/summarize behavior
```

FunctionGemma still remains the tool router. EmbeddingGemma only selects relevant local facts.

## Current Device Fit

The target phone is Xiaomi/Redmi `2406ERN9CI`, Android 16/API 36, with `5,531,208 kB` physical RAM and about `1,690,236 kB` available in the captured snapshot. FunctionGemma is already installed at about `276 MB`, and the LiteRT/XNNPACK cache is about `260 MB`.

That makes EmbeddingGemma plausible but not automatically approved. It is smaller and safer than adding a second generative model, but it still needs:

- #120 cold/warm model load measurements.
- Memory and thermal checks while FunctionGemma is installed.
- Explicit user approval before any model artifact download.
- A purge/encryption design before any private embeddings are persisted.

## Implemented No-Model Scaffold

Added Android app module:

- `EmbeddingGemmaPromptFormatter`
- `EmbeddingVector`
- `MessageEmbeddingProvider`
- `MessageEmbeddingRecord`
- `ExactMessageEmbeddingIndex`
- `SemanticMessageRetrievalPrototype`

The scaffold intentionally has no model artifact and no runtime dependency. It accepts an embedding provider interface so the future real implementation can wrap whichever Android runtime path is approved later.

Current tests use `KeywordCategoryEmbeddingProvider`, a deterministic fake embedder, to prove:

- Query/document prompts match EmbeddingGemma retrieval prompt shapes.
- Photo notifications remain placeholders and do not imply image inspection.
- A natural-language payment query can retrieve the relevant sample message.
- Exact cosine ranking has deterministic tie-breaking by recency.
- Mixed vector dimensions are ignored rather than crashed during search.

## Prompt Formatting Contract

For queries:

```text
task: search result | query: {user query}
```

For message documents:

```text
title: {conversation title or none} | text:
chat: {conversation}; sender: {sender or unknown}; kind: {content kind};
priority: {priority}; posted_at_ms: {timestamp}; message: {truthful message body}
```

For media and hidden content, the body uses the existing truthful notification text:

- Photo attachment: contents not inspected.
- Video attachment: contents not inspected.
- Sticker: contents not inspected.
- Audio or voice message: not transcribed.
- Document attachment: contents not inspected.
- Hidden content: content hidden or unavailable.

This prevents semantic memory from becoming a place where the app invents image, audio, or hidden-message contents.

## Future Storage Design

Do not add this Room schema until the real model spike is approved.

Recommended first table:

```kotlin
@Entity(
    tableName = "message_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = MessageEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageEventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["messageEventId"], unique = true),
        Index(value = ["modelId"]),
        Index(value = ["dimension"])
    ]
)
data class MessageEmbeddingEntity(
    @PrimaryKey val id: String,
    val messageEventId: String,
    val modelId: String,
    val dimension: Int,
    val quantization: String,
    val encryptedVectorBytes: ByteArray,
    val vectorIv: ByteArray,
    val sourceTextToken: String,
    val promptVersion: Int,
    val createdAt: Long
)
```

Recommended later table only if needed:

```text
conversation_embedding_summaries(
  id,
  conversationId,
  modelId,
  dimension,
  summaryWindowStart,
  summaryWindowEnd,
  encryptedVectorBytes,
  vectorIv,
  promptVersion,
  createdAt
)
```

Start with message-level vectors only. Add conversation/day summaries later if exact per-message retrieval becomes too noisy or too slow.

## Vector Storage Cost

Approximate storage per message before database overhead:

| Dimension | Float32 vector | Int8/quantized vector |
| :--- | ---: | ---: |
| 128d | 512 B | 128 B |
| 256d | 1,024 B | 256 B |
| 512d | 2,048 B | 512 B |
| 768d | 3,072 B | 768 B |

Estimated raw vector storage:

| Message rows | 128d float32 | 256d float32 | 768d float32 |
| ---: | ---: | ---: | ---: |
| 1,000 | about 0.5 MB | about 1.0 MB | about 3.1 MB |
| 10,000 | about 5.1 MB | about 10.2 MB | about 30.7 MB |
| 50,000 | about 25.6 MB | about 51.2 MB | about 153.6 MB |

The first approved spike should use `128d`, exact cosine search, and a small retention window. Move to `256d` only if retrieval quality misses important messages.

## Retrieval Ranking

Phase 1:

```text
candidate messages
 -> exact cosine similarity
 -> tie-break by postedAt desc
 -> tie-break by message id
 -> top 5
```

Phase 2 after measurement:

```text
keyword filter + time/chat filter
 -> vector rerank
 -> priority/active-notification boost
 -> top-k facts for prompt context
```

Only add an approximate nearest-neighbor index if exact search becomes slow on real stored-message counts.

## Privacy Rules

Embeddings are sensitive. They are derived from private messages and can leak meaning even when the original text is encrypted.

Required rules:

- Store embeddings only after explicit storage consent.
- Keep all embedding generation local.
- Encrypt persisted vector bytes or protect them under the same local-sensitive-data policy as message bodies.
- Delete embeddings automatically when the linked `message_events` row is deleted.
- Include embeddings in `delete_all` local WhatsApp purge.
- Do not log vectors, vector dimensions tied to private messages, query text, sender names, group names, or retrieved private message bodies.
- Do not sync embeddings or use them for analytics.

## Android Integration Path

1. Keep the current no-model scaffold.
2. Finish #120 FunctionGemma/device benchmark first.
3. Ask for explicit approval before downloading any EmbeddingGemma artifact.
4. Add an allowlisted `EmbeddingGemmaModelCatalog` entry.
5. Add a separate `EmbeddingModelManager` so FunctionGemma is not imported/initialized repeatedly.
6. Generate embeddings in a background worker after capture, with battery/thermal checks.
7. Persist vectors only after storage consent and purge linkage.
8. Add `search_semantic_whatsapp_messages` only after retrieval quality beats keyword search on the benchmark corpus.

The real model must be imported once and reused through a manager, matching the user's model lifecycle requirement. It must not be loaded independently by every tool call.

## Evaluation Corpus

Use synthetic/private-free samples first:

| User query | Expected retrieval |
| :--- | :--- |
| "show me that payment message" | UPI/payment/invoice row |
| "what did office say about meeting" | office/client/meeting row |
| "find the address he sent" | address/location row |
| "anything urgent from family" | family row with urgent/priority signal |
| "what did Mom ask me to do" | Mom/request/action row |
| "which message mentioned tomorrow appointment" | appointment/schedule row |
| "what photo did they send" | photo placeholder only, no image description |
| "read hidden message" | hidden-content unavailable row, no content claim |

Success criteria for the approved real-model spike:

- Top-1 accuracy at least 80% on synthetic corpus.
- Top-3 accuracy at least 95% on synthetic corpus.
- No private text in logs.
- Query latency acceptable on the Redmi 13 5G with current FunctionGemma installed.
- Memory/thermal impact does not degrade the existing WhatsApp assistant flow.

## Current Recommendation

Keep #113 in design/prototype state until #120 exists and the user approves a model artifact. The scaffold is ready for a real provider later, but production should continue using keyword search, follow-ups, priority flags, and FunctionGemma routing for now.
