import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";

const root = dirname(fileURLToPath(new URL("../package.json", import.meta.url)));
const dist = join(root, "dist");
const partialsDir = join(root, "src", "partials");

const includeMap = new Map([
  ["header", "header.html"],
  ["hero", "hero.html"],
  ["proof", "proof.html"],
  ["features", "features.html"],
  ["download", "download.html"],
  ["privacy", "privacy.html"],
  ["install", "install.html"],
  ["cta", "cta.html"],
  ["footer", "footer.html"]
]);

const releaseLinks = await import(
  pathToFileURL(join(dist, "assets", "release-links.js")).href
);

const placeholderMap = new Map([
  ["ANDROID_APK_DOWNLOAD_URL", releaseLinks.ANDROID_APK_DOWNLOAD_URL],
  ["ARCHITECTURE_DOC_URL", releaseLinks.ARCHITECTURE_DOC_URL],
  ["ISSUES_URL", releaseLinks.ISSUES_URL],
  ["RELEASES_URL", releaseLinks.RELEASES_URL],
  ["REPOSITORY_URL", releaseLinks.REPOSITORY_URL],
  ["SECURITY_DOC_URL", releaseLinks.SECURITY_DOC_URL]
]);

let html = await readFile(join(root, "index.html"), "utf8");

for (const [name, fileName] of includeMap) {
  const marker = `<!-- @include ${name} -->`;
  const partial = await readFile(join(partialsDir, fileName), "utf8");
  html = html.replace(marker, partial.trimEnd());
}

for (const [name, value] of placeholderMap) {
  html = html.replaceAll(`{{${name}}}`, value);
}

const unresolvedIncludes = html.match(/<!-- @include [a-z-]+ -->/g);
if (unresolvedIncludes) {
  throw new Error(`Unresolved HTML includes: ${unresolvedIncludes.join(", ")}`);
}

const unresolvedPlaceholders = html.match(/\{\{[A-Z0-9_]+\}\}/g);
if (unresolvedPlaceholders) {
  throw new Error(`Unresolved HTML placeholders: ${unresolvedPlaceholders.join(", ")}`);
}

await mkdir(dist, { recursive: true });
await writeFile(join(dist, "index.html"), html);

console.log("PASS | website-html-compose | ok");
