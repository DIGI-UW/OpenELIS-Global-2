import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, test } from "vitest";

const scriptsDirectory = path.dirname(fileURLToPath(import.meta.url));
const nginxConfigPath = path.resolve(scriptsDirectory, "../nginx/nginx.conf");

function locationBlock(source, marker) {
  const start = source.indexOf(marker);
  if (start === -1) {
    return null;
  }
  const end = source.indexOf("}", start);
  return source.slice(start, end + 1);
}

/**
 * Checks the served configuration text only; it does not run nginx. The
 * fallback assertion matters because only the last try_files parameter is an
 * internal redirect: an SPA deep link reaches the index.html location, and
 * its header, only when /index.html is that last parameter.
 */
describe("frontend nginx configuration", () => {
  const config = fs.readFileSync(nginxConfigPath, "utf8");

  test("makes browsers revalidate index.html on every load", () => {
    const indexBlock = locationBlock(config, "location = /index.html {");

    expect(indexBlock).not.toBeNull();
    expect(indexBlock).toContain('add_header Cache-Control "no-cache" always;');
  });

  test("serves SPA deep links through the index.html location", () => {
    const fallbackBlock = locationBlock(config, "location / {");

    expect(fallbackBlock).toMatch(/try_files \$uri \$uri\/ \/index\.html;/);
  });
});
