import type { DownloadItem } from "./types.js";
import {
  ANDROID_APK_DOWNLOAD_NAME,
  ANDROID_APK_DOWNLOAD_URL,
  ARCHITECTURE_DOC_URL,
  FUNCTION_GEMMA_MODEL_DOWNLOAD_URL,
  FUNCTION_GEMMA_MODEL_FILE_NAME,
  ISSUES_URL,
  RELEASES_URL,
  REPOSITORY_URL
} from "./release-links.js";

export const downloadItems: readonly DownloadItem[] = [
  {
    id: "android-apk",
    kind: "android",
    title: "Android APK",
    eyebrow: "Recommended",
    description:
      "Install GemmaControl directly on any Android 16 device. Debug build — sideloading required. Enable Unknown Sources in your browser or file manager.",
    href: ANDROID_APK_DOWNLOAD_URL,
    fileName: ANDROID_APK_DOWNLOAD_NAME,
    status: "available",
    primary: true,
    meta: ["v1.0.0 · Debug build", "Android 16 (API 36)", "arm64-v8a · ~71 MB", "Offline · AES-GCM encrypted"],
    secondaryHref: `${RELEASES_URL}/latest`,
    secondaryLabel: "View on GitHub →"
  },
  {
    id: "ai-model",
    kind: "model",
    title: "FunctionGemma Model",
    eyebrow: "AI Model",
    description:
      "The FunctionGemma 270M Q8 quantised model binary for LiteRT-LM. Download separately and place it via the app Settings screen. SHA-256 verified on load.",
    href: FUNCTION_GEMMA_MODEL_DOWNLOAD_URL,
    fileName: FUNCTION_GEMMA_MODEL_FILE_NAME,
    status: "available",
    primary: false,
    meta: ["270M parameters · Q8 quantised", "LiteRT-LM SDK format", "EKV 1024 · ~276 MB", "100% on-device inference"],
    secondaryHref: ARCHITECTURE_DOC_URL,
    secondaryLabel: "Architecture docs →"
  },
  {
    id: "source",
    kind: "source",
    title: "Source Code",
    eyebrow: "Open Source",
    description:
      "Full Kotlin + Jetpack Compose Android source. Review the MVVM architecture, notification parsers, LiteRT engine, and every security boundary before you build.",
    href: REPOSITORY_URL,
    fileName: "gemma_control",
    status: "available",
    primary: false,
    meta: ["Kotlin · Jetpack Compose", "Clean MVVM architecture", "MIT License · Open Source", "16 AI tool functions"],
    secondaryHref: ISSUES_URL,
    secondaryLabel: "Report an issue →"
  }
];
