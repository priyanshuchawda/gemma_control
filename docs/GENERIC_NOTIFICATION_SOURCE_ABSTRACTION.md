# Generic Notification Source Abstraction

Snapshot date: 2026-06-05

Related issues: #110, #101, #102

## Decision

Add a generic notification source abstraction, but keep WhatsApp as the only enabled production source in V1.

The app now has a source catalog and common event/query models that can represent WhatsApp, SMS, Gmail, Phone, Calendar, and Other sources without rewriting the schema later. This does not broaden capture. Non-WhatsApp sources are recognized as planned or unsupported only, with zero enabled actions.

## Source Catalog

`NotificationSourceCatalog` defines source descriptors:

| Source | Example package(s) | Status | Enabled actions now | Planned shape |
| :--- | :--- | :--- | :--- | :--- |
| WhatsApp | `com.whatsapp`, `com.whatsapp.w4b` | `ENABLED` | capture, read, summarize, search, active reply, open draft, source-specific local writes | Current V1 implementation |
| SMS | `com.google.android.apps.messaging`, `com.android.mms` | `PLANNED` | none | Future read/summarize/search after permission and parser review |
| Gmail | `com.google.android.gm` | `PLANNED` | none | Future read/summarize/search after notification parser review |
| Phone | `com.google.android.dialer`, `com.android.dialer`, `com.android.server.telecom` | `PLANNED` | none | Future call notification summaries only; no calling actions without separate review |
| Calendar | `com.google.android.calendar` | `PLANNED` | none | Future read/summarize/search and event actions after separate capability review |
| Other | Any unrecognized package | `UNSUPPORTED` | none | No automatic behavior |

Only `NotificationSourceImplementationStatus.ENABLED` sources can expose enabled actions.

## Common API Shape

The shared types are deliberately small:

- `NotificationSourceType`: `WHATSAPP`, `SMS`, `GMAIL`, `PHONE`, `CALENDAR`, `OTHER`
- `NotificationCommonAction`: capture, read, summarize, search, active reply, open draft, source-specific write
- `NotificationQueryScope`: common read/summarize/search request shape with source types, conversation filter, query, time window, and limit
- `ParsedNotificationEvent`: generic event projection with source descriptor, lifecycle, conversation, message list, reply availability, parse source, and active state
- `ParsedNotificationMessage`: sender/body/timestamp plus generic `NotificationContentState`

WhatsApp-specific rows still keep their existing `ParsedWhatsAppNotificationEvent` and Room entities. The generic projection is a compatibility layer for future source work, not a database migration.

## Safety Boundary

Production capture remains WhatsApp-only:

```kotlin
WhatsAppNotificationParser.isPackageSupported(packageName)
```

now delegates to:

```kotlin
NotificationSourceCatalog.isProductionCaptureEnabled(packageName)
```

Only WhatsApp packages return `true`.

Active notification replies are also source-gated:

```kotlin
NotificationSourceCatalog.canUseActiveNotificationReply(packageName)
```

Only WhatsApp packages return `true`; SMS, Gmail, Phone, Calendar, and Other return `false`.

This prevents a future source descriptor from accidentally inheriting WhatsApp reply/send behavior.

## Future Source Onboarding Gate

Before enabling another source:

1. Add source-specific notification parser tests.
2. Define content kinds and hidden/media/system handling.
3. Add capability matrix entries for required Android permissions.
4. Decide whether replies/actions are read-only, local-write, open-app, send, or disallowed.
5. Add safety-router tests proving unsupported actions reject.
6. Add manual device validation for that source.
7. Keep source-specific executors separate from WhatsApp executors.

## Tests

Current no-phone tests:

- WhatsApp packages are the only enabled production capture sources.
- SMS/Gmail/Phone/Calendar packages are recognized but have no enabled capture or reply actions.
- Unknown packages remain unsupported.
- WhatsApp parser package support uses the source catalog.
- A parsed WhatsApp event can project into a generic `ParsedNotificationEvent`.
- `NotificationQueryScope` can represent future SMS/Gmail read/search scopes without enabling those sources.
