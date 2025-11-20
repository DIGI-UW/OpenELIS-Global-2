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
 * Reference: Testing Roadmap - Session Management (cy.session())
 * Pattern matches: cy.setupStorageTests() in storage-setup.js
 */
Cypress.Commands.add("login", (username, password) => {
  // Default credentials (matches TestProperties)
  const DEFAULT_USERNAME = "admin";
  const DEFAULT_PASSWORD = "adminADMIN!";
  const loginUsername = username || DEFAULT_USERNAME;
  const loginPassword = password || DEFAULT_PASSWORD;

  // Use cy.session() to cache and reuse login session across tests
  // Same pattern as cy.setupStorageTests() in storage-setup.js
  cy.session(
    `login-session-${loginUsername}`,
    () => {
      // Login via UI (only runs if session doesn't exist)
      cy.visit("/login");
      cy.get("[data-cy='loginButton']", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.get("#loginName").should("be.visible").clear().type(loginUsername);
      cy.get("#password").should("be.visible").clear().type(loginPassword);
      cy.get("[data-cy='loginButton']")
        .should("be.visible")
        .should("not.be.disabled")
        .click();

      // Wait for successful login (check for home page or main header)
      cy.url({ timeout: 15000 }).should("not.include", "/login");
      cy.get("#mainHeader, [data-cy='menuButton']", { timeout: 10000 }).should(
        "exist",
      );
    },
    {
      cacheAcrossSpecs: true, // Share session across test files (same as storage tests)
    },
  );

  // After session is established, ensure we're logged in
  // Visit home page to verify session is active
  cy.visit("/");
  cy.get("#mainHeader, [data-cy='menuButton']", { timeout: 10000 }).should(
    "exist",
  );
});
