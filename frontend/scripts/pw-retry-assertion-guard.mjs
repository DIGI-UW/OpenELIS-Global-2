#!/usr/bin/env node
/**
 * Playwright retry-assertion guard — blocks the non-retrying
 * "query-then-assert" anti-pattern:
 *
 *   const rowCount = await rows.count();       // one-shot DOM snapshot
 *   expect(rowCount).toBeGreaterThan(0);       // non-retrying assert on raw value
 *
 * These patterns flake on cold CI runners whenever the DOM/XHR hasn't
 * completed at the moment the query fires. Playwright's web-first
 * assertions (`await expect(locator).toHaveCount(n)`, `.toBeVisible()`,
 * `.toHaveText()`, `.toBeEnabled()`) auto-retry and are the correct
 * replacement.
 *
 * Flagged methods (all return a non-retrying, point-in-time snapshot):
 *   count, innerText, textContent, getAttribute, inputValue, isVisible,
 *   isEnabled, isChecked, isEditable, isHidden, isDisabled.
 *
 * Detection: `const NAME = await ...<method>(...)` followed within the
 * next 15 lines by `expect(NAME)...`. If both appear, it's a violation.
 *
 * NOT flagged (intentional — these aren't this anti-pattern):
 *   - `.count()` used as a loop bound or index (no `expect(var)` follows).
 *   - `.textContent()` / `.innerText()` whose value is returned or used
 *     in flow-control branching (no `expect(var)` follows).
 *   - `.isVisible()` / `.isEnabled()` used in `if (await foo.isX()) { ... }`
 *     flow control (no binding captured). This is a different anti-pattern
 *     addressed case-by-case; this guard doesn't tackle it.
 *
 * Mode: STRICT (exits 1 on any violation). See Constitution V.6
 * (Test Quality Invariants) and .specify/guides/playwright-best-practices.md
 * §"Use Auto-Retrying Assertions".
 */

import fs from "fs";
import path from "path";

const root = path.resolve(process.cwd(), "playwright");

const FLAGGED_METHODS = [
  "count",
  "innerText",
  "textContent",
  "getAttribute",
  "inputValue",
  "isVisible",
  "isEnabled",
  "isChecked",
  "isEditable",
  "isHidden",
  "isDisabled",
];

// Window in which to look for the follow-up expect(var) after the capture.
// 15 lines is a conservative scope for a single test-step block.
const LOOKAHEAD = 15;

const captureRe = new RegExp(
  `\\bconst\\s+(\\w+)\\s*=\\s*\\(?\\s*await\\s+.*\\.(${FLAGGED_METHODS.join("|")})\\s*\\(`,
);

const violations = [];

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full);
      continue;
    }
    if (!full.endsWith(".ts")) continue;
    const content = fs.readFileSync(full, "utf8");
    const lines = content.split("\n");

    for (let i = 0; i < lines.length; i += 1) {
      const m = lines[i].match(captureRe);
      if (!m) continue;
      const varName = m[1];
      const method = m[2];

      // Build a regex that matches `expect(varName` or `expect.soft(varName` etc.
      // with the variable as the first argument.
      const expectRe = new RegExp(
        `\\bexpect(?:\\.\\w+)?\\s*\\(\\s*${varName}\\s*[,)]`,
      );

      const end = Math.min(i + LOOKAHEAD, lines.length - 1);
      for (let j = i + 1; j <= end; j += 1) {
        if (expectRe.test(lines[j])) {
          violations.push({
            file: path.relative(process.cwd(), full),
            captureLine: i + 1,
            expectLine: j + 1,
            varName,
            method,
          });
          break;
        }
      }
    }
  }
}

walk(root);

if (violations.length > 0) {
  console.error(
    "Disallowed non-retrying query-then-assert pattern found. Replace " +
      "`const X = await locator.<method>(); expect(X)...` with Playwright's " +
      "auto-retrying web-first assertions (e.g. `await expect(locator).toHaveCount(n)`, " +
      "`.toBeVisible()`, `.toHaveText()`, `.toBeEnabled()`).\n",
  );
  for (const v of violations) {
    console.error(
      `  - ${v.file}:${v.captureLine} — const ${v.varName} = await ….${v.method}() ` +
        `→ expect(${v.varName}) on line ${v.expectLine}`,
    );
  }
  console.error(
    "\nSee .specify/guides/playwright-best-practices.md " +
      "§'Use Auto-Retrying Assertions' and Constitution V.6.",
  );
  process.exit(1);
}

console.log("Playwright retry-assertion guard passed.");
