import { cp, mkdir, copyFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = dirname(fileURLToPath(new URL("../package.json", import.meta.url)));
const dist = join(root, "dist");

await mkdir(dist, { recursive: true });
await copyFile(join(root, "index.html"), join(dist, "index.html"));
await copyFile(join(root, "src", "styles.css"), join(dist, "styles.css"));

const publicDir = join(root, "public");
if (existsSync(publicDir)) {
  await cp(publicDir, dist, { recursive: true, force: true });
}
