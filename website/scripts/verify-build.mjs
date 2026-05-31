import { access, readFile, readdir, stat } from "node:fs/promises";
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
  "downloads/README.md",
  // Legacy stubs (kept for backward compat)
  "styles/base.css",
  "styles/header.css",
  "styles/hero.css",
  "styles/sections.css",
  "styles/responsive.css",
  // New domain-specific partials
  "styles/tokens.css",
  "styles/reset.css",
  "styles/animations.css",
  "styles/layout.css",
];

for (const file of requiredFiles) {
  await access(join(dist, file));
}

const html = await readFile(join(dist, "index.html"), "utf8");
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

const compiledScript = await readCompiledScript(join(dist, "assets"));

if (!compiledScript.includes("GemmaControl.apk")) {
  throw new Error("Compiled site script does not include the APK download entry.");
}

if (!compiledScript.includes("renderDownloads")) {
  throw new Error("Compiled site script does not include the download renderer.");
}

if (iconStats.size <= 0) {
  throw new Error("Copied app icon is empty.");
}

console.log("PASS | website-build-verify | ok");

async function readCompiledScript(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const scripts = await Promise.all(
    entries
      .filter((entry) => entry.isFile() && entry.name.endsWith(".js"))
      .map((entry) => readFile(join(directory, entry.name), "utf8"))
  );
  return scripts.join("\n");
}
