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
 * Uses direct cy.request() for reliability (no intercept timing issues)
 * Per Constitution V.5: Avoid intercept timing issues by using direct requests
 */
Cypress.Commands.add("waitForBackend", (restEndpoint = null) => {
  // Use cy.request() to verify backend is responding (more reliable than intercepts)
  // This avoids the timing issue where intercepts may be set up after page load starts
  const checkEndpoint = restEndpoint || "/api/OpenELIS-Global/rest/menu";

  cy.request({
    method: "GET",
    url: checkEndpoint,
    failOnStatusCode: false, // Don't fail on 401/403/404 - just verify backend responds
    timeout: 30000,
    retryOnStatusCodeFailure: false,
  }).then((response) => {
    // Backend is responding if we get any HTTP status code
    expect(response.status).to.be.a("number");
    cy.log(
      `Backend ready: ${checkEndpoint} responded with status ${response.status}`,
    );
  });
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

/**
 * Ensure user is logged out via API (proper auth check, not DOM-based)
 * Checks /session endpoint and calls /Logout if authenticated
 * Usage: cy.ensureLoggedOut()
 */
Cypress.Commands.add("ensureLoggedOut", () => {
  // Check authentication status via API (same endpoint the app uses)
  cy.request({
    url: "/api/OpenELIS-Global/session",
    failOnStatusCode: false,
    credentials: "include",
  }).then((response) => {
    if (response.status === 200 && response.body?.authenticated === true) {
      // User is authenticated - logout via API (same as app does)
      const csrfToken = response.body.csrf || "";
      cy.request({
        method: "POST",
        url: "/api/OpenELIS-Global/Logout",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": csrfToken,
        },
        credentials: "include",
        failOnStatusCode: false,
      }).then(() => {
        // Verify logout succeeded
        cy.request({
          url: "/api/OpenELIS-Global/session",
          failOnStatusCode: false,
          credentials: "include",
        }).then((verifyResponse) => {
          // After logout, session should return authenticated: false or 401/403
          if (
            verifyResponse.status === 200 &&
            verifyResponse.body?.authenticated === true
          ) {
            cy.log(
              "Warning: Logout API call succeeded but session still authenticated",
            );
          }
        });
      });
    }
    // If not authenticated, nothing to do
  });
});
