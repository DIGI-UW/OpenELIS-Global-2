import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, test } from "vitest";
import { findProfileCompatibilityViolations } from "./analyzer-profile-compatibility.mjs";

const frontendDir = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const repoRoot = path.resolve(frontendDir, "..");

const fixturePaths = [
  "projects/analyzer-profiles/astm/genexpert-astm.json",
  "projects/analyzer-profiles/file/fluorocycler-xt.json",
];

const fixtures = fixturePaths.map((relativePath) => ({
  relativePath,
  profile: JSON.parse(
    fs.readFileSync(path.join(repoRoot, relativePath), "utf8"),
  ),
}));

describe.each(fixtures)(
  "$relativePath compatibility fixture",
  ({ profile }) => {
    test("proves communication and new-connection default responsibilities", () => {
      expect(findProfileCompatibilityViolations(profile)).toEqual([]);
    });

    test("rejects a metadata-only projection of the same profile", () => {
      const thinProjection = {
        profileMeta: profile.profileMeta,
        category: profile.category,
        protocol: profile.protocol?.name,
        default_test_mappings: profile.default_test_mappings,
      };

      expect(findProfileCompatibilityViolations(thinProjection)).toEqual(
        expect.arrayContaining([
          "protocol must remain a typed object",
          "configDefaults must contain profile-owned new-connection defaults",
        ]),
      );
    });
  },
);
