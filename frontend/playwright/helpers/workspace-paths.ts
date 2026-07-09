import * as fs from "fs";
import * as path from "path";

const HARNESS_ROOT = path.join("projects", "analyzer-harness");
const FRONTEND_PACKAGE = path.join("frontend", "package.json");

/** Override host path to `projects/analyzer-harness/volume/analyzer-imports` for non-repo layouts. */
const ENV_HARNESS_IMPORTS = "HARNESS_ANALYZER_IMPORTS_DIR";

function isWorkspaceRoot(candidate: string): boolean {
  return (
    fs.existsSync(path.join(candidate, HARNESS_ROOT)) &&
    fs.existsSync(path.join(candidate, FRONTEND_PACKAGE))
  );
}

export function resolveWorkspaceRoot(startDir: string): string {
  let current = path.resolve(startDir);

  let parent = path.dirname(current);
  while (parent !== current) {
    if (isWorkspaceRoot(current)) {
      return current;
    }
    current = parent;
    parent = path.dirname(current);
  }

  if (isWorkspaceRoot(current)) {
    return current;
  }

  throw new Error(
    `Could not resolve workspace root from ${startDir}: missing ${HARNESS_ROOT}`,
  );
}

export function resolveHarnessImportsDir(startDir: string): string {
  const override = process.env[ENV_HARNESS_IMPORTS]?.trim();
  if (override) {
    return path.resolve(override);
  }
  return path.join(
    resolveWorkspaceRoot(startDir),
    HARNESS_ROOT,
    "volume",
    "analyzer-imports",
  );
}
