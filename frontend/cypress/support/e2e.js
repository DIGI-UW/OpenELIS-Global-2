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

// DISABLED: Storage test support (001-sample-storage feature)
// Storage tests are temporarily disabled - uncomment to re-enable
// import "./load-storage-fixtures";
// import "./storage-setup";

// Capture browser console logs and forward to terminal
// This is especially important for Electron browser
// Queue logs and forward them via cy.task when in command context
let consoleLogQueue = [];

// Helper to format console message
const formatMessage = (args) => {
  return args
    .map((arg) => {
      if (typeof arg === "object") {
        try {
          return JSON.stringify(arg, null, 2);
        } catch (e) {
          return String(arg);
        }
      }
      return String(arg);
    })
    .join(" ");
};

// Process queued console logs when in command context
Cypress.on("test:before:run", () => {
  consoleLogQueue = [];
});

// Forward queued logs after each command
Cypress.on("command:enqueued", () => {
  if (consoleLogQueue.length > 0) {
    const logs = [...consoleLogQueue];
    consoleLogQueue = [];
    logs.forEach((log) => {
      cy.task("log", `[${log.type.toUpperCase()}] ${log.message}`, {
        log: true,
      });
    });
  }
});

Cypress.on("window:before:load", (win) => {
  // Store original console methods
  const originalLog = win.console.log;
  const originalError = win.console.error;
  const originalWarn = win.console.warn;
  const originalInfo = win.console.info;

  // Override console methods to capture and queue logs
  win.console.log = (...args) => {
    originalLog.apply(win.console, args);
    const message = formatMessage(args);
    consoleLogQueue.push({ type: "log", message });
    // Also store for later retrieval
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "log",
      message: message,
      timestamp: new Date().toISOString(),
    });
  };

  win.console.error = (...args) => {
    originalError.apply(win.console, args);
    const message = formatMessage(args);
    consoleLogQueue.push({ type: "error", message });
    // Also store for later retrieval
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "error",
      message: message,
      timestamp: new Date().toISOString(),
    });
  };

  win.console.warn = (...args) => {
    originalWarn.apply(win.console, args);
    const message = formatMessage(args);
    consoleLogQueue.push({ type: "warn", message });
    // Also store for later retrieval
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "warn",
      message: message,
      timestamp: new Date().toISOString(),
    });
  };

  win.console.info = (...args) => {
    originalInfo.apply(win.console, args);
    const message = formatMessage(args);
    consoleLogQueue.push({ type: "info", message });
    // Also store for later retrieval
    if (!win._cypressConsoleLogs) win._cypressConsoleLogs = [];
    win._cypressConsoleLogs.push({
      type: "info",
      message: message,
      timestamp: new Date().toISOString(),
    });
  };
});

// Capture uncaught exceptions and log them
// Note: We can't use cy.task() here, but Electron will show console errors automatically
Cypress.on("uncaught:exception", (err, runnable) => {
  // Electron console will show this automatically with ELECTRON_ENABLE_LOGGING=1
  // Return false to prevent Cypress from failing the test
  // This allows us to see the error but continue
  return false;
});
