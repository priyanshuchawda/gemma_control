# FunctionGemma Fine-Tuning Decision

Snapshot date: 2026-06-05

Related issues: #116, #112, #115

Dataset template: [function_gemma_finetune_dataset_template.jsonl](function_gemma_finetune_dataset_template.jsonl)

## Decision

Do not fine-tune FunctionGemma now.

Keep V1 on:

```text
enriched tool descriptions
 -> compact PhoneContextSnapshot
 -> FunctionGemma proposal
 -> Kotlin parser and safety router
 -> user confirmation where required
```

Fine-tuning becomes justified only if the #115 routing benchmark and later physical-device FunctionGemma runs show that prompt/tool-description improvements cannot reach production routing quality.

No training, model download, model conversion, or model import is approved by this decision.

## Why

The current app has a small, typed, safety-labelled tool surface. FunctionGemma is already the right model class for routing natural language into local tool calls, but the highest-risk failures are not model-capability problems alone:

- missing Android capability state
- stale active notification reply targets
- hidden/media placeholder truthfulness
- ambiguous chat names
- unsafe execution without confirmation
- malformed or incomplete tool arguments

Those must remain Kotlin responsibilities. Fine-tuning can improve command-to-tool mapping, but it must not become a way to bypass `ToolCallParser`, `ToolSafetyRouter`, capability checks, confirmation sheets, or local privacy policy.

## Fine-Tuning Gate

Revisit fine-tuning only when all conditions are true:

| Gate | Required evidence |
| :--- | :--- |
| Routing accuracy | Physical and offline benchmark accuracy stays below `95%` for supported single-turn WhatsApp commands after prompt/tool-description improvements. |
| Malformed output | Valid JSON/tool-call parse rate stays below `99%`, or malformed output appears in safety-critical categories. |
| Safety routing | Unsupported, hidden-content, media-placeholder, delete, open-app, and send-message prompts fail cleanly less than `100%` of the time. |
| Semantic nuance | Indirect commands like "anything urgent from office" or "what did Mom ask me to do" route incorrectly after #113 retrieval context is available. |
| Latency | Fine-tuned artifact does not regress p95 proposal latency by more than `20%` on the Xiaomi Redmi 13 5G benchmark. |
| Maintenance | Tool schema version, prompt version, dataset version, and evaluation report are all versioned together. |
| Privacy | The dataset is synthetic or explicitly sanitized and approved. No private WhatsApp text, names, phone numbers, notification keys, or group names are used raw. |
| Approval | The user explicitly approves training and later model import/download. |

If any gate is missing, continue improving prompts, compact context, deterministic fallback routing, and the local parser first.

## Metrics To Report

Each fine-tuning evaluation report must include:

- routing accuracy by category
- malformed output rate
- unsupported/refusal quality
- wrong-tool rate
- missing-required-argument rate
- unsafe-confirmation bypass attempts
- p50/p95 routing latency
- model file size and runtime cache size
- RAM/PSS impact on the target phone
- prompt/tool schema version
- dataset version and privacy review status

Do not report private message bodies or real contact/chat names.

## Dataset Schema

Each JSONL record should follow this shape:

```json
{
  "id": "stable-example-id",
  "schema_version": "functiongemma-routing-v1",
  "split": "train|eval|holdout",
  "category": "read|summarize|search|reply|follow_up|reminder|unsupported|open_external_app|delete_data",
  "user_utterance": "English user command",
  "compact_context": "Bounded synthetic PhoneContextSnapshot facts",
  "expected_tool_name": "tool name or null",
  "expected_tool_arguments": {},
  "expected_safety_level": "ReadOnly|LocalWrite|ConfirmationRequired|OpenExternalApp|SendMessage|DeleteData|Reject",
  "expected_assistant_policy": "Short expected policy behavior",
  "privacy_source": "synthetic|sanitized_approved"
}
```

Required dataset coverage:

- read latest / chat-specific read
- summarize recent WhatsApp context
- search by topic, sender, and time
- reply to live active notification
- draft reply for named chat
- follow-up creation
- reminder scheduling
- unsupported direct WhatsApp history access
- unsupported hidden/media-content claims
- open WhatsApp draft flows
- local data deletion
- capture pause/resume
- ambiguous multiple-chat commands

## Synthetic Template Policy

Use [function_gemma_finetune_dataset_template.jsonl](function_gemma_finetune_dataset_template.jsonl) as the starter template. It intentionally uses synthetic contact names and synthetic message ids.

Before any real training set is created:

1. Replace private user names with synthetic aliases.
2. Replace phone numbers with fake E.164 examples reserved for documentation or testing.
3. Replace message contents with synthetic equivalents that preserve intent only.
4. Remove notification keys, timestamps that identify real events, group names, and attachment filenames.
5. Keep hidden/media-placeholder examples as placeholders, not invented image/audio descriptions.
6. Tag every row with `privacy_source`.
7. Review the dataset against the current `WhatsAppToolRegistry` and `ToolSafetyRouter`.

## Evaluation Order

Run this order before training:

```text
1. deterministic planner corpus
2. FunctionGemma zero-shot prompt/tool-description corpus
3. FunctionGemma with #114 compact context
4. FunctionGemma with #113 semantic retrieval context, if approved
5. synthetic fine-tune dry-run design review
6. training only after explicit approval
```

Do not fine-tune against stale tool names. If a tool schema changes, regenerate the template and rerun all routing tests.

## Current Recommendation

For the current phone and roadmap, #116 is complete as an evaluation decision:

- no fine-tuning now
- no private data training set now
- no model download now
- keep FunctionGemma imported/loaded through the existing single model lifecycle
- use the synthetic template only as a future benchmark/training contract

## Source Links

- FunctionGemma overview: https://ai.google.dev/gemma/docs/functiongemma
- FunctionGemma model card: https://ai.google.dev/gemma/docs/functiongemma/model_card
- FunctionGemma formatting and best practices: https://ai.google.dev/gemma/docs/functiongemma/formatting-and-best-practices
