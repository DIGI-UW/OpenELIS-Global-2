const { defineConfig } = require("cypress");
const fs = require("fs");
const path = require("path");

// Get project root - cypress.config.js is in frontend/, so go up one level
const PROJECT_ROOT = path.resolve(__dirname, "..");

module.exports = defineConfig({
  defaultCommandTimeout: 30000, // Increased timeout for slow operations
  viewportWidth: 1200,
  viewportHeight: 700,
  video: true, // Enable video recording for debugging
  watchForFileChanges: false,
  screenshotOnRunFailure: true, // Take screenshots on failure
  env: {
    // Control whether test fixtures are cleaned up after tests
    // Set CYPRESS_CLEANUP_FIXTURES=false to keep fixtures for manual testing/debugging
    // Default: true (cleanup enabled)
    CLEANUP_FIXTURES: process.env.CYPRESS_CLEANUP_FIXTURES !== "false",
  },
  e2e: {
    setupNodeEvents(on, config) {
      // Task to load storage test fixtures
      on("task", {
        loadStorageTestData() {
          const { execSync } = require("child_process");
          // Use PROJECT_ROOT constant defined at module level
          const sqlFile = path.join(
            PROJECT_ROOT,
            "src/test/resources/storage-test-data.sql",
          );
          // Verify file exists
          if (!fs.existsSync(sqlFile)) {
            throw new Error(`SQL file not found: ${sqlFile} (PROJECT_ROOT: ${PROJECT_ROOT})`);
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
            console.error("Error loading storage test data:", error);
            console.error("SQL file path:", sqlFile);
            console.error("Project root:", PROJECT_ROOT);
            return null;
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
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
