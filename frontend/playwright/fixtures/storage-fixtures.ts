import fs from "fs";
import path from "path";
import { execSync } from "child_process";

let fixturesLoaded = false;

/**
 * Playwright equivalent to Cypress storage fixture loading.
 * Opt-in with PW_LOAD_STORAGE_FIXTURES=true to avoid unexpected local side effects.
 */
export function ensureStorageFixturesLoaded(): void {
  if (fixturesLoaded) {
    return;
  }

  if (process.env.PW_LOAD_STORAGE_FIXTURES !== "true") {
    return;
  }

  const projectRoot = path.resolve(__dirname, "../../../");
  const loaderScript = path.resolve(
    projectRoot,
    "src/test/resources/load-test-fixtures.sh",
  );

  if (!fs.existsSync(loaderScript)) {
    throw new Error(`Storage fixture loader script not found: ${loaderScript}`);
  }

  execSync(`bash "${loaderScript}"`, {
    stdio: "inherit",
    cwd: projectRoot,
    shell: "/bin/bash",
  });

  fixturesLoaded = true;
}
