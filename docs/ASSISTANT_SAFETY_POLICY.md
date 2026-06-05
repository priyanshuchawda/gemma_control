# Assistant Safety Policy

Snapshot date: 2026-06-05

Related issues: #119, #106, #118, #112

## Decision

Use deterministic Kotlin safety gates for V1. Do not add ShieldGemma, ShieldGemma 2, or any other safety model now.

ShieldGemma-style classifiers are useful for future generated-content moderation and actual image/media processing, but they are extra models with non-trivial memory, latency, prompt-policy, and lifecycle cost. GemmaControl V1 does not generate open-ended external content and does not analyze actual media bytes, so deterministic rules plus explicit confirmations are the correct safety layer now.

No model is downloaded, imported, trained, converted, or loaded by this decision.

## Current Safety Stack

```text
User command
 -> deterministic planner / FunctionGemma proposal
 -> ToolCallParser
 -> AssistantCapabilityMatrix
 -> ToolSafetyRouter
 -> confirmation UI when required
 -> Kotlin executor
 -> Android/local storage boundary
```

FunctionGemma can propose a tool. It cannot bypass:

- tool-name allowlist
- required argument validation
- Android capability checks
- content-kind truthfulness checks
- active notification liveness
- reply text nonblank/bounded checks
- explicit user confirmation
- local data delete confirmation
- media placeholder rejection

## Operation Policy Levels

`AssistantSafetyPolicy` maps the current router and future media boundary into these policy levels:

| Policy level | Current examples | Confirmation | Model safety gate |
| :--- | :--- | :--- | :--- |
| `ReadOnly` | list/search/get captured WhatsApp messages, actionable inbox reads | No | Not needed for V1 |
| `LocalWrite` | create follow-up, schedule reminder, mark priority, mark follow-up complete | No external send; local UI may still surface result | Not needed for V1 |
| `ConfirmationRequired` | draft reply, pause/resume capture | Required | Not needed for V1 |
| `OpenExternalApp` | open WhatsApp share/click-to-chat draft | Required | Not needed for V1 |
| `SendMessage` | active notification RemoteInput reply | Strict manual confirmation and live notification required | Not needed for V1 |
| `DeleteData` | delete all or scoped local WhatsApp data | Strict manual confirmation and explicit delete intent required | Not needed for V1 |
| `MediaAnalysis` | future user-selected image/video analysis | Required | Future ShieldGemma 2-style image gate |
| `Reject` | hidden content analysis, notification placeholder image analysis, unsupported actions | No execution | Not needed for V1 |

The current `ToolSafetyRouter` remains the execution authority for WhatsApp tools. `AssistantSafetyPolicy` documents and tests the policy mapping, including the future `MediaAnalysis` level that is not a V1 tool.

## ShieldGemma Decision

### Text Safety

ShieldGemma 1 is a text moderation model family for prompt/output classification across harm categories such as dangerous content, harassment, hate speech, and sexually explicit content.

Do not add it for V1 because:

- replies are user-supplied or simple drafts, not autonomous open-ended generated messages
- high-risk action safety is structural: no silent send, no silent delete, no hidden media claims
- the model would add another large artifact and lifecycle path
- deterministic checks already cover the current tool risks more directly

Reconsider text moderation only if GemmaControl later generates substantive reply text, rewrites sensitive messages, or exposes a broader open-ended assistant surface.

### Image Safety

ShieldGemma 2 is a 4B image safety classifier. It is relevant only after #118 evolves from placeholder-only media handling into actual user-selected image/video analysis.

Do not add it now because:

- notification placeholders have no image bytes
- V1 has no media picker or URI-analysis flow
- a 4B safety model is too heavy to add before #120 device evidence
- the app still needs a full media-analysis safety policy before processing actual private images

If actual media analysis is approved later, ShieldGemma 2 should run as a pre/post analysis safety gate before any vision model output is shown or spoken.

## Deterministic Rules For V1

| Area | Rule |
| :--- | :--- |
| Message reading | Read only captured local notifications and stored rows after user consent. |
| Hidden content | Say content is hidden/unavailable; do not infer. |
| Media placeholders | Say attachment type only; do not analyze without user-selected bytes. |
| Reply | Require target resolution; active sends require strict manual confirmation. |
| Draft/open app | Require confirmation before launching WhatsApp intents. |
| Delete | Require explicit delete intent and strict confirmation; delete only GemmaControl local data. |
| Capture controls | Require confirmation before pause/resume when model-proposed. |
| Unsupported requests | Reject or clarify; do not improvise new Android powers. |
| Logs/benchmarks | Do not log private message text, sender names, group names, URIs, media contents, or generated private outputs. |

## Future Safety Gate

Add a model-based classifier only when all are true:

1. The product adds actual media analysis or autonomous generated reply text.
2. Deterministic rules cannot cover the new risk surface.
3. #120 records enough memory/latency/thermal headroom for the extra model.
4. The app can keep the single-import lifecycle: no duplicate model loading.
5. The user explicitly approves the new model artifact.
6. Tests prove policy prompts, refusal behavior, logging, and deletion flows.

## Source Links

- ShieldGemma overview: https://ai.google.dev/gemma/docs/shieldgemma
- ShieldGemma text model card: https://ai.google.dev/gemma/docs/shieldgemma/model_card
- ShieldGemma 2 image model card: https://ai.google.dev/gemma/docs/shieldgemma/model_card_2
