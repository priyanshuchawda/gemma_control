import { createDownloadCard } from "./download-card.js";
import type { DownloadItem } from "./types.js";

export function renderDownloads(items: readonly DownloadItem[]): void {
  const grid = document.querySelector<HTMLElement>("#download-grid");
  if (!grid) return;

  const fragment = document.createDocumentFragment();
  for (const item of items) {
    fragment.append(createDownloadCard(item));
  }
  grid.replaceChildren(fragment);
}
