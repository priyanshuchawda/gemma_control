import { downloadItems } from "./downloads.js";
import { bindHeaderScrollState } from "./header.js";
import { renderDownloads } from "./render-downloads.js";

function init(): void {
  renderDownloads(downloadItems);
  bindHeaderScrollState();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init, { once: true });
} else {
  init();
}
