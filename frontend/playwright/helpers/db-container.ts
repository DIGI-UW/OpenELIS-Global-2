/**
 * Canonical Postgres container name for optional Playwright `docker exec` helpers.
 *
 * Compose contract: `container_name: openelisglobal-database` on `db.openelis.org`
 * (`build.docker-compose.yml`, `projects/analyzer-harness/docker-compose.base.yml`).
 *
 * Override when targeting a non-standard stack (first match wins):
 * `HARNESS_DB_CONTAINER`, `DATABASE_CONTAINER`, `DB_CONTAINER`.
 * With `includeFileImportOverride`, `FILE_IMPORT_DB_CONTAINER` is checked first.
 */

import { DEFAULT_HARNESS_DB_CONTAINER } from "./harness-contract";

function assertValidContainerName(name: string): void {
  if (!/^[a-zA-Z0-9_.-]+$/.test(name)) {
    throw new Error(`Invalid DB container name: ${name}`);
  }
}

function firstNonEmptyEnv(keys: string[]): string | undefined {
  for (const key of keys) {
    const value = process.env[key]?.trim();
    if (value) {
      return value;
    }
  }
  return undefined;
}

export function resolveDbContainer(includeFileImportOverride = false): string {
  const keys: string[] = [];
  if (includeFileImportOverride) {
    keys.push("FILE_IMPORT_DB_CONTAINER");
  }
  keys.push("HARNESS_DB_CONTAINER", "DATABASE_CONTAINER", "DB_CONTAINER");

  const chosen = firstNonEmptyEnv(keys);
  if (chosen) {
    assertValidContainerName(chosen);
    return chosen;
  }

  return DEFAULT_HARNESS_DB_CONTAINER;
}
