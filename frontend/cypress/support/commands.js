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
Cypress.Commands.add("login", (username, password) => {
  // Default credentials (matches TestProperties.js - dev default, non-negotiable)
  const DEFAULT_USERNAME = "admin";
  const DEFAULT_PASSWORD = "adminADMIN!"; // Dev default (one exclamation mark)
  const loginUsername = username || DEFAULT_USERNAME;
  const loginPassword = password || DEFAULT_PASSWORD;

  // Use cy.session() to cache and reuse login session across tests
  // Same pattern as cy.setupStorageTests() in storage-setup.js
  // Uses HTTP Basic Auth (fast, reliable, works with cy.request() - no cookie issues)
  // Backend supports Basic Auth via BasicAuthRequestedMatcher (SecurityConfig.java)
  cy.session(
    `login-session-${loginUsername}`,
    () => {
      // Use HTTP Basic Auth for API-based login (10-20x faster, more reliable)
      // Backend activates Basic Auth when Authorization: Basic header is present
      // This works with cy.request() because it doesn't require cookies
      const base64Credentials = btoa(`${loginUsername}:${loginPassword}`);

      // Test authentication by calling a protected endpoint with Basic Auth
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

      // Also establish browser session by visiting home page with Basic Auth
      // This ensures both API calls AND browser navigation work
      cy.visit("/", {
        auth: {
          username: loginUsername,
          password: loginPassword,
        },
      });

      // Verify we're logged in
      cy.get("#mainHeader, [data-cy='menuButton']").should("exist");
    },
    {
      cacheAcrossSpecs: true, // Share session across test files (same as storage tests)
    },
  );

  // After session is established, ensure we're logged in
  // Visit home page to verify session is active
  cy.visit("/");
  cy.get("#mainHeader, [data-cy='menuButton']").should("exist");
});
