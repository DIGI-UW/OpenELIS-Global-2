const { defineConfig } = require("cypress");
const fs = require("fs");
const path = require("path");

// Get project root - cypress.config.js is in frontend/, so go up one level
const PROJECT_ROOT = path.resolve(__dirname, "..");

module.exports = defineConfig({
  defaultCommandTimeout: 30000, // Increased timeout for slow operations
  viewportWidth: 1200,
  viewportHeight: 700,
  video: false, // Disabled by default per Constitution V.5 (enable only for debugging specific failures)
  watchForFileChanges: false,
  screenshotOnRunFailure: true, // Take screenshots on failure (required per Constitution V.5)
  env: {
    // Control whether test fixtures are cleaned up after tests
    // Set CYPRESS_CLEANUP_FIXTURES=false to keep fixtures for manual testing/debugging
    // Default: false (cleanup disabled for faster iteration)
    CLEANUP_FIXTURES: process.env.CYPRESS_CLEANUP_FIXTURES === "true",

    // Skip fixture loading entirely (assumes fixtures already exist)
    // Set CYPRESS_SKIP_FIXTURES=true to skip loading (fastest iteration)
    // Default: false (check and load if needed)
    SKIP_FIXTURES: process.env.CYPRESS_SKIP_FIXTURES === "true",

    // Force reload fixtures even if they already exist
    // Set CYPRESS_FORCE_FIXTURES=true to always reload
    // Default: false (check existence first)
    FORCE_FIXTURES: process.env.CYPRESS_FORCE_FIXTURES === "true",
  },
  e2e: {
    setupNodeEvents(on, config) {
      // NOTE: Storage E2E tests (001-sample-storage) are currently disabled
      // Storage tests excluded via excludeSpecPattern in e2e config
      // Storage support imports commented out in e2e.js
      // Storage tasks below remain registered but won't be called (harmless)
      // To re-enable: Uncomment imports in e2e.js and remove excludeSpecPattern

      // Task to log messages to terminal (for console.log capture)
      // This is used to forward browser console logs to terminal
      on("task", {
        log(message, options = {}) {
          // Only log if not explicitly disabled
          if (options.log !== false) {
            console.log(message);
          }
          return null;
        },
        logObject(obj) {
          console.log(JSON.stringify(obj, null, 2));
          return null;
        },
      });

      // Task to load storage test fixtures
      on("task", {
        loadStorageTestData() {
          const { execSync } = require("child_process");
          // Use unified fixture loader script
          const loaderScript = path.join(
            PROJECT_ROOT,
            "src/test/resources/load-test-fixtures.sh",
          );
          // Verify script exists
          if (!fs.existsSync(loaderScript)) {
            throw new Error(
              `Fixture loader script not found: ${loaderScript} (PROJECT_ROOT: ${PROJECT_ROOT})`,
            );
          }
          try {
            execSync(`bash "${loaderScript}"`, {
              stdio: "inherit",
              cwd: PROJECT_ROOT,
              shell: "/bin/bash",
            });
            return null;
          } catch (error) {
            console.error("Error loading test fixtures:", error);
            console.error("Loader script path:", loaderScript);
            console.error("Project root:", PROJECT_ROOT);
            return null;
          }
        },
        checkStorageFixturesExist() {
          const { execSync } = require("child_process");
          // Check if E2E test data exists (quick check for a known test room)
          const checkSql = `
            SELECT COUNT(*) as count FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE');
          `;
          try {
            const result = execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -t -c "${checkSql}"`,
              {
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
              },
            );
            const count = parseInt(result.trim(), 10);
            return count >= 2; // At least 2 test rooms exist
          } catch (error) {
            console.error("Error checking storage fixtures:", error);
            return false;
          }
        },
        cleanStorageTestData() {
          const { execSync } = require("child_process");
          const sql = `
            DELETE FROM sample_storage_movement WHERE sample_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%');
            DELETE FROM sample_storage_assignment WHERE sample_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%');
            DELETE FROM sample_human WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%');
            DELETE FROM sample_item WHERE samp_id IN (SELECT id FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%');
            DELETE FROM sample WHERE accession_number LIKE 'E2E-%' OR accession_number LIKE 'TEST-%';
            DELETE FROM patient_identity WHERE patient_id IN (SELECT id FROM patient WHERE external_id LIKE 'E2E-%');
            DELETE FROM patient WHERE external_id LIKE 'E2E-%';
            DELETE FROM person WHERE id IN (SELECT person_id FROM patient WHERE external_id LIKE 'E2E-%' UNION SELECT id FROM person WHERE last_name LIKE 'E2E-%');
            DELETE FROM storage_position WHERE id BETWEEN 100 AND 10000;
            DELETE FROM storage_rack WHERE id BETWEEN 30 AND 100;
            DELETE FROM storage_shelf WHERE id BETWEEN 20 AND 100;
            DELETE FROM storage_device WHERE id BETWEEN 10 AND 100;
            DELETE FROM storage_room WHERE id BETWEEN 1 AND 100;
          `;
          try {
            execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -c "${sql}"`,
              {
                stdio: "inherit",
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
              },
            );
            return null;
          } catch (error) {
            console.error("Error cleaning storage test data:", error);
            return null;
          }
        },
      });

      try {
        const e2eFolder = path.join(__dirname, "cypress/e2e");

        // Define the first four prioritized tests
        const prioritizedTests = [
          "cypress/e2e/login.cy.js",
          "cypress/e2e/home.cy.js",
          "cypress/e2e/AdminE2E/organizationManagement.cy.js",
          "cypress/e2e/AdminE2E/providerManagement.cy.js",
          "cypress/e2e/patientEntry.cy.js",
          "cypress/e2e/orderEntity.cy.js",
        ];

        const findTestFiles = (dir) => {
          let results = [];
          const files = fs.readdirSync(dir);

          for (const file of files) {
            const fullPath = path.join(dir, file);
            const stat = fs.statSync(fullPath);

            if (stat.isDirectory()) {
              results = results.concat(findTestFiles(fullPath));
            } else if (file.endsWith(".cy.js")) {
              const relativePath = fullPath.replace(__dirname + path.sep, "");
              if (!prioritizedTests.includes(relativePath)) {
                results.push(relativePath);
              }
            }
          }

          return results;
        };

        let remainingTests = findTestFiles(e2eFolder);
        remainingTests.sort((a, b) => a.localeCompare(b));

        // Combine the prioritized tests and dynamically detected tests
        config.specPattern = [...prioritizedTests, ...remainingTests];

        console.log("Running tests in custom order:", config.specPattern);

        return config;
      } catch (error) {
        console.error("Error in setupNodeEvents:", error);
        return config;
      }
    },
    baseUrl: "https://localhost",
    testIsolation: false,
    // DISABLED: Exclude storage tests (001-sample-storage feature)
    // Remove "**/storage*.cy.js" from this array to re-enable storage tests
    excludeSpecPattern: ["**/storage*.cy.js"],
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
