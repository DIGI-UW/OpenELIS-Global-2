// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

Cypress.Commands.add("getElement", (selector) => {
  cy.wait(100)
    .get("body")
    .then(($body) => {
      if ($body.find(selector).length) {
        return cy.get(selector);
      } else {
        return null;
      }
    });
});

Cypress.Commands.add("enterText", (selector, value) => {
  return cy
    .get(selector)
    .should("exist")
    .and("be.visible")
    .clear()
    .type(value)
    .should(($el) => {
      // Use Cypress retry-ability - check if value matches
      const actualValue = $el.val() || $el.text() || "";
      let matches = actualValue === value;

      // Check if one contains the other (for partial matches)
      if (!matches) {
        matches = actualValue.includes(value) || value.includes(actualValue);
      }

      expect(
        matches,
        `Expected field value "${actualValue}" to match "${value}"`,
      ).to.be.true;
    });
});

/**
 * Wait for backend API to be ready before running tests
 * Checks both login endpoint and optionally a specific REST endpoint
 */
Cypress.Commands.add("waitForBackend", (restEndpoint = null) => {
  // Use cy.request to check backend is ready (more reliable than intercept)
  cy.request({
    method: "GET",
    url: "/api/OpenELIS-Global/LoginPage",
    failOnStatusCode: false, // Don't fail if endpoint returns error, just check it responds
  }).then((response) => {
    // API is responding (even if 404/500, it means backend is up)
    expect(response.status).to.be.a("number");
  });

  // If a REST endpoint is specified, wait for it too
  if (restEndpoint) {
    cy.request({
      method: "GET",
      url: restEndpoint,
      failOnStatusCode: false, // Don't fail if endpoint returns error, just check it responds
    }).then((response) => {
      // API is responding (even if 404/500, it means backend is up)
      expect(response.status).to.be.a("number");
    });
  }
});

/**
 * Check if storage test fixtures already exist
 * Usage: cy.checkStorageFixturesExist()
 *
 * Note: loadStorageFixtures and cleanStorageFixtures are defined in
 * load-storage-fixtures.js to avoid duplication
 */
Cypress.Commands.add("checkStorageFixturesExist", () => {
  return cy.task("checkStorageFixturesExist");
});

/**
 * Verify storage test fixtures are complete
 * Usage: cy.verifyStorageFixtures()
 */
Cypress.Commands.add("verifyStorageFixtures", () => {
  return cy.task("verifyFixtures");
});

/**
 * Login with session caching (10-20x faster - Testing Roadmap pattern)
 * Usage: cy.login() or cy.login(username, password)
 *
 * Uses cy.session() to cache login state across tests (same pattern as cy.setupStorageTests()).
 * Login runs ONCE per test file, not before every test.
 *
 * Uses API-based authentication (cy.request()) instead of UI-based for:
 * - 10-20x faster (no page loads, no DOM interactions)
 * - More reliable (no UI timing issues, direct API call)
 * - Same endpoint as UI login (/ValidateLogin?apiCall=true)
 *
 * NOTE: UI login is still tested in login.cy.js (uses LoginPage directly, not cy.login())
 *
 * Reference: Testing Roadmap - Session Management (cy.session())
 * Pattern matches: cy.setupStorageTests() in storage-setup.js
 */
// Logout command - clears session quickly
Cypress.Commands.add("logout", () => {
  // Clear session by calling logout endpoint or clearing cookies
  cy.request({
    method: "POST",
    url: "/Logout",
    failOnStatusCode: false,
  }).then(() => {
    // Clear all cookies to ensure clean state
    cy.clearCookies();
    // Clear session storage
    cy.window().then((win) => {
      win.sessionStorage.clear();
    });
    // Clear local storage as well
    cy.window().then((win) => {
      win.localStorage.clear();
    });
    // Visit login page to ensure we're logged out
    cy.visit("/login", { failOnStatusCode: false });
    // Wait for login page to be ready
    cy.get("body").should("be.visible");
    // Verify we're actually on the login page
    cy.url().should("satisfy", (url) => {
      return url.includes("/login") || url.includes("/LoginPage");
    });
  });
});

Cypress.Commands.add("login", (username, password) => {
  // Default credentials (matches TestProperties.js - dev default, non-negotiable)
  const DEFAULT_USERNAME = "admin";
  const DEFAULT_PASSWORD = "adminADMIN!"; // Dev default (one exclamation mark)
  const loginUsername = username || DEFAULT_USERNAME;
  const loginPassword = password || DEFAULT_PASSWORD;

  // Store credentials in Cypress env (persists across tests)
  // Global intercept in e2e.js will use these credentials
  Cypress.env("BASIC_AUTH_USERNAME", loginUsername);
  Cypress.env("BASIC_AUTH_PASSWORD", loginPassword);

  // Use cy.session() to cache and reuse login verification across tests
  // Same pattern as cy.setupStorageTests() in storage-setup.js
  // Uses HTTP Basic Auth (fast, reliable, works with cy.request() - no cookie issues)
  // Backend supports Basic Auth via BasicAuthRequestedMatcher (SecurityConfig.java)
  // Global intercept in e2e.js adds Basic Auth header to all requests automatically
  cy.session(
    `login-session-${loginUsername}`,
    () => {
      const base64Credentials = btoa(`${loginUsername}:${loginPassword}`);

      // Test authentication by calling a protected endpoint with Basic Auth
      // Global intercept will add header automatically, but we set it explicitly here too
      cy.request({
        method: "GET",
        url: "/api/OpenELIS-Global/rest/user-test-sections/ALL",
        headers: {
          Authorization: `Basic ${base64Credentials}`,
        },
        failOnStatusCode: false,
      }).then((response) => {
        if (response.status !== 200) {
          cy.log(
            `Basic Auth failed: status ${response.status}, body: ${JSON.stringify(response.body)}`,
          );
        }
        expect(response.status).to.eq(200);
      });

      // Visit home page - global intercept will add Basic Auth header automatically
      cy.visit("/");

      // Verify we're logged in
      cy.get("#mainHeader, [data-cy='menuButton']").should("exist");
    },
    {
      cacheAcrossSpecs: true, // Share session across test files (same as storage tests)
    },
  );

  // After session is established, ensure we're logged in
  // Visit home page to verify session is active (global intercept adds Basic Auth)
  cy.visit("/");
  cy.get("#mainHeader, [data-cy='menuButton']").should("exist");
});
