import type { DownloadItem } from "./types.js";

const GH_RELEASE = "https://github.com/priyanshuchawda/gemma_control/releases/latest/download";

export const downloadItems: readonly DownloadItem[] = [
  {
    id: "android-apk",
    kind: "android",
    title: "Android APK",
    eyebrow: "Recommended",
    description:
      "Install GemmaControl directly on any Android 16 device. Debug build — sideloading required. Enable Unknown Sources in your browser or file manager.",
    href: `${GH_RELEASE}/GemmaControl-v1.0.0-debug.apk`,
    fileName: "GemmaControl.apk",
    status: "available",
    primary: true,
    meta: ["v1.0.0 · Debug build", "Android 16 (API 36)", "arm64-v8a · ~71 MB", "Offline · AES-GCM encrypted"],
    secondaryHref: "https://github.com/priyanshuchawda/gemma_control/releases/latest",
    secondaryLabel: "View on GitHub →"
  },
  {
    id: "ai-model",
    kind: "model",
    title: "FunctionGemma Model",
    eyebrow: "AI Model",
    description:
      "The FunctionGemma 270M Q8 quantised model binary for LiteRT-LM. Download separately and place it via the app Settings screen. SHA-256 verified on load.",
    href: `${GH_RELEASE}/mobile_actions_q8_ekv1024.litertlm`,
    fileName: "mobile_actions_q8_ekv1024.litertlm",
    status: "available",
    primary: false,
    meta: ["270M parameters · Q8 quantised", "LiteRT-LM SDK format", "EKV 1024 · ~276 MB", "100% on-device inference"],
    secondaryHref: "https://github.com/priyanshuchawda/gemma_control/blob/main/docs/ARCHITECTURE.md",
    secondaryLabel: "Architecture docs →"
  },
  {
    id: "source",
    kind: "source",
    title: "Source Code",
    eyebrow: "Open Source",
    description:
      "Full Kotlin + Jetpack Compose Android source. Review the MVVM architecture, notification parsers, LiteRT engine, and every security boundary before you build.",
    href: "https://github.com/priyanshuchawda/gemma_control",
    fileName: "gemma_control",
    status: "available",
    primary: false,
    meta: ["Kotlin · Jetpack Compose", "Clean MVVM architecture", "MIT License · Open Source", "16 AI tool functions"],
    secondaryHref: "https://github.com/priyanshuchawda/gemma_control/issues",
    secondaryLabel: "Report an issue →"
  }
];
