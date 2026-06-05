# FunctionGemma Routing Benchmark

This benchmark is intentionally offline for the current device-development phase. It does not run or download a model. The goal is to lock the routing corpus, enriched tool-description expectations, and Kotlin fallback behavior before expanding the native FunctionGemma tool surface.

## Current Baseline

| Metric | Value |
| --- | --- |
| App-level WhatsApp tool registry | 16 tools |
| Native LiteRT `WhatsAppTools` callbacks | 13 callbacks |
| Offline model routing accuracy | 0% recorded, model execution deferred |
| Model download requirement | None |
| Verification owner | JVM tests plus later physical-device model run |

The current baseline is not a quality score for FunctionGemma. It records that model execution is deliberately deferred while the app builds the deterministic corpus and prompt/tool-description surface.

Fine-tuning decision: [FUNCTION_GEMMA_FINE_TUNING_DECISION.md](FUNCTION_GEMMA_FINE_TUNING_DECISION.md). Current decision is no fine-tuning until this benchmark records persistent routing, malformed-output, or safety-refusal failures after prompt/context improvements.

## Corpus Categories

The JVM corpus in `FunctionGemmaRoutingBenchmarkCorpusTest` covers:

- read latest / what did I miss
- summarize recent WhatsApp context
- continue reading
- search by topic, time, sender, or keyword
- reply to latest active notification
- reply to a named chat as a draft
- multiple active chats
- follow-up and todo creation
- reminder scheduling
- priority / urgent-message handling
- unsupported voice notes
- unsupported direct WhatsApp history reads
- unsupported hidden media-content understanding

## Routing Expectations

- Tool descriptions include natural phrases such as "what did I miss", "find the payment message", "tell Mom", "anything urgent", "remind me later", and "mark important".
- Kotlin remains responsible for unsupported and high-risk workflows. Voice notes and empty replies are clarified before model execution; broad direct-history and hidden-media requests can reach FunctionGemma only as proposal requests and still require Kotlin validation before any action.
- Future physical runs should report routing accuracy, malformed output rate, unsupported-command quality, latency, and battery/thermal notes against this same corpus.
- Future fine-tuning evaluation must use only synthetic or explicitly sanitized/approved rows. The starter schema and examples live in [function_gemma_finetune_dataset_template.jsonl](function_gemma_finetune_dataset_template.jsonl).
