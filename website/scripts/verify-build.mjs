import { access, readFile, stat } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = dirname(fileURLToPath(new URL("../package.json", import.meta.url)));
const dist = join(root, "dist");

const requiredFiles = [
  "index.html",
  "styles.css",
  "assets/main.js",
  "assets/main.js.map",
  "assets/app-icon.webp",
  "downloads/README.md"
];

for (const file of requiredFiles) {
  await access(join(dist, file));
}

const html = await readFile(join(dist, "index.html"), "utf8");
const script = await readFile(join(dist, "assets", "main.js"), "utf8");
const iconStats = await stat(join(dist, "assets", "app-icon.webp"));

const requiredHtml = [
  "GemmaControl",
  "Download APK",
  "assets/main.js",
  "styles.css",
  "Content-Security-Policy"
];

for (const marker of requiredHtml) {
  if (!html.includes(marker)) {
    throw new Error(`Missing expected HTML marker: ${marker}`);
  }
}

if (!script.includes("GemmaControl.apk")) {
  throw new Error("Compiled site script does not include the APK download entry.");
}

if (iconStats.size <= 0) {
  throw new Error("Copied app icon is empty.");
}

console.log("PASS | website-build-verify | ok");
