import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, test } from "vitest";

const frontendDir = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const repoRoot = path.resolve(frontendDir, "..");
const policy = path.join(
  repoRoot,
  "projects/analyzer-harness/playwright-project-policy.sh",
);
const harnessBuild = path.join(repoRoot, "projects/analyzer-harness/build.sh");

const runPolicy = (command) =>
  execFileSync("bash", ["-c", `source "$1"; ${command}`, "test", policy], {
    cwd: repoRoot,
    encoding: "utf8",
  }).trim();

describe("analyzer harness Playwright project policy", () => {
  test("uses visible checkpoint stories for non-video parity", () => {
    expect(runPolicy('resolve_harness_playwright_project "parity" ""')).toBe(
      "harness-demo",
    );
  });

  test("reserves video mode for the final demo flow", () => {
    expect(runPolicy('resolve_harness_playwright_project "video" ""')).toBe(
      "harness-demo-video",
    );
  });

  test("accepts the current foundational analyzer stories", () => {
    expect(
      runPolicy('validate_harness_playwright_project "harness-foundational"'),
    ).toBe("harness-foundational");
  });
  test("local build includes every branch-owned harness service", () => {
    const buildScript = readFileSync(harnessBuild, "utf8");

    expect(buildScript).toContain(
      "build oe.openelis.org frontend.openelis.org openelis-analyzer-bridge astm-simulator",
    );
  });
});
