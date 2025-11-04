const { defineConfig } = require("cypress");
const fs = require("fs");
const path = require("path");

module.exports = defineConfig({
  defaultCommandTimeout: 30000, // Increased timeout for slow operations
  viewportWidth: 1200,
  viewportHeight: 700,
  video: true, // Enable video recording for debugging
  watchForFileChanges: false,
  screenshotOnRunFailure: true, // Take screenshots on failure
  e2e: {
    setupNodeEvents(on, config) {
      // Task to load storage test fixtures
      on("task", {
        loadStorageTestData() {
          const { execSync } = require("child_process");
          const sqlFile = path.join(
            __dirname,
            "../../src/test/resources/storage-test-data.sql",
          );
          try {
            execSync(
              `docker exec -i openelisglobal-database psql -U clinlims -d clinlims < ${sqlFile}`,
              {
                stdio: "inherit",
                cwd: path.join(__dirname, "../.."),
              },
            );
            return null;
          } catch (error) {
            console.error("Error loading storage test data:", error);
            return null;
          }
        },
        cleanStorageTestData() {
          const { execSync } = require("child_process");
          const sql = `
            DELETE FROM sample_storage_movement WHERE sample_id IN (SELECT id FROM sample WHERE accession_number LIKE 'TEST-%');
            DELETE FROM sample_storage_assignment WHERE sample_id IN (SELECT id FROM sample WHERE accession_number LIKE 'TEST-%');
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
                cwd: path.join(__dirname, "../.."),
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
