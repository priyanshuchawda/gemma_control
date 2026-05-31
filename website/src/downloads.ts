import type { DownloadItem } from "./types.js";

export const downloadItems: readonly DownloadItem[] = [
  {
    id: "android-apk",
    kind: "android",
    title: "Android APK",
    eyebrow: "Recommended",
    description:
      "Install GemmaControl directly on a supported Android phone. Use a signed APK for real users.",
    href: "downloads/GemmaControl.apk",
    fileName: "GemmaControl.apk",
    status: "available",
    primary: true,
    meta: ["Version 1.0", "Android 16 target", "Local-only workflows"],
    secondaryHref: "https://github.com/priyanshuchawda/gemma_control/releases/latest",
    secondaryLabel: "GitHub release"
  },
  {
    id: "windows-installer",
    kind: "windows",
    title: "Windows installer",
    eyebrow: "Not available",
    description:
      "GemmaControl is currently a native Android app. A Windows package should stay disabled until a real desktop build exists.",
    href: "#support",
    fileName: "GemmaControl.exe",
    status: "planned",
    primary: false,
    meta: ["No desktop build yet", "Do not upload placeholder EXEs"]
  },
  {
    id: "source",
    kind: "source",
    title: "Source code",
    eyebrow: "Developers",
    description:
      "Review the Android project, docs, and local verification workflow before building your own artifact.",
    href: "https://github.com/priyanshuchawda/gemma_control",
    fileName: "gemma_control",
    status: "available",
    primary: false,
    meta: ["Kotlin", "Jetpack Compose", "LiteRT-LM"]
  }
];
