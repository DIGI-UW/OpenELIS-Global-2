// ***********************************************************
// This example support/e2e.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import "./commands";

// Import cypress-fail-fast support for fail-fast functionality
// Controlled by FAIL_FAST_ENABLED env variable (set via E2E_FAIL_FAST=true)
import "cypress-fail-fast";

// Patient Merge test support (008-patient-merge feature)
import "./patient-merge-setup";

// Capture browser console logs and forward to terminal
// This is especially important for Electron browser
// Note: Electron console logs are automatically shown when ELECTRON_ENABLE_LOGGING=1
// This handler captures console messages and logs them via Cypress commands when available
Cypress.on("window:before:load", (win) => {
  // Store original console methods
  const originalLog = win.console.log;
  const originalError = win.console.error;
  const originalWarn = win.console.warn;
  const originalInfo = win.console.info;

  // Override console methods to capture logs
  win.console.log = (...args) => {
    originalLog.apply(win.console, args);
    // Store in window for later retrieval
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "log",
      message: args
        .map((arg) =>
          typeof arg === "object" ? JSON.stringify(arg, null, 2) : String(arg),
        )
        .join(" "),
      timestamp: new Date().toISOString(),
    });
  };

  win.console.error = (...args) => {
    originalError.apply(win.console, args);
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "error",
      message: args
        .map((arg) =>
          typeof arg === "object" ? JSON.stringify(arg, null, 2) : String(arg),
        )
        .join(" "),
      timestamp: new Date().toISOString(),
    });
  };

  win.console.warn = (...args) => {
    originalWarn.apply(win.console, args);
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "warn",
      message: args
        .map((arg) =>
          typeof arg === "object" ? JSON.stringify(arg, null, 2) : String(arg),
        )
        .join(" "),
      timestamp: new Date().toISOString(),
    });
  };

  win.console.info = (...args) => {
    originalInfo.apply(win.console, args);
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "info",
      message: args
        .map((arg) =>
          typeof arg === "object" ? JSON.stringify(arg, null, 2) : String(arg),
        )
        .join(" "),
      timestamp: new Date().toISOString(),
    });
  };
});

// Let uncaught exceptions fail the test with the real error message.
// Returning true (default) causes Cypress to fail immediately with the
// actual JS error instead of a vague timeout like "button not found".
Cypress.on("uncaught:exception", (err, runnable) => {
  // Log for visibility in CI output
  console.error("[uncaught:exception]", err.message);
  // Return true (or omit return) to let Cypress fail the test
  return true;
});

// Fail-fast: Stop test run on first failure using official Cypress.stop() API
// https://docs.cypress.io/api/cypress-api/stop
afterEach(function () {
  if (this.currentTest?.state === "failed") {
    Cypress.stop();
  }
});
