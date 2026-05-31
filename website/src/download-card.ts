import { createElement } from "./dom.js";
import { createActionIcon, createSvgIcon } from "./icons.js";
import type { DownloadItem } from "./types.js";

export function createDownloadCard(item: DownloadItem): HTMLElement {
  const card = createElement("article", `download-card${item.primary ? " primary" : ""}`);
  card.id = `download-card-${item.id}`;
  card.append(
    createCardHeader(item),
    createElement("p", "download-description", item.description),
    createMetaList(item.meta),
    createActions(item)
  );
  return card;
}

function createCardHeader(item: DownloadItem): HTMLElement {
  const header = createElement("div", "download-card-header");
  const icon = createElement("span", "download-icon");
  const headingGroup = createElement("div");
  icon.append(createSvgIcon(item.kind));
  headingGroup.append(createElement("span", "card-eyebrow", item.eyebrow));
  headingGroup.append(createElement("h3", undefined, item.title));
  header.append(icon, headingGroup);
  return header;
}

function createMetaList(metaItems: readonly string[]): HTMLUListElement {
  const meta = createElement("ul", "download-meta");
  for (const m of metaItems) meta.append(createElement("li", undefined, m));
  return meta;
}

function createActions(item: DownloadItem): HTMLElement {
  const row = createElement("div", "download-actions");
  row.append(createPrimaryAction(item));
  if (item.secondaryHref && item.secondaryLabel) {
    row.append(createSecondaryAction(item.secondaryHref, item.secondaryLabel));
  }
  return row;
}

function createPrimaryAction(item: DownloadItem): HTMLAnchorElement {
  const a = document.createElement("a");
  a.className = item.primary ? "button button-primary" : "button button-ghost";
  a.href = item.href;
  a.setAttribute("aria-label", `${item.status === "available" ? "Download" : "View"} ${item.title}`);
  a.id = `download-btn-${item.id}`;

  if (item.status === "available" && item.kind !== "source") {
    a.setAttribute("download", item.fileName);
  }
  if (item.kind === "source") {
    a.setAttribute("target", "_blank");
    a.setAttribute("rel", "noopener");
  }

  if (item.status === "planned") {
    a.setAttribute("aria-disabled", "true");
    a.tabIndex = -1;
  }

  a.append(createActionIcon(item.status));
  a.append(document.createTextNode(downloadLabel(item)));
  return a;
}

function createSecondaryAction(href: string, label: string): HTMLAnchorElement {
  const a = document.createElement("a");
  a.className = "text-link";
  a.href = href;
  a.textContent = label;
  if (href.startsWith("http")) {
    a.setAttribute("target", "_blank");
    a.setAttribute("rel", "noopener");
  }
  return a;
}

function downloadLabel(item: DownloadItem): string {
  if (item.status !== "available") return "Not available";
  switch (item.kind) {
    case "android": return "Download APK";
    case "model":   return "Download Model";
    case "source":  return "View Source";
  }
}
