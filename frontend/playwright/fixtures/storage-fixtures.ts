import fs from "fs";
import path from "path";
import { execSync } from "child_process";

type FlowType = "read" | "mutating";
type FixtureMode = "verify-reuse" | "reset-load-verify" | "skip";

type EnsureStorageFixtureOptions = {
  flowType?: FlowType;
  forceReload?: boolean;
};

const loadedModes = new Set<string>();

function normalizeFixtureMode(value: string): FixtureMode {
  if (value === "verify-reuse" || value === "reset-load-verify") {
    return value;
  }
  if (value === "skip") {
    return "skip";
  }
  return "verify-reuse";
}

function resolveFixtureMode(flowType: FlowType): FixtureMode {
  const strategyMode = process.env.PW_FIXTURE_STRATEGY_MODE || "hybrid";

  if (strategyMode === "verify-reuse") {
    return "verify-reuse";
  }

  if (strategyMode === "reset-load-verify") {
    return "reset-load-verify";
  }

  if (strategyMode === "skip") {
    return "skip";
  }

  if (flowType === "mutating") {
    return normalizeFixtureMode(
      process.env.PW_FIXTURE_MUTATING_MODE || "reset-load-verify",
    );
  }

  return normalizeFixtureMode(
    process.env.PW_FIXTURE_READ_MODE || "verify-reuse",
  );
}

function checkStorageFixturesExist(projectRoot: string): boolean | null {
  const checkSql = `
    SELECT
      (SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')) AS rooms,
      (SELECT COUNT(*) FROM storage_device WHERE id BETWEEN 10 AND 20) AS devices,
      (SELECT COUNT(*) FROM storage_shelf WHERE id BETWEEN 20 AND 30) AS shelves,
      (SELECT COUNT(*) FROM storage_rack WHERE id BETWEEN 30 AND 40) AS racks,
      (SELECT COUNT(*) FROM storage_box WHERE id BETWEEN 100 AND 10000) AS boxes;
  `;

  try {
    const result = execSync(
      `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -t -A -F "," -c "${checkSql}"`,
      {
        cwd: projectRoot,
        shell: "/bin/bash",
        encoding: "utf8",
      },
    );
    const raw = (result || "").trim();
    const [rooms, devices, shelves, racks, boxes] = raw
      .split(",")
      .map((value) => parseInt((value || "").trim(), 10));

    return (
      Number.isFinite(rooms) &&
      Number.isFinite(devices) &&
      Number.isFinite(shelves) &&
      Number.isFinite(racks) &&
      Number.isFinite(boxes) &&
      rooms >= 2 &&
      devices >= 1 &&
      shelves >= 1 &&
      racks >= 1 &&
      boxes >= 1
    );
  } catch (error) {
    return null;
  }
}

function runFixtureLoader(projectRoot: string, resetFirst: boolean): void {
  const loaderScript = path.resolve(
    projectRoot,
    "src/test/resources/load-test-fixtures.sh",
  );

  if (!fs.existsSync(loaderScript)) {
    throw new Error(`Storage fixture loader script not found: ${loaderScript}`);
  }

  const resetArg = resetFirst ? " --reset" : "";
  execSync(`bash "${loaderScript}"${resetArg}`, {
    stdio: "inherit",
    cwd: projectRoot,
    shell: "/bin/bash",
  });
}

function verifyFixtureState(projectRoot: string): void {
  const verification = checkStorageFixturesExist(projectRoot);
  if (verification === true) {
    return;
  }

  if (verification === null) {
    // Non-fatal in environments where docker-backed checks are unavailable.
    return;
  }

  if (process.env.PW_FIXTURE_STRICT_VERIFY !== "false") {
    throw new Error(
      "Storage fixture verification failed after fixture load operation.",
    );
  }
}

/**
 * Playwright storage fixture management (M3 hybrid strategy).
 *
 * Opt-in with PW_LOAD_STORAGE_FIXTURES=true to avoid unexpected local side
 * effects when running unrelated specs.
 *
 * Defaults:
 * - PW_FIXTURE_STRATEGY_MODE=hybrid
 * - PW_FIXTURE_READ_MODE=verify-reuse
 * - PW_FIXTURE_MUTATING_MODE=reset-load-verify
 */
export function ensureStorageFixturesLoaded(
  options: EnsureStorageFixtureOptions = {},
): void {
  if (process.env.PW_LOAD_STORAGE_FIXTURES !== "true") {
    return;
  }

  const flowType: FlowType =
    options.flowType === "mutating" ? "mutating" : "read";
  const fixtureMode = resolveFixtureMode(flowType);
  if (fixtureMode === "skip") {
    return;
  }

  const projectRoot = path.resolve(__dirname, "../../../");
  const cacheKey = `${flowType}:${fixtureMode}`;
  if (!options.forceReload && loadedModes.has(cacheKey)) {
    return;
  }

  if (fixtureMode === "verify-reuse") {
    const fixturesExist = checkStorageFixturesExist(projectRoot);
    if (fixturesExist === true && !options.forceReload) {
      loadedModes.add(cacheKey);
      return;
    }

    runFixtureLoader(projectRoot, false);
    verifyFixtureState(projectRoot);
    loadedModes.add(cacheKey);
    return;
  }

  runFixtureLoader(projectRoot, true);
  verifyFixtureState(projectRoot);
}
