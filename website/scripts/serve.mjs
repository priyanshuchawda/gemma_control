import { createReadStream, existsSync } from "node:fs";
import { stat } from "node:fs/promises";
import { createServer } from "node:http";
import { dirname, extname, join, normalize, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = dirname(fileURLToPath(new URL("../package.json", import.meta.url)));
const dist = resolve(root, "dist");
const port = Number(process.env.PORT ?? 4173);

const contentTypes = new Map([
  [".html", "text/html; charset=utf-8"],
  [".css", "text/css; charset=utf-8"],
  [".js", "text/javascript; charset=utf-8"],
  [".map", "application/json; charset=utf-8"],
  [".webp", "image/webp"],
  [".md", "text/markdown; charset=utf-8"],
  [".apk", "application/vnd.android.package-archive"]
]);

const server = createServer(async (request, response) => {
  const requestUrl = new URL(request.url ?? "/", `http://localhost:${port}`);
  const pathname = requestUrl.pathname === "/" ? "/index.html" : requestUrl.pathname;
  const candidate = normalize(join(dist, decodeURIComponent(pathname)));
  const rel = relative(dist, candidate);

  if (rel.startsWith("..") || rel === "" && pathname !== "/index.html") {
    response.writeHead(403);
    response.end("Forbidden");
    return;
  }

  if (!existsSync(candidate) || !(await stat(candidate)).isFile()) {
    response.writeHead(404);
    response.end("Not found");
    return;
  }

  response.writeHead(200, {
    "Content-Type": contentTypes.get(extname(candidate)) ?? "application/octet-stream",
    "X-Content-Type-Options": "nosniff"
  });
  createReadStream(candidate).pipe(response);
});

server.listen(port, () => {
  console.log(`GemmaControl website preview: http://localhost:${port}`);
});
