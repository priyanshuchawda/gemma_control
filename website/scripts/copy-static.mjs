import { cp, mkdir, copyFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const root = dirname(fileURLToPath(new URL("../package.json", import.meta.url)));
const dist = join(root, "dist");

await mkdir(dist, { recursive: true });
await copyFile(join(root, "src", "styles.css"), join(dist, "styles.css"));
await cp(join(root, "src", "styles"), join(dist, "styles"), { recursive: true, force: true });

const publicDir = join(root, "public");
if (existsSync(publicDir)) {
  await cp(publicDir, dist, { recursive: true, force: true });
}
