import { execSync } from "child_process";
import * as fs from "fs";
import * as path from "path";

const PROJECT_ROOT = path.resolve(__dirname, "../../..");
const SQL_FILE = path.join(__dirname, "file-import-setup.sql");

const DB_CONTAINER =
  process.env.DATABASE_CONTAINER ||
  process.env.FILE_IMPORT_DB_CONTAINER ||
  "openelisglobal-database";

/**
 * Loads file import E2E fixtures into the database via docker exec.
 * Idempotent: SQL deletes existing E2E-FILE-% rows before inserting.
 * Container: DATABASE_CONTAINER or FILE_IMPORT_DB_CONTAINER env, else openelisglobal-database.
 */
export function loadFileImportFixtures(): void {
  if (!fs.existsSync(SQL_FILE)) {
    throw new Error(`File import SQL fixture not found: ${SQL_FILE}`);
  }
  execSync(
    `docker exec -i ${DB_CONTAINER} psql -U clinlims -d clinlims < "${SQL_FILE}"`,
    {
      stdio: "inherit",
      cwd: PROJECT_ROOT,
      shell: "/bin/bash",
    },
  );
}

/**
 * Returns true if file import fixtures (E2E-FILE-% analyzers) exist in the DB.
 */
export function checkFileImportFixturesExist(): boolean {
  const checkSql = `SELECT COUNT(*) FROM clinlims.analyzer WHERE name LIKE 'E2E-FILE-%';`;
  try {
    const result = execSync(
      `docker exec -i ${DB_CONTAINER} psql -U clinlims -d clinlims -t -c "${checkSql}"`,
      {
        cwd: PROJECT_ROOT,
        shell: "/bin/bash",
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
 * Deletes file import E2E fixtures from the database.
 */
export function cleanFileImportTestData(): void {
  const sql = `
    DELETE FROM clinlims.file_import_configuration WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name LIKE 'E2E-FILE-%');
    DELETE FROM clinlims.analyzer_plugin_config WHERE analyzer_id IN (SELECT id FROM clinlims.analyzer WHERE name LIKE 'E2E-FILE-%');
    DELETE FROM clinlims.analyzer WHERE name LIKE 'E2E-FILE-%';
    DELETE FROM clinlims.analyzer_type WHERE name LIKE 'E2E-FILE-%';
  `;
  execSync(
    `docker exec -i ${DB_CONTAINER} psql -U clinlims -d clinlims -c "${sql}"`,
    {
      stdio: "inherit",
      cwd: PROJECT_ROOT,
      shell: "/bin/bash",
    },
  );
}
