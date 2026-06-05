# Media Understanding Boundary

Snapshot date: 2026-06-05

Related issues: #118, #112, #102, #119

## Decision

Keep V1 WhatsApp media handling placeholder-only.

When WhatsApp notifications expose text such as `Photo`, `Sticker`, `Video`, `Audio`, or `Document`, GemmaControl must say only the attachment type and that contents were not inspected. It must not describe image contents, summarize audio, read document text, infer objects, identify people, or claim OCR from notification text.

Future media understanding is allowed only through an explicit media-access flow where the user selects a file or grants a scoped URI. This issue does not add a media model, does not request gallery/media permissions, and does not download or import any model.

## Media States

| State | Meaning | Current V1 behavior | Future path |
| :--- | :--- | :--- | :--- |
| `NotificationPlaceholderOnly` | Notification text says a media type exists, but no bytes/URI are available to the app. | Read/summarize as "photo/sticker/document attachment; contents not inspected." | Never send to a vision model. Ask the user to select the media if analysis is wanted. |
| `UserSelectedFile` | User explicitly selects an image/video through Android Photo Picker or another approved picker. | Not implemented in V1. | Analyze only after confirmation and model approval. |
| `LocalUriWithGrant` | App has a user-granted content URI, optionally persisted for a bounded workflow. | Not implemented in V1. | Analyze only after confirmation; keep URI grants scoped and revocable. |
| `Unsupported` | Hidden content, unsupported document/audio type, missing MIME type, or no permission. | Refuse analysis and explain the limitation. | Add separate extractor/model only after policy review. |

The Kotlin guard for this policy is `MediaUnderstandingPolicy`. It is deliberately JVM-testable and does not depend on Android `Uri` or any model runtime.

## Android Access Boundary

Notification Listener access is not gallery/media access. It can capture notification metadata and text that Android exposes, but it does not give GemmaControl the actual image, video, sticker, voice note, or document bytes behind a WhatsApp notification.

For future user-approved media analysis, prefer Android Photo Picker:

- it lets the user select specific images/videos
- it avoids broad media-library access
- it integrates with Compose through Activity Result contracts
- it can fall back to `ACTION_OPEN_DOCUMENT` on devices where the picker is unavailable

Do not request broad media permissions for V1 WhatsApp notification reading/replying. On Android 14+ / target SDK 34+, selected-media access exists for apps that build their own gallery picker, but GemmaControl should not build a broad gallery browser unless media understanding becomes a primary product feature.

## Model Choice Boundary

Actual media analysis is separated from notification capture V1:

| Candidate | Potential role | Decision |
| :--- | :--- | :--- |
| Gemma 3+ image understanding | General image description, object/scene reasoning, OCR-style understanding where supported. | Future only after actual user-selected bytes and runtime approval. |
| Gemma 3n E2B/E4B | Device-oriented multimodal text/image/audio path. | Future only; still much heavier than FunctionGemma and deferred by #117. |
| Gemma 4 E2B/E4B | Stronger multimodal reasoning if runtime/device headroom exists. | Future only; requires #120 benchmark evidence and explicit model approval. |
| PaliGemma / PaliGemma 2 | Vision-language model for captioning, VQA, text reading, object detection/segmentation style tasks. | Future image-specific candidate, not for notification placeholders. |
| ShieldGemma 2 | Image safety classifier for future actual media processing. | Future-only per [ASSISTANT_SAFETY_POLICY.md](ASSISTANT_SAFETY_POLICY.md); not needed for V1 placeholders. |

No model should receive a prompt that implies a placeholder contains real visual/audio/document content.

## Prompt And TTS Rules

For placeholder-only media:

```text
content_kind=PHOTO
body=Photo attachment (contents not inspected)
```

Spoken output:

```text
Photo attachment. I cannot inspect image contents from the notification.
```

For actual user-selected image/video media in a future flow:

```text
media_access_state=UserSelectedFile
mime_type=image/jpeg
user_confirmed_analysis=true
actual_media_bytes_available=true
```

Do not reuse stored notification rows as proof that bytes exist.

## Safety Requirements For Future Media

Before implementing actual media analysis:

1. Add a user-visible picker/confirmation flow.
2. Store only scoped URI metadata needed for the current task.
3. Do not log URI strings, filenames, OCR text, or model outputs containing private media content.
4. Purge temporary decoded bitmaps and extracted text after the task.
5. Run the model through the same single-import lifecycle rule: no duplicate model loading.
6. Follow [ASSISTANT_SAFETY_POLICY.md](ASSISTANT_SAFETY_POLICY.md) before enabling image/media analysis.
7. Benchmark memory, latency, cache, and thermal cost on the Xiaomi Redmi 13 5G.

## Tests

Current no-phone tests cover:

- `MediaUnderstandingPolicyTest`: placeholders cannot inspect bytes or enter a model path; selected image/video states require user-granted access and confirmation.
- `GemmaPromptBuilderTest`: prompt bodies for photo placeholders say contents were not inspected.
- `PhoneContextSnapshotBuilderTest`: compact phone context keeps media content kind and non-invented body text.
- `WhatsAppLocalToolExecutorTest`: local reads return attachment placeholders, not descriptions.
- `VoiceReadAloudBuilderTest`: TTS uses `WhatsAppContentKind.spokenSummaryText`.

## Source Links

- Android Photo Picker: https://developer.android.com/training/data-storage/shared/photo-picker
- Android selected photo/video access: https://developer.android.com/about/versions/14/changes/partial-photo-video-access
- Gemma image understanding: https://ai.google.dev/gemma/docs/capabilities/vision/image
- Gemma 3n model card: https://ai.google.dev/gemma/docs/gemma-3n/model_card
- PaliGemma model card: https://ai.google.dev/gemma/docs/paligemma/model-card
