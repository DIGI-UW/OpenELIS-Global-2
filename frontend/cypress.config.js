const { defineConfig } = require("cypress");
const fs = require("fs");
const path = require("path");

// Get project root - cypress.config.js is in frontend/, so go up one level
const PROJECT_ROOT = path.resolve(__dirname, "..");

module.exports = defineConfig({
  defaultCommandTimeout: 4000, // Standard timeout (4s) - Cypress retry-ability handles most cases. Only backend API calls should take longer.
  viewportWidth: 1920,
  viewportHeight: 1080,
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

    // Reset database before loading fixtures
    // Set CYPRESS_RESET_DATABASE=true to reset before loading
    // Default: false (preserves existing data)
    RESET_DATABASE: process.env.CYPRESS_RESET_DATABASE === "true",

    // Verify fixtures even when skipping load
    // Set CYPRESS_VERIFY_FIXTURES=true to verify fixtures
    // Default: false (skip verification when skipping load)
    VERIFY_FIXTURES: process.env.CYPRESS_VERIFY_FIXTURES === "true",
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

      // Task to reset test database
      on("task", {
        resetDatabase() {
          const { execSync } = require("child_process");
          const resetScript = path.join(
            PROJECT_ROOT,
            "src/test/resources/reset-test-database.sh",
          );
          if (!fs.existsSync(resetScript)) {
            throw new Error(
              `Reset script not found: ${resetScript} (PROJECT_ROOT: ${PROJECT_ROOT})`,
            );
          }
          try {
            execSync(`bash "${resetScript}" --force`, {
              stdio: "inherit",
              cwd: PROJECT_ROOT,
              shell: "/bin/bash",
            });
            return null;
          } catch (error) {
            console.error("Error resetting database:", error);
            throw error;
          }
        },
        loadStorageTestData(options = {}) {
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
            // Build command with optional --reset flag
            let command = `bash "${loaderScript}"`;
            if (options.reset) {
              command += " --reset";
            }
            if (options.noVerify) {
              command += " --no-verify";
            }
            execSync(command, {
              stdio: "inherit",
              cwd: PROJECT_ROOT,
              shell: "/bin/bash",
            });
            return null;
          } catch (error) {
            console.error("Error loading test fixtures:", error);
            console.error("Loader script path:", loaderScript);
            console.error("Project root:", PROJECT_ROOT);
            throw error;
          }
        },
        checkStorageFixturesExist() {
          const { execSync } = require("child_process");
          // Comprehensive check: rooms, samples, and patients
          const checkSql = `
            SELECT 
              (SELECT COUNT(*) FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')) as rooms,
              (SELECT COUNT(*) FROM sample WHERE accession_number LIKE 'E2E-%') as samples,
              (SELECT COUNT(*) FROM patient WHERE external_id LIKE 'E2E-%') as patients;
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
            // Parse result: "3 | 10 | 3"
            const parts = result
              .trim()
              .split("|")
              .map((s) => parseInt(s.trim(), 10));
            const rooms = parts[0] || 0;
            const samples = parts[1] || 0;
            const patients = parts[2] || 0;

            // Fixtures exist if we have at least 3 rooms, 10 samples, and 3 patients
            return rooms >= 3 && samples >= 10 && patients >= 3;
          } catch (error) {
            console.error("Error checking storage fixtures:", error);
            return false;
          }
        },
        verifyFixtures() {
          const { execSync } = require("child_process");
          // Comprehensive verification query
          const verifySql = `
            SELECT 
              'Storage Hierarchy' AS category,
              'Rooms' AS type, 
              COUNT(*) AS count,
              CASE WHEN COUNT(*) >= 3 THEN 'OK' ELSE 'MISSING' END AS status
            FROM storage_room WHERE code IN ('MAIN', 'SEC', 'INACTIVE')
            UNION ALL
            SELECT '', 'Devices', COUNT(*), CASE WHEN COUNT(*) >= 5 THEN 'OK' ELSE 'MISSING' END
            FROM storage_device WHERE id BETWEEN 10 AND 20
            UNION ALL
            SELECT '', 'Shelves', COUNT(*), CASE WHEN COUNT(*) >= 6 THEN 'OK' ELSE 'MISSING' END
            FROM storage_shelf WHERE id BETWEEN 20 AND 30
            UNION ALL
            SELECT '', 'Racks', COUNT(*), CASE WHEN COUNT(*) >= 6 THEN 'OK' ELSE 'MISSING' END
            FROM storage_rack WHERE id BETWEEN 30 AND 40
            UNION ALL
            SELECT '', 'Positions', COUNT(*), CASE WHEN COUNT(*) >= 99 THEN 'OK' ELSE 'MISSING' END
            FROM storage_position WHERE id BETWEEN 100 AND 10000
            UNION ALL
            SELECT 
              'E2E Test Data' AS category,
              'Patients' AS type,
              COUNT(*),
              CASE WHEN COUNT(*) >= 3 THEN 'OK' ELSE 'MISSING' END
            FROM patient WHERE external_id LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Samples', COUNT(*), CASE WHEN COUNT(*) >= 10 THEN 'OK' ELSE 'MISSING' END
            FROM sample WHERE accession_number LIKE 'E2E-%'
            UNION ALL
            SELECT '', 'Sample Items', COUNT(*), CASE WHEN COUNT(*) >= 20 THEN 'OK' ELSE 'MISSING' END
            FROM sample_item WHERE id BETWEEN 10000 AND 20000
            UNION ALL
            SELECT '', 'Storage Assignments', COUNT(*), CASE WHEN COUNT(*) >= 15 THEN 'OK' ELSE 'MISSING' END
            FROM sample_storage_assignment WHERE id >= 1000
            UNION ALL
            SELECT '', 'Analyses', COUNT(*), CASE WHEN COUNT(*) >= 5 THEN 'OK' ELSE 'MISSING' END
            FROM analysis WHERE id BETWEEN 20000 AND 30000
            UNION ALL
            SELECT '', 'Results', COUNT(*), CASE WHEN COUNT(*) >= 2 THEN 'OK' ELSE 'MISSING' END
            FROM result WHERE id BETWEEN 30000 AND 40000;
          `;
          try {
            const result = execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -t -c "${verifySql}"`,
              {
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
              },
            );
            console.log("Fixture Verification Results:");
            console.log(result);

            // Check if any status is MISSING
            const hasMissing = result.includes("MISSING");
            return !hasMissing;
          } catch (error) {
            console.error("Error verifying fixtures:", error);
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
        execSql(options) {
          const { execSync } = require("child_process");
          const { query } = options;
          try {
            const result = execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -c "${query}"`,
              {
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
              },
            );
            return { success: true, output: result };
          } catch (error) {
            console.error("Error executing SQL:", error);
            return { success: false, error: error.message };
          }
        },
        deleteTestPatient(options) {
          const { execSync } = require("child_process");
          const { subjectNumber, nationalId } = options;
          // Delete patient by subjectNumber or nationalId (cascades to person, patient_identity, etc.)
          const sql = `
            DELETE FROM patient_identity WHERE patient_id IN (
              SELECT patient_id FROM patient_identity 
              WHERE identity_type_id IN (
                SELECT id FROM patient_identity_type WHERE identity_type = 'SUBJECT'
              ) AND identity_data = '${subjectNumber}'
              UNION
              SELECT patient_id FROM patient_identity 
              WHERE identity_type_id IN (
                SELECT id FROM patient_identity_type WHERE identity_type = 'NATIONAL'
              ) AND identity_data = '${nationalId}'
            );
            DELETE FROM patient WHERE id IN (
              SELECT patient_id FROM patient_identity 
              WHERE identity_type_id IN (
                SELECT id FROM patient_identity_type WHERE identity_type = 'SUBJECT'
              ) AND identity_data = '${subjectNumber}'
              UNION
              SELECT patient_id FROM patient_identity 
              WHERE identity_type_id IN (
                SELECT id FROM patient_identity_type WHERE identity_type = 'NATIONAL'
              ) AND identity_data = '${nationalId}'
            );
            DELETE FROM person WHERE id IN (
              SELECT person_id FROM patient WHERE id IN (
                SELECT patient_id FROM patient_identity 
                WHERE identity_type_id IN (
                  SELECT id FROM patient_identity_type WHERE identity_type = 'SUBJECT'
                ) AND identity_data = '${subjectNumber}'
                UNION
                SELECT patient_id FROM patient_identity 
                WHERE identity_type_id IN (
                  SELECT id FROM patient_identity_type WHERE identity_type = 'NATIONAL'
                ) AND identity_data = '${nationalId}'
              )
            );
          `;
          try {
            execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -c "${sql}"`,
              {
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
                stdio: "pipe",
              },
            );
            return { success: true };
          } catch (error) {
            console.error("Error deleting test patient:", error);
            return { success: false, error: error.message };
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
    // Storage tests re-enabled for debugging and validation
    // excludeSpecPattern: ["**/storage*.cy.js"],
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
