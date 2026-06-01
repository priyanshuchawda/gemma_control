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
index.html               # Small build-time HTML include template
src/partials/            # Static page sections split by page area
src/main.ts              # Page bootstrap only
src/release-links.ts     # Canonical release and repository URLs
src/downloads.ts         # Download card data
src/download-card.ts     # Download card DOM rendering
src/icons.ts             # Inline SVG icon helpers
src/header.ts            # Sticky header scroll state
src/styles/              # CSS split by base/header/hero/sections/responsive
scripts/compose-html.mjs # Builds dist/index.html from the template and partials
```

## APK Distribution

The primary Android button points to the GitHub Releases artifact configured in:

```text
website/src/release-links.ts
```

The current canonical APK URL is:

```text
https://github.com/priyanshuchawda/gemma_control/releases/latest/download/GemmaControl-v1.0.0-debug.apk
```

When the release artifact name changes, update `website/src/release-links.ts` and run:

```powershell
npm run build
```

Do not place APKs in git. If you temporarily self-host local artifacts for a static preview, keep them under `website/public/downloads/`; `.gitignore` excludes the binary formats.

## Artifact Rules

Do not commit:

- APK or AAB files
- EXE or MSI installers
- `.litertlm`, `.tflite`, `.bin` model files
- raw logs
- screenshots containing private data
- credentials or tokens
