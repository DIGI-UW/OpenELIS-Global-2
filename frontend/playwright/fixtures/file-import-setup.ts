import { execFileSync } from "child_process";
import * as fs from "fs";
import * as path from "path";

const SQL_FILE = path.resolve(
  __dirname,
  "../../../src/test/resources/fixtures/file-import-e2e.sql",
);

const DB_CONTAINER =
  process.env.DATABASE_CONTAINER ||
  process.env.FILE_IMPORT_DB_CONTAINER ||
  "openelisglobal-database";

function assertValidContainerName(containerName: string): void {
  if (!/^[a-zA-Z0-9_.-]+$/.test(containerName)) {
    throw new Error(`Invalid container name: ${containerName}`);
  }
}

/**
 * Legacy helper for direct SQL fixture loading (pre-REST-seeding model).
 *
 * Active harness CI uses:
 *  1) analyzer-harness-e2e.sql + file-import-e2e.sql for cleanup/type baseline
 *  2) projects/analyzer-harness/seed-analyzers.sh for analyzer creation
 *
 * Keep this helper only for historical/local experiments.
 * Container: DATABASE_CONTAINER or FILE_IMPORT_DB_CONTAINER env, else openelisglobal-database.
 */
export function loadFileImportFixtures(): void {
  assertValidContainerName(DB_CONTAINER);
  if (!fs.existsSync(SQL_FILE)) {
    throw new Error(`File import SQL fixture not found: ${SQL_FILE}`);
  }
  const sql = fs.readFileSync(SQL_FILE);
  execFileSync(
    "docker",
    ["exec", "-i", DB_CONTAINER, "psql", "-U", "clinlims", "-d", "clinlims"],
    { stdio: ["pipe", "inherit", "inherit"], input: sql },
  );
}

/**
 * Returns true if API-seeded FILE analyzers exist in the DB.
 */
export function checkFileImportFixturesExist(): boolean {
  assertValidContainerName(DB_CONTAINER);
  const checkSql = `SELECT COUNT(*) FROM clinlims.analyzer WHERE name IN ('QuantStudio 5','QuantStudio 7','FluoroCycler XT');`;
  try {
    const result = execFileSync(
      "docker",
      [
        "exec",
        "-i",
        DB_CONTAINER,
        "psql",
        "-U",
        "clinlims",
        "-d",
        "clinlims",
        "-t",
        "-c",
        checkSql,
      ],
      {
        encoding: "utf8",
      },
    );
    const count = parseInt(result.trim(), 10);
    return count > 0;
  } catch {
    return false;
  }
}

/**
 * Deletes legacy E2E-FILE fixture rows from the database.
 */
export function cleanFileImportTestData(): void {
  assertValidContainerName(DB_CONTAINER);
  const sql = `
    DELETE FROM clinlims.file_import_configuration WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name LIKE 'E2E-FILE-%');
    DELETE FROM clinlims.analyzer_plugin_config WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name LIKE 'E2E-FILE-%');
    DELETE FROM clinlims.analyzer WHERE name LIKE 'E2E-FILE-%';
    DELETE FROM clinlims.analyzer_type WHERE name LIKE 'E2E-FILE-%';
  `;
  execFileSync(
    "docker",
    [
      "exec",
      "-i",
      DB_CONTAINER,
      "psql",
      "-U",
      "clinlims",
      "-d",
      "clinlims",
      "-c",
      sql,
    ],
    { stdio: "inherit" },
  );
}
