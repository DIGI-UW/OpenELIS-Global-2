import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, test } from "vitest";

const frontendDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(frontendDir, "../..");
const seedScript = readFileSync(
  path.join(repoRoot, "projects/analyzer-harness/seed-analyzers.sh"),
  "utf8",
);
const resetScript = readFileSync(
  path.join(repoRoot, "src/test/resources/reset-test-database.sh"),
  "utf8",
);

describe("analyzer harness fixture policy", () => {
  test("reconciles named analyzer fixtures without a product hard-delete route", () => {
    expect(seedScript).not.toContain("/delete");
    expect(seedScript).toContain('method="PUT"');
    expect(seedScript).toContain('url="$ANALYZER_API/$analyzer_id"');
  });

  test("retries catalog reads after fixture reload transients", () => {
    const fetchJson = seedScript.match(/fetch_json\(\) \{([\s\S]*?)\n\}/)?.[1];

    expect(fetchJson).toContain("for attempt in 1 2 3 4 5");
  });

  test("database reset fails on obsolete schema references", () => {
    expect(resetScript.match(/ON_ERROR_STOP=1/g)).toHaveLength(2);
    expect(resetScript).not.toContain("short_code LIKE 'TEST-%'");
  });

  test("database reset is atomic and follows E2E patient sample links", () => {
    expect(resetScript).toContain("BEGIN;");
    expect(resetScript).toContain("CREATE TEMP TABLE tmp_e2e_sample_ids");
    expect(resetScript).toContain("COMMIT;");
  });
});
