type DownloadStatus = "available" | "planned";
type DownloadKind = "android" | "windows" | "source";

interface DownloadItem {
  readonly id: string;
  readonly kind: DownloadKind;
  readonly title: string;
  readonly eyebrow: string;
  readonly description: string;
  readonly href: string;
  readonly fileName: string;
  readonly status: DownloadStatus;
  readonly primary: boolean;
  readonly meta: readonly string[];
  readonly secondaryHref?: string;
  readonly secondaryLabel?: string;
}

const downloadItems: readonly DownloadItem[] = [
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

const iconPaths: Record<DownloadKind, readonly string[]> = {
  android: [
    "M8 7h8",
    "M7 9v8a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9",
    "M9 5 7.5 3.5",
    "M15 5l1.5-1.5",
    "M10 13h.01",
    "M14 13h.01"
  ],
  windows: [
    "M4 5.5 10.5 4v7H4z",
    "M12 3.7 20 2v9h-8z",
    "M4 13h6.5v7L4 18.5z",
    "M12 13h8v9l-8-1.7z"
  ],
  source: [
    "M8 18 3 12l5-6",
    "M16 6l5 6-5 6",
    "M14 4l-4 16"
  ]
};

function createElement<K extends keyof HTMLElementTagNameMap>(
  tag: K,
  className?: string,
  text?: string
): HTMLElementTagNameMap[K] {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text) element.textContent = text;
  return element;
}

function createSvgIcon(kind: DownloadKind): SVGSVGElement {
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("aria-hidden", "true");
  svg.setAttribute("focusable", "false");

  for (const d of iconPaths[kind]) {
    const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("d", d);
    svg.append(path);
  }

  return svg;
}

function createDownloadCard(item: DownloadItem): HTMLElement {
  const card = createElement("article", `download-card${item.primary ? " primary" : ""}`);

  const header = createElement("div", "download-card-header");
  const icon = createElement("span", "download-icon");
  icon.append(createSvgIcon(item.kind));
  const headingGroup = createElement("div");
  headingGroup.append(createElement("span", "card-eyebrow", item.eyebrow));
  headingGroup.append(createElement("h3", undefined, item.title));
  header.append(icon, headingGroup);

  const description = createElement("p", "download-description", item.description);

  const meta = createElement("ul", "download-meta");
  for (const metaItem of item.meta) {
    meta.append(createElement("li", undefined, metaItem));
  }

  const actionRow = createElement("div", "download-actions");
  const action = document.createElement("a");
  action.className = item.primary ? "button button-primary" : "button button-secondary";
  action.href = item.href;
  action.setAttribute("aria-label", `${item.status === "available" ? "Download" : "View"} ${item.title}`);
  if (item.status === "available" && item.kind !== "source") {
    action.setAttribute("download", item.fileName);
  }
  if (item.status === "planned") {
    action.setAttribute("aria-disabled", "true");
    action.tabIndex = -1;
  }
  action.append(createActionIcon(item.status));
  action.append(document.createTextNode(item.status === "available" ? downloadLabel(item) : "Not available"));
  actionRow.append(action);

  if (item.secondaryHref && item.secondaryLabel) {
    const secondary = document.createElement("a");
    secondary.className = "text-link";
    secondary.href = item.secondaryHref;
    secondary.textContent = item.secondaryLabel;
    actionRow.append(secondary);
  }

  card.append(header, description, meta, actionRow);
  return card;
}

function createActionIcon(status: DownloadStatus): HTMLSpanElement {
  const icon = createElement("span", "button-icon");
  icon.setAttribute("aria-hidden", "true");
  const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
  svg.setAttribute("viewBox", "0 0 24 24");
  const path = document.createElementNS("http://www.w3.org/2000/svg", "path");
  path.setAttribute("d", status === "available" ? "M12 3v11m0 0 4-4m-4 4-4-4M5 19h14" : "M6 12h12");
  svg.append(path);
  icon.append(svg);
  return icon;
}

function downloadLabel(item: DownloadItem): string {
  switch (item.kind) {
    case "android":
      return "Download APK";
    case "windows":
      return "Download EXE";
    case "source":
      return "Open source";
  }
}

function renderDownloads(): void {
  const grid = document.querySelector<HTMLElement>("#download-grid");
  if (!grid) return;

  const fragment = document.createDocumentFragment();
  for (const item of downloadItems) {
    fragment.append(createDownloadCard(item));
  }
  grid.replaceChildren(fragment);
}

function updateHeaderOnScroll(): void {
  const header = document.querySelector<HTMLElement>(".site-header");
  if (!header) return;

  const onScroll = (): void => {
    header.toggleAttribute("data-scrolled", window.scrollY > 8);
  };

  onScroll();
  window.addEventListener("scroll", onScroll, { passive: true });
}

function init(): void {
  renderDownloads();
  updateHeaderOnScroll();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init, { once: true });
} else {
  init();
}
