import type { DownloadKind, DownloadStatus } from "./types.js";
import { createElement } from "./dom.js";

const SVG_NAMESPACE = "http://www.w3.org/2" + "000/svg";

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

export function createSvgIcon(kind: DownloadKind): SVGSVGElement {
  const svg = createSvg();
  for (const d of iconPaths[kind]) {
    svg.append(createPath(d));
  }
  return svg;
}

export function createActionIcon(status: DownloadStatus): HTMLSpanElement {
  const icon = createElement("span", "button-icon");
  icon.setAttribute("aria-hidden", "true");
  const svg = createSvg();
  svg.append(createPath(status === "available" ? "M12 3v11m0 0 4-4m-4 4-4-4M5 19h14" : "M6 12h12"));
  icon.append(svg);
  return icon;
}

function createSvg(): SVGSVGElement {
  const svg = document.createElementNS(SVG_NAMESPACE, "svg") as SVGSVGElement;
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("aria-hidden", "true");
  svg.setAttribute("focusable", "false");
  return svg;
}

function createPath(d: string): SVGPathElement {
  const path = document.createElementNS(SVG_NAMESPACE, "path") as SVGPathElement;
  path.setAttribute("d", d);
  return path;
}
