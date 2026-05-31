# GemmaControl Download Website

Static TypeScript website for distributing GemmaControl release artifacts without committing binaries.

## Local Commands

```powershell
npm run typecheck
npm run build
npm run serve
```

The build output is written to `website/dist/`.

## Source Structure

```text
src/main.ts              # Page bootstrap only
src/downloads.ts         # Download card data
src/download-card.ts     # Download card DOM rendering
src/icons.ts             # Inline SVG icon helpers
src/header.ts            # Sticky header scroll state
src/styles/              # CSS split by base/header/hero/sections/responsive
```

## APK Distribution

The primary Android button points to:

```text
downloads/GemmaControl.apk
```

To make the direct download work for a static build, place a signed APK at:

```text
website/public/downloads/GemmaControl.apk
```

Then run:

```powershell
npm run build
```

The APK will be copied into `website/dist/downloads/` for local preview or hosting, but it must not be committed. For production, GitHub Releases is preferred; update `website/src/main.ts` if the canonical artifact URL changes.

## Artifact Rules

Do not commit:

- APK or AAB files
- EXE or MSI installers
- `.litertlm`, `.tflite`, `.bin` model files
- raw logs
- screenshots containing private data
- credentials or tokens
