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

The website download card points to:

```text
downloads/GemmaControl.apk
```

For a local static build:

1. Build or obtain a signed APK outside git.
2. Place it at `website/public/downloads/GemmaControl.apk`.
3. Run `npm run build`.
4. Serve or upload `website/dist/`.

For production, prefer GitHub Releases or another release artifact store. If the canonical download URL changes, update the Android APK entry in `website/src/main.ts`.

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
