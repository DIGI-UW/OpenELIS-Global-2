import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, test } from "vitest";
import {
  findAddedProfileBoundaryViolations,
  findHardcodedProfileSpecialCases,
  findProfileDefaultAuthorityViolations,
  findProfileCompatibilityViolations,
} from "./analyzer-profile-compatibility.mjs";

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
    test("proves communication and instance-default responsibilities", () => {
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
          "configDefaults must contain profile-owned instance defaults",
        ]),
      );
    });
  },
);

describe("profile implementation policy", () => {
  test("rejects profile-specific production branches using fixture-derived values", () => {
    const profile = fixtures[0].profile;
    const source = [
      `if (profileId === "${profile.profileMeta.id}") return specialCase;`,
      `if (manufacturer.equals("${profile.manufacturer}")) return fallback;`,
      `switch (testCode) { case "${profile.default_test_mappings[0].test_code}": return override; }`,
    ].join("\n");

    expect(findHardcodedProfileSpecialCases(source, fixtures)).toHaveLength(3);
  });

  test("allows generic profile and pin lookup", () => {
    const source = [
      "const profile = catalog.get(profilePin.id, profilePin.revision);",
      "const mapping = profile.testMappings.get(rawAnalyzerCode);",
      "return runtime.configure(profile.protocol, profile.configDefaults);",
    ].join("\n");

    expect(findHardcodedProfileSpecialCases(source, fixtures)).toEqual([]);
  });

  test("rejects source constants as selected-profile defaults", () => {
    const source = [
      "const protocolVersion = PLUGIN_PROTOCOL_DEFAULTS[selectedProfile.protocol];",
      "const communicationMode = DEFAULT_COMMUNICATION_MODE;",
      "const filePattern = FILE_FORMAT_PATTERNS[selectedProfile.protocol];",
    ].join("\n");

    expect(findProfileDefaultAuthorityViolations(source)).toEqual([
      "DEFAULT_COMMUNICATION_MODE",
      "FILE_FORMAT_PATTERNS",
      "PLUGIN_PROTOCOL_DEFAULTS",
    ]);
  });

  test("allows defaults read from the selected profile", () => {
    const source = [
      "const defaults = selectedProfile.configDefaults;",
      "const protocolVersion = selectedProfile.protocol.version;",
      "const communicationMode = defaults.communicationMode;",
    ].join("\n");

    expect(findProfileDefaultAuthorityViolations(source)).toEqual([]);
  });

  test("rejects new profile-boundary violations in production diffs", () => {
    const profile = fixtures[0].profile;
    const diff = [
      "diff --git a/src/main/java/org/openelisglobal/analyzer/Bad.java b/src/main/java/org/openelisglobal/analyzer/Bad.java",
      "+++ b/src/main/java/org/openelisglobal/analyzer/Bad.java",
      "+AnalyzerQcRule rule;",
      "+String defaultConfigId;",
      "+WatchService watcher;",
      `+if (profileId.equals("${profile.profileMeta.id}")) return specialCase;`,
      "diff --git a/src/main/java/org/itech/ahb/Bad.java b/src/main/java/org/itech/ahb/Bad.java",
      "+++ b/src/main/java/org/itech/ahb/Bad.java",
      "+return fallbackControlClassification(message);",
    ].join("\n");

    expect(
      findAddedProfileBoundaryViolations(diff, fixtures).map(
        (violation) => violation.rule,
      ),
    ).toEqual([
      "analyzer-qc-rule",
      "copied-profile-authority",
      "oe-file-watcher",
      "hardcoded-profile-special-case",
      "hidden-control-classifier-fallback",
    ]);
  });

  test("allows deletions, tests, profile data, and generic production lookup", () => {
    const profile = fixtures[0].profile;
    const diff = [
      "diff --git a/src/main/java/org/openelisglobal/analyzer/Good.java b/src/main/java/org/openelisglobal/analyzer/Good.java",
      "+++ b/src/main/java/org/openelisglobal/analyzer/Good.java",
      "+return catalog.get(profilePin.id(), profilePin.revision());",
      "-AnalyzerQcRule removed;",
      "diff --git a/src/test/java/ProfileTest.java b/src/test/java/ProfileTest.java",
      "+++ b/src/test/java/ProfileTest.java",
      `+String fixtureId = "${profile.profileMeta.id}";`,
      "diff --git a/projects/analyzer-profiles/astm/example.json b/projects/analyzer-profiles/astm/example.json",
      "+++ b/projects/analyzer-profiles/astm/example.json",
      `+{"id":"${profile.profileMeta.id}"}`,
    ].join("\n");

    expect(findAddedProfileBoundaryViolations(diff, fixtures)).toEqual([]);
  });
});
