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

// Storage test support (001-sample-storage feature)
import "./load-storage-fixtures";
import "./storage-setup";

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

// Capture uncaught exceptions and log them
// Note: We can't use cy.task() here, but Electron will show console errors automatically
Cypress.on("uncaught:exception", (err, runnable) => {
  // Electron console will show this automatically with ELECTRON_ENABLE_LOGGING=1
  // Return false to prevent Cypress from failing the test
  // This allows us to see the error but continue
  return false;
});

// Global Basic Auth setup for all requests
// Backend supports Basic Auth via BasicAuthRequestedMatcher (SecurityConfig.java)
// Basic Auth is enabled by default (matchIfMissing = true)
// Set up intercept in beforeEach so it applies to all tests
// Store credentials in Cypress env so they persist across tests
const DEFAULT_USERNAME = "admin";
const DEFAULT_PASSWORD = "adminADMIN!"; // Dev default (non-negotiable)

// Initialize default credentials
Cypress.env("BASIC_AUTH_USERNAME", DEFAULT_USERNAME);
Cypress.env("BASIC_AUTH_PASSWORD", DEFAULT_PASSWORD);

// Set up global intercept in beforeEach (runs before each test)
// This ensures Basic Auth header is added to ALL requests EXCEPT login page tests
// Login page tests (login.cy.js) test UI login flow, so they should NOT use Basic Auth
beforeEach(function () {
  // Skip Basic Auth setup for login.cy.js (tests UI login flow)
  if (Cypress.spec.name.includes("login.cy.js")) {
    return;
  }

  const username = Cypress.env("BASIC_AUTH_USERNAME") || DEFAULT_USERNAME;
  const password = Cypress.env("BASIC_AUTH_PASSWORD") || DEFAULT_PASSWORD;
  const base64Credentials = btoa(`${username}:${password}`);

  // Intercept all requests and add Basic Auth header if not present
  cy.intercept("**", (req) => {
    const authHeader =
      req.headers["authorization"] || req.headers["Authorization"];
    const isLoginPage =
      req.url.includes("/LoginPage") || req.url.includes("/ValidateLogin");

    if (!authHeader && !isLoginPage) {
      req.headers["authorization"] = `Basic ${base64Credentials}`;
    }
  });
});
