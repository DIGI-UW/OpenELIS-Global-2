import fs from "fs";
import path from "path";

const root = path.resolve(process.cwd(), "playwright");
const violations = [];

const allowedFiles = new Set([
  path.resolve(root, "helpers/title-card.ts"),
  path.resolve(root, "helpers/video-pause.ts"),
]);

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full);
      continue;
    }
    if (!full.endsWith(".ts")) continue;
    if (allowedFiles.has(full)) continue;
    const content = fs.readFileSync(full, "utf8");
    const lines = content.split("\n");
    lines.forEach((line, idx) => {
      if (line.includes("waitForTimeout(")) {
        violations.push(`${path.relative(process.cwd(), full)}:${idx + 1}`);
      }
    });
  }
}

walk(root);

if (violations.length > 0) {
  console.error("Disallowed waitForTimeout() usage found:");
  violations.forEach((v) => console.error(`  - ${v}`));
  process.exit(1);
}

console.log("Playwright timeout guard passed.");
