import type { DownloadKind, DownloadStatus } from "./types.js";
import { createElement } from "./dom.js";

const SVG_NS = "http://www.w3.org/2000/svg";

const iconPaths: Record<DownloadKind, readonly string[]> = {
  android: [
    "M8 7h8",
    "M7 9v8a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2V9",
    "M9 5 7.5 3.5",
    "M15 5l1.5-1.5",
    "M10 13h.01",
    "M14 13h.01"
  ],
  model: [
    "M12 2L2 7l10 5 10-5-10-5z",
    "M2 17l10 5 10-5",
    "M2 12l10 5 10-5"
  ],
  source: [
    "M8 18 3 12l5-6",
    "M16 6l5 6-5 6",
    "M14 4l-4 16"
  ]
};

export function createSvgIcon(kind: DownloadKind): SVGSVGElement {
  const svg = makeSvg();
  for (const d of iconPaths[kind]) svg.append(makePath(d));
  return svg;
}

export function createActionIcon(status: DownloadStatus): HTMLSpanElement {
  const icon = createElement("span", "button-icon");
  icon.setAttribute("aria-hidden", "true");
  const svg = makeSvg();
  svg.append(makePath(
    status === "available"
      ? "M12 3v11m0 0 4-4m-4 4-4-4M5 19h14"
      : "M10 6h4M12 14v4M8 6l1 8h6l1-8"
  ));
  icon.append(svg);
  return icon;
}

function makeSvg(): SVGSVGElement {
  const svg = document.createElementNS(SVG_NS, "svg") as SVGSVGElement;
  svg.setAttribute("viewBox", "0 0 24 24");
  svg.setAttribute("aria-hidden", "true");
  svg.setAttribute("focusable", "false");
  return svg;
}

function makePath(d: string): SVGPathElement {
  const path = document.createElementNS(SVG_NS, "path") as SVGPathElement;
  path.setAttribute("d", d);
  return path;
}
