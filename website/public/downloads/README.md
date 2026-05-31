# Download Artifacts

Place distributable binaries here only when preparing a local/static website build.

Expected Android filename:

```text
GemmaControl.apk
```

Do not commit APK, AAB, EXE, MSI, model binaries, raw logs, private screenshots, or credentials. The repository `.gitignore` excludes these artifacts. For production distribution, prefer GitHub Releases or another release artifact store and update the website download manifest in `website/src/main.ts`.
