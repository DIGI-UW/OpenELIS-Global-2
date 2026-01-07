const { defineConfig } = require("cypress");
const fs = require("fs");
const path = require("path");

// Get project root - cypress.config.js is in frontend/, so go up one level
const PROJECT_ROOT = path.resolve(__dirname, "..");

module.exports = defineConfig({
  defaultCommandTimeout: 3000, // 3 seconds - use Cypress retry-ability instead of long timeouts
  viewportWidth: 1920, // Large desktop for full modal visibility (including warnings/checkboxes)
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
  },
  e2e: {
    setupNodeEvents(on, config) {
      // NOTE: Storage E2E tests (001-sample-storage) are currently disabled
      // Storage tests excluded via excludeSpecPattern in e2e config
      // Storage support imports commented out in e2e.js
      // Storage tasks below remain registered but won't be called (harmless)
      // To re-enable: Uncomment imports in e2e.js and remove excludeSpecPattern

      // Register all Cypress tasks in ONE handler (Cypress does not merge task handlers).
      // This keeps logging/diagnostics and fixture utilities available across specs.
      on("task", {
        // Log messages to the Node process stdout (captured by CI/tee logs)
        log(message, options = {}) {
          if (options.log !== false) {
            console.log(message);
          }
          return null;
        },
        logObject(obj) {
          console.log(JSON.stringify(obj, null, 2));
          return null;
        },

        // Storage test fixture helpers
        loadStorageTestData() {
          const { execSync } = require("child_process");
          const loaderScript = path.join(
            PROJECT_ROOT,
            "src/test/resources/load-test-fixtures.sh",
          );
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
            // Throw error to fail the test immediately with a clear message
            // This prevents tests from running with missing fixtures
            throw new Error(
              `Failed to load test fixtures: ${error.message || error}. Check logs above for details.`,
            );
          }
        },
        checkStorageFixturesExist() {
          const { execSync } = require("child_process");
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
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
              },
            );
            const raw = (result || "").trim();
            const [rooms, devices, shelves, racks, boxes] = raw
              .split(",")
              .map((v) => parseInt((v || "").trim(), 10));

            // Fixtures are only considered present if the FULL hierarchy exists.
            // (Rooms alone are not sufficient; shelves/racks/boxes are critical for location CRUD + box grid tests.)
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

      // Patient Merge tasks
      on("task", {
        loadPatientMergeTestData() {
          const { execSync } = require("child_process");
          const sqlFile = path.join(
            __dirname,
            "cypress/support/patient-merge-setup.sql",
          );
          if (!fs.existsSync(sqlFile)) {
            throw new Error(`Patient merge SQL fixture not found: ${sqlFile}`);
          }
          try {
            execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims < "${sqlFile}"`,
              {
                stdio: "inherit",
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
              },
            );
            return null;
          } catch (error) {
            console.error("Error loading patient merge test data:", error);
            return null;
          }
        },
        checkPatientMergeFixturesExist() {
          const { execSync } = require("child_process");
          const checkSql = `SELECT COUNT(*) as count FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%';`;
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
            return count >= 2; // Both Alice and Bob exist
          } catch (error) {
            console.error("Error checking patient merge fixtures:", error);
            return false;
          }
        },
        cleanPatientMergeTestData() {
          const { execSync } = require("child_process");
          const sql = `
            DELETE FROM clinlims.sample_human WHERE patient_id IN (SELECT id FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%');
            DELETE FROM clinlims.patient_identity WHERE patient_id IN (SELECT id FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%');
            DELETE FROM clinlims.patient WHERE national_id LIKE 'UG-MERGE-%';
            DELETE FROM clinlims.person WHERE email LIKE '%@testmerge.com';
            DELETE FROM clinlims.sample WHERE accession_number LIKE 'MERGE-%';
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
            console.error("Error cleaning patient merge test data:", error);
            return null;
          }
        },
        // Verification task: Get sample count for a patient by national ID
        getPatientSampleCount(nationalId) {
          const { execSync } = require("child_process");
          const sql = `
            SELECT COUNT(*) as sample_count
            FROM clinlims.sample_human sh
            JOIN clinlims.patient p ON sh.patient_id = p.id
            WHERE p.national_id = '${nationalId}';
          `;
          try {
            const result = execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -t -c "${sql}"`,
              {
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
              },
            );
            return parseInt(result.trim(), 10);
          } catch (error) {
            console.error("Error getting patient sample count:", error);
            return -1;
          }
        },
        // Verification task: Get patient demographics by national ID
        getPatientDemographics(nationalId) {
          const { execSync } = require("child_process");
          const sql = `
            SELECT
              per.first_name,
              per.last_name,
              per.primary_phone,
              per.email,
              per.street_address,
              per.city,
              p.national_id,
              p.is_merged,
              per.work_phone,
              per.fax
            FROM clinlims.patient p
            JOIN clinlims.person per ON p.person_id = per.id
            WHERE p.national_id = '${nationalId}';
          `;
          try {
            const result = execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -t -A -F '|' -c "${sql}"`,
              {
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
              },
            );
            const parts = result.trim().split("|");
            if (parts.length >= 7) {
              return {
                firstName: parts[0],
                lastName: parts[1],
                phone: parts[2],
                email: parts[3],
                address: parts[4],
                city: parts[5],
                nationalId: parts[6],
                isMerged: parts[7] === "t" || parts[7] === "true",
                workPhone: parts[8] || null,
                fax: parts[9] || null,
              };
            }
            return null;
          } catch (error) {
            console.error("Error getting patient demographics:", error);
            return null;
          }
        },
        // Verification task: Check if merge audit record exists
        getMergeAuditRecord(mergedPatientNationalId) {
          const { execSync } = require("child_process");
          // Column names per Liquibase schema (016-patient-merge-create-audit-table.xml):
          // - reason (not merge_reason)
          // - merge_date (not merged_at)
          const sql = `
            SELECT
              pma.id,
              pma.primary_patient_id,
              pma.merged_patient_id,
              pma.reason,
              pma.merge_date
            FROM clinlims.patient_merge_audit pma
            JOIN clinlims.patient p ON pma.merged_patient_id = p.id
            WHERE p.national_id = '${mergedPatientNationalId}'
            ORDER BY pma.merge_date DESC
            LIMIT 1;
          `;
          try {
            const result = execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims -t -A -F '|' -c "${sql}"`,
              {
                cwd: PROJECT_ROOT,
                shell: "/bin/bash",
                encoding: "utf8",
              },
            );
            const parts = result.trim().split("|");
            if (parts.length >= 4) {
              return {
                auditId: parts[0],
                primaryPatientId: parts[1],
                mergedPatientId: parts[2],
                mergeReason: parts[3],
                mergedAt: parts[4],
              };
            }
            return null;
          } catch (error) {
            console.error("Error getting merge audit record:", error);
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
    // Storage tests are now enabled for M2 frontend verification
    // No excludeSpecPattern - all storage tests should run
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
