import * as fs from "fs";
import * as path from "path";

const HARNESS_ROOT = path.join("projects", "analyzer-harness");
const FRONTEND_PACKAGE = path.join("frontend", "package.json");

function isWorkspaceRoot(candidate: string): boolean {
  return (
    fs.existsSync(path.join(candidate, HARNESS_ROOT)) &&
    fs.existsSync(path.join(candidate, FRONTEND_PACKAGE))
  );
}

export function resolveWorkspaceRoot(startDir: string): string {
  let current = path.resolve(startDir);

  while (true) {
    if (isWorkspaceRoot(current)) {
      return current;
    }

    const parent = path.dirname(current);
    if (parent === current) {
      throw new Error(
        `Could not resolve workspace root from ${startDir}: missing ${HARNESS_ROOT}`,
      );
    }

    current = parent;
  }
}

export function resolveHarnessImportsDir(startDir: string): string {
  return path.join(
    resolveWorkspaceRoot(startDir),
    HARNESS_ROOT,
    "volume",
    "analyzer-imports",
  );
}
