# Model Lab

Small local scripts for testing the downloaded Gemma checkpoints before moving
prompt, schema, or retrieval logic into the Android app.

These scripts use synthetic WhatsApp data only.

## FunctionGemma routing

```powershell
C:\Python313\python.exe scripts\model_lab\functiongemma_router_lab.py --model base
```

The base `functiongemma-270m-it` checkpoint is the better local test target for
custom WhatsApp tools. The `FunctionGemma_270M_Mobile_Actions` fine-tune is very
good for its seven trained mobile actions, but local testing showed it does not
generalize reliably to our WhatsApp tool names and arguments.

Use custom commands like this:

```powershell
C:\Python313\python.exe scripts\model_lab\functiongemma_router_lab.py --model base --command "Summarize my WhatsApp messages"
```

## EmbeddingGemma retrieval

```powershell
C:\Python313\python.exe scripts\model_lab\embeddinggemma_retrieval_lab.py
```

This manually follows the local SentenceTransformer module layout:

1. Gemma text backbone
2. Mean pooling
3. Two dense layers
4. L2 normalization

It is useful for testing stored message search, chat/context retrieval, and
future semantic memory. It does not generate summaries.

## Current local findings

- FunctionGemma should be used for tool routing, not long-form summarization.
- The base FunctionGemma checkpoint routes custom WhatsApp tools better than the
  mobile-actions fine-tune.
- The mobile-actions fine-tune can still validate the `call:name{...}` output
  format, but it is biased toward its training functions.
- EmbeddingGemma works well for ranking relevant mock messages by query.
- If FunctionGemma returns plain text or malformed/missing arguments, Android
  should convert that to a local clarification/fallback instead of showing a raw
  LiteRT error.
