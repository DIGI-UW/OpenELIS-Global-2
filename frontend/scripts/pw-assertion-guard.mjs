#!/usr/bin/env node
/**
 * Playwright assertion guard — warns about test() and test.step() blocks
 * that lack expect() calls. Tests without assertions provide zero
 * regression protection (Constitution V.6, Rule E1).
 *
 * Mode: INFORMATIONAL — prints warnings but does NOT fail the build.
 * Scope: Only reports on files changed in the current branch diff vs develop.
 *        Pre-existing violations in unchanged files are not reported.
 *
 * Known caveats:
 * - Checks for literal `expect(` in the block text. If assertions are inside
 *   helper functions (e.g., showTitleCard, evidence), this guard will flag the
 *   block as assertion-free even though the helper asserts internally.
 * - Title card steps and video-only steps are legitimate assertion-free blocks
 *   in demo specs. These are expected warnings, not bugs.
 * - Brace-counting is approximate; deeply nested template literals with braces
 *   may confuse the parser.
 *
 * To make this strict (blocking), change STRICT_MODE to true.
 */
import { readFileSync, readdirSync, statSync } from "fs";
import { join, relative } from "path";
import { execFileSync } from "child_process";

const STRICT_MODE = false;
const SPEC_DIR = join(process.cwd(), "playwright");

// Get files changed in current branch vs develop (or fallback to all)
let changedFiles = null;
try {
  const diffOutput = execFileSync(
    "git",
    [
      "diff",
      "--name-only",
      "origin/develop..HEAD",
      "--",
      "frontend/playwright/",
    ],
    { encoding: "utf-8" },
  ).trim();
  if (diffOutput) {
    // git returns paths relative to repo root (frontend/playwright/...),
    // strip the "frontend/" prefix since this script runs from frontend/
    changedFiles = new Set(
      diffOutput.split("\n").map((f) => f.trim().replace(/^frontend\//, "")),
    );
  }
} catch {
  // If git diff fails (e.g., no origin/develop), scan all files
  changedFiles = null;
}

const violations = [];

function scanDir(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      scanDir(full);
    } else if (full.endsWith(".spec.ts")) {
      const rel = relative(process.cwd(), full);
      // Only check files in the branch diff (or all if diff unavailable)
      if (changedFiles === null || changedFiles.has(rel)) {
        checkFile(full);
      }
    }
  }
}

function checkFile(filePath) {
  const content = readFileSync(filePath, "utf-8");
  const lines = content.split("\n");
  const rel = relative(process.cwd(), filePath);

  let inBlock = null;
  let braceDepth = 0;
  let blockStartDepth = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    const stepMatch = line.match(/test\.step\(\s*["'`]([^"'`]+)["'`]/);
    const testMatch = line.match(/\btest\(\s*["'`]([^"'`]+)["'`]/);

    if (stepMatch && !inBlock) {
      inBlock = {
        type: "test.step",
        name: stepMatch[1],
        line: i + 1,
        hasExpect: false,
      };
      blockStartDepth = braceDepth;
    } else if (testMatch && !inBlock) {
      inBlock = {
        type: "test",
        name: testMatch[1],
        line: i + 1,
        hasExpect: false,
      };
      blockStartDepth = braceDepth;
    }

    for (const ch of line) {
      if (ch === "{") braceDepth++;
      if (ch === "}") {
        braceDepth--;
        if (inBlock && braceDepth <= blockStartDepth) {
          if (!inBlock.hasExpect) {
            violations.push(
              `  - ${rel}:${inBlock.line} — ${inBlock.type}("${inBlock.name}") has no expect()`,
            );
          }
          inBlock = null;
        }
      }
    }

    if (inBlock && line.includes("expect(")) {
      inBlock.hasExpect = true;
    }
  }
}

scanDir(SPEC_DIR);

if (violations.length > 0) {
  console.warn(
    `⚠ Assertion guard: ${violations.length} test block(s) without expect():`,
  );
  violations.forEach((v) => console.warn(v));
  if (STRICT_MODE) {
    console.error("Failing build (STRICT_MODE=true).");
    process.exit(1);
  } else {
    console.warn("(informational only — not blocking build)");
  }
} else {
  console.log("Playwright assertion guard passed.");
}
