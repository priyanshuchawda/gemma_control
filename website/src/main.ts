import { downloadItems } from "./downloads.js";
import { bindHeaderScrollState } from "./header.js";
import { renderDownloads } from "./render-downloads.js";
import { initScrollReveal } from "./scroll-reveal.js";

function init(): void {
  renderDownloads(downloadItems);
  bindHeaderScrollState();
  initScrollReveal();
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", init, { once: true });
} else {
  init();
}
