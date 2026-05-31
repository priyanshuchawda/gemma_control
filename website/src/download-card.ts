import { createElement } from "./dom.js";
import { createActionIcon, createSvgIcon } from "./icons.js";
import type { DownloadItem } from "./types.js";

export function createDownloadCard(item: DownloadItem): HTMLElement {
  const card = createElement("article", `download-card${item.primary ? " primary" : ""}`);
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
  for (const metaItem of metaItems) {
    meta.append(createElement("li", undefined, metaItem));
  }
  return meta;
}

function createActions(item: DownloadItem): HTMLElement {
  const actionRow = createElement("div", "download-actions");
  actionRow.append(createPrimaryAction(item));

  if (item.secondaryHref && item.secondaryLabel) {
    actionRow.append(createSecondaryAction(item.secondaryHref, item.secondaryLabel));
  }

  return actionRow;
}

function createPrimaryAction(item: DownloadItem): HTMLAnchorElement {
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
  return action;
}

function createSecondaryAction(href: string, label: string): HTMLAnchorElement {
  const secondary = document.createElement("a");
  secondary.className = "text-link";
  secondary.href = href;
  secondary.textContent = label;
  return secondary;
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
