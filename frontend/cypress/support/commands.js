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
    .should("have.value", value);
});

/**
 * Wait for backend API to be ready before running tests
 * Checks both login endpoint and optionally a specific REST endpoint
 */
Cypress.Commands.add("waitForBackend", (restEndpoint = null) => {
  // Wait for login endpoint
  cy.intercept("/api/OpenELIS-Global/LoginPage").as("backendReady");
  cy.visit("/");
  cy.wait("@backendReady", { timeout: 30000 });

  // If a REST endpoint is specified, wait for it too
  if (restEndpoint) {
    cy.intercept("GET", restEndpoint).as("restApiReady");
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
 * Load storage test fixtures (only if not already loaded)
 * Usage: cy.loadStorageFixtures()
 */
Cypress.Commands.add("loadStorageFixtures", () => {
  cy.task("loadStorageTestData");
});

/**
 * Check if storage test fixtures already exist
 * Usage: cy.checkStorageFixturesExist()
 */
Cypress.Commands.add("checkStorageFixturesExist", () => {
  return cy.task("checkStorageFixturesExist");
});

/**
 * Clean storage test fixtures
 * Usage: cy.cleanStorageFixtures()
 */
Cypress.Commands.add("cleanStorageFixtures", () => {
  cy.task("cleanStorageTestData");
});
