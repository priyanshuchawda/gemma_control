# GemmaControl Download Website

The download website lives in `website/`. It is a static TypeScript site with no runtime framework and no deployment coupling.

## Purpose

- Give users a polished place to download the Android APK.
- Explain install requirements clearly before users sideload the app.
- Keep privacy and safety claims consistent with the Android app.
- Avoid committing raw APKs, AABs, EXEs, model binaries, logs, private screenshots, or credentials.

## Build

```powershell
cd website
npm run verify
```

This runs:

- TypeScript strict typecheck.
- Static build into `website/dist/`.
- Build verifier for expected HTML, JavaScript, icon, and download metadata.

## Local Preview

```powershell
cd website
npm run serve
```

Open:

```text
http://localhost:4173
```

## APK Distribution

The website download buttons and TypeScript download cards use the canonical GitHub Releases URLs in:

```text
website/src/release-links.ts
```

The current APK target is:

```text
https://github.com/priyanshuchawda/gemma_control/releases/latest/download/GemmaControl-v1.0.0-debug.apk
```

When a release artifact name changes, update `website/src/release-links.ts` and run `npm run verify`. Do not commit APKs, AABs, EXEs, MSIs, model binaries, raw logs, private screenshots, or credentials.

## Artifact Safety

`.gitignore` blocks:

- `*.apk`
- `*.aab`
- `*.exe`
- `*.msi`
- `*.litertlm`
- `*.tflite`
- `*.bin`
- `*.log`
- local private screenshots
- `website/dist/`

Do not bypass these ignores for release binaries. Use release storage instead.
