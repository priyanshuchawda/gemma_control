# Android Accessibility Service Evaluation

Snapshot date: 2026-06-05

Related issues: #122, #101, #106, #107, #110, #123

## Decision

Accessibility is **not needed for V1** and must not be implemented before a separate reviewed prototype issue.

For V1, GemmaControl should continue using:

- Notification Listener Access for captured WhatsApp notifications.
- Active notification `RemoteInput` for strictly user-confirmed replies.
- Android intents and WhatsApp draft flows for manual handoff.
- Local encrypted storage for captured rows, follow-ups, priorities, and reminders.
- FunctionGemma only as a tool-call proposal/router.

Accessibility can be reconsidered for V2 only if the product explicitly expands into broader assistive phone control and the safety model below is accepted.

## Official Android Facts

Android accessibility services can observe accessibility events, inspect active-window node trees when configured to retrieve window content, perform node actions such as clicks/scrolls/text actions, and perform global actions such as Back/Home/Recents.

That capability is powerful enough to help a user operate apps, but it is also sensitive because it may expose visible screen text and allow interaction with other apps on the user's behalf.

Google Play policy also treats AccessibilityService use as a special API. Apps targeting Android 12+ that use it need a Play Console accessibility declaration, and only services genuinely designed to help people with disabilities should declare `isAccessibilityTool=true`.

Source links:

- Android accessibility service guide: https://developer.android.com/guide/topics/ui/accessibility/service
- Android `AccessibilityService` API reference: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Google Play AccessibilityService API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Android restricted settings help: https://support.google.com/android/answer/12623953

## Current Device Reality

The target Xiaomi/Redmi device snapshot says:

- `enabled_accessibility_services=null`
- Accessibility is not enabled.
- `ACCESS_RESTRICTED_SETTINGS` had default/recent rejected signals.
- Xiaomi/HyperOS setup may require manual user handling.

This means Accessibility cannot be assumed during tests or normal use. Even if implemented later, the app must be useful without it.

## Capability Comparison

| Workflow | Current V1 path | Accessibility-only value | V1 decision |
| :--- | :--- | :--- | :--- |
| Read new WhatsApp notifications | Notification listener | None for new notification capture | Use current path. |
| Read old/full WhatsApp chat screen | Not available unless messages were captured earlier | Could read currently visible screen text after user opens WhatsApp/chat | V2 evaluation only. |
| Search WhatsApp UI | Current local search covers captured rows only | Could tap/search inside WhatsApp UI if screen labels are stable | V2 evaluation only; brittle. |
| Open a chat | Click-to-chat/share intents where possible | Could navigate WhatsApp UI to a visible chat row | Prefer intents/manual handoff first. |
| Draft a reply | App draft sheet, share/click-to-chat, active notification target | Could type text into focused WhatsApp input field | Draft-only if ever approved; never auto-send. |
| Send a reply | Active notification `RemoteInput` after strict confirmation | Could tap WhatsApp send button | Disallowed for Accessibility automation in current plan. |
| Delete messages/chats | Not supported | Could tap destructive WhatsApp UI | Disallowed. |
| Change Android/WhatsApp settings | Not supported | Could navigate settings UI | Disallowed except opening settings for manual user action. |
| Media analysis | Not available from notification placeholders | Could see visible media labels/thumbnails but not reliably inspect bytes | Use media picker/user-selected files instead. |
| Hidden/redacted content | Truthfully unavailable | Cannot recover content hidden by Android/WhatsApp | Remains unavailable. |
| Other apps beyond WhatsApp | Out of V1 scope | Could read/control visible UI of other apps | Future product decision only. |

## Safety Policy

| Category | Accessibility examples | Policy |
| :--- | :--- | :--- |
| Read-only | Detect foreground app, read visible text labels, identify visible buttons, report current screen state | Allowed only in an approved prototype, only after explicit user enablement, and only for the foreground task the user requested. |
| Draft-only | Focus a message field, place prepared reply text, open a chat/search field | Allowed only if the user requested that exact target and a visible confirmation exists before any external app action. |
| Confirm-required | Tap non-destructive navigation, scroll a list, press Back/Home, open a specific chat, start a WhatsApp search | Requires in-app preview plus user confirmation before the action sequence begins; stop if UI state changes. |
| Strict manual handoff | Final WhatsApp send, system permission toggles, account/security screens | The assistant may explain the next step; the user must perform the final action manually. |
| Disallowed | Send messages, delete content, archive chats, change privacy/security settings, approve permissions, install apps, interact with payments/OTP/2FA, bypass restricted settings | Must not be automated by Accessibility. |

## Required Product Rules If Revisited

1. Accessibility must be a separate user-enabled mode, not part of the default WhatsApp notification assistant.
2. The service must have a narrow purpose that the user can understand.
3. The app must show prominent disclosure and obtain explicit consent before enabling.
4. The app must not run hidden broad automation in the background.
5. The app must never silently send messages or perform destructive actions.
6. Every action sequence must be interruptible.
7. UI automation must stop when package, screen, focused node, or visible text no longer matches the planned state.
8. No visible screen text, node tree, or package event stream may be logged with private content.
9. Physical Xiaomi/HyperOS persistence must be tested after reboot, idle, swipe-away, and battery-saver conditions.
10. Google Play declaration and `isAccessibilityTool` eligibility must be decided before distribution.

## Prototype Gate

No Accessibility code should start until a future issue provides:

- Exact V2 user story.
- User-facing disclosure copy.
- Accessibility service metadata plan.
- Play policy review.
- Per-action safety table.
- Kill switch and pause/resume behavior.
- Privacy-safe log policy.
- Xiaomi/HyperOS manual setup checklist.
- Manual device test matrix.

First allowed prototype, if approved later:

```text
User opens WhatsApp manually
 -> User says "what is visible on this chat?"
 -> Accessibility reads visible text only
 -> App summarizes visible screen state
 -> No taps, no typing, no send
```

Only after that passes should draft-only UI actions be considered.

## Recommendation

Close #122 with this decision:

```text
V1: no Accessibility service.
V2: possible assistive mode after explicit review.
Never: silent sends, destructive actions, permission toggles, restricted-settings bypass, or broad background control.
```

This keeps GemmaControl reliable for real users now while preserving a controlled path toward broader phone assistance if the safety and policy requirements are worth the added complexity.
