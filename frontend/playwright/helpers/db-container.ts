import { execFileSync } from "child_process";

const DEFAULT_DB_CONTAINER = "openelisglobal-database";
const DB_CONTAINER_CANDIDATES = [
  "analyzer-harness-db-1",
  "openelisglobal-database",
];

function getRunningContainers(): Set<string> {
  try {
    const output = execFileSync("docker", ["ps", "--format", "{{.Names}}"], {
      encoding: "utf8",
    });
    return new Set(
      output
        .split("\n")
        .map((name) => name.trim())
        .filter(Boolean),
    );
  } catch {
    return new Set();
  }
}

function assertValidContainerName(name: string): void {
  if (!/^[a-zA-Z0-9_.-]+$/.test(name)) {
    throw new Error(`Invalid DB container name: ${name}`);
  }
}

/**
 * Resolve the DB container for local harness and CI-style stacks.
 *
 * Priority:
 * 1) Explicit env overrides
 *    - When includeFileImportOverride=true: FILE_IMPORT_DB_CONTAINER first,
 *      then DATABASE_CONTAINER, then DB_CONTAINER
 * 2) Active known container names
 * 3) Legacy default
 */
export function resolveDbContainer(includeFileImportOverride = false): string {
  const envCandidates: string[] = [];
  if (includeFileImportOverride) {
    const fileImportContainer = process.env.FILE_IMPORT_DB_CONTAINER?.trim();
    if (fileImportContainer) {
      envCandidates.push(fileImportContainer);
    }
  }
  for (const value of [
    process.env.DATABASE_CONTAINER,
    process.env.DB_CONTAINER,
  ]) {
    const trimmed = value?.trim();
    if (trimmed) {
      envCandidates.push(trimmed);
    }
  }

  if (envCandidates.length > 0) {
    const chosen = envCandidates[0];
    assertValidContainerName(chosen);
    return chosen;
  }

  const running = getRunningContainers();
  for (const candidate of DB_CONTAINER_CANDIDATES) {
    if (running.has(candidate)) {
      return candidate;
    }
  }

  return DEFAULT_DB_CONTAINER;
}
