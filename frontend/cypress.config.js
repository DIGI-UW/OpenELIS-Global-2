const { defineConfig } = require("cypress");
const fs = require("fs");
const path = require("path");

module.exports = defineConfig({
  defaultCommandTimeout: 8000,
  viewportWidth: 1200,
  viewportHeight: 700,
  video: false,
  watchForFileChanges: false,
  e2e: {
    setupNodeEvents(on, config) {
      const e2eFolder = path.join(__dirname, "cypress/e2e");
      const findTestFiles = (dir) => {
        let results = [];
        const files = fs.readdirSync(dir);

        for (const file of files) {
          const fullPath = path.join(dir, file);
          const stat = fs.statSync(fullPath);

          if (stat.isDirectory()) {
            results = results.concat(findTestFiles(fullPath));
          } else if (file.endsWith(".cy.js")) {
            results.push(fullPath.replace(__dirname + path.sep, ""));
          }
        }

        return results;
      };
      config.specPattern = findTestFiles(e2eFolder);
      console.log("Discovered test files:", config.specPattern);
      return config;
    },
    baseUrl: "https://localhost",
    testIsolation: false,
    env: {
      STARTUP_WAIT_MILLISECONDS: 300000,
    },
  },
});
