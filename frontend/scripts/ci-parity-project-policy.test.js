import { execFileSync, spawnSync } from "node:child_process";
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

const runPolicy = (command) =>
  execFileSync("bash", ["-c", `source "$1"; ${command}`, "test", policy], {
    cwd: repoRoot,
    encoding: "utf8",
  }).trim();

describe("analyzer harness Playwright project policy", () => {
  test("uses foundational analyzer flows for non-video parity", () => {
    expect(runPolicy('resolve_harness_playwright_project "parity" ""')).toBe(
      "harness-foundational",
    );
  });

  test("reserves video mode for the final demo flow", () => {
    expect(runPolicy('resolve_harness_playwright_project "video" ""')).toBe(
      "harness-demo-video",
    );
  });

  test("accepts the explicit foundational project", () => {
    expect(
      runPolicy('validate_harness_playwright_project "harness-foundational"'),
    ).toBe("harness-foundational");
  });

  test("foundational parity has analyzer scenarios", () => {
    const result = spawnSync(
      "bash",
      [
        "-c",
        'source "$1"; assert_harness_project_has_specs "$2" "harness-foundational"',
        "test",
        policy,
        repoRoot,
      ],
      { cwd: repoRoot, encoding: "utf8" },
    );

    expect(result.status).toBe(0);
  });

  test("video mode fails closed until a demo scenario exists", () => {
    const result = spawnSync(
      "bash",
      [
        "-c",
        'source "$1"; assert_harness_project_has_specs "$2" "harness-demo-video"',
        "test",
        policy,
        repoRoot,
      ],
      { cwd: repoRoot, encoding: "utf8" },
    );

    expect(result.status).not.toBe(0);
    expect(result.stderr).toContain("no analyzer demo specs");
  });
});
