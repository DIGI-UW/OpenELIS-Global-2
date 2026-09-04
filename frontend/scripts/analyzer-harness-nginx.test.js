import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, test } from "vitest";

const scriptsDirectory = path.dirname(fileURLToPath(import.meta.url));
const nginxTemplatePath = path.resolve(
  scriptsDirectory,
  "../../volume/nginx/nginx.conf.template",
);

function locationBlocks(source) {
  const blocks = [];
  const marker = "location / {";
  let searchFrom = 0;

  while (searchFrom < source.length) {
    const start = source.indexOf(marker, searchFrom);
    if (start === -1) {
      break;
    }

    const openingBrace = source.indexOf("{", start);
    let depth = 0;
    let end = openingBrace;
    for (; end < source.length; end += 1) {
      if (source[end] === "{") {
        depth += 1;
      } else if (source[end] === "}") {
        depth -= 1;
        if (depth === 0) {
          end += 1;
          break;
        }
      }
    }

    blocks.push(source.slice(start, end));
    searchFrom = end;
  }

  return blocks;
}

describe("analyzer harness nginx template", () => {
  test("forwards Vite HMR WebSocket upgrades through every frontend proxy", () => {
    const template = fs.readFileSync(nginxTemplatePath, "utf8");
    const frontendBlocks = locationBlocks(template).filter((block) =>
      block.includes("proxy_pass http://frontend.openelis.org;"),
    );

    expect(template).toContain("map $http_upgrade $connection_upgrade");
    expect(frontendBlocks).toHaveLength(2);
    for (const block of frontendBlocks) {
      expect(block).toContain("proxy_http_version 1.1;");
      expect(block).toContain("proxy_set_header Upgrade $http_upgrade;");
      expect(block).toContain(
        "proxy_set_header Connection $connection_upgrade;",
      );
    }
  });
});
