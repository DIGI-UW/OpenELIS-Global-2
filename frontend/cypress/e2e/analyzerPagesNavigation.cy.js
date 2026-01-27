/**
 * Simple Analyzer Pages Navigation Test
 *
 * Task Reference: T159 (simplified happy path)
 *
 * Tests that we can navigate to the three main analyzer pages:
 * 1. Analyzers List (/analyzers)
 * 2. Error Dashboard (/analyzers/errors)
 * 3. Field Mappings (via clicking on analyzer row)
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default
 * - Screenshots enabled on failure
 * - Uses data-testid selectors (PREFERRED)
 * - Simple happy path test
 *
 * Execution:
 * - Development: npm run cy:single "cypress/e2e/analyzerPagesNavigation.cy.js"
 */

// Basic auth credentials
const AUTH = {
  username: "admin",
  password: "adminADMIN!",
};

describe("Analyzer Pages Navigation", () => {
  before("Setup authentication", () => {
    // Wait for backend API to be available
    cy.waitForBackend("/api/OpenELIS-Global/rest/analyzer/analyzers");

    // Use cy.session() to cache and reuse basic auth session across tests
    cy.session("analyzer-tests-session", () => {
      // Establish session with basic auth by making an authenticated request
      cy.request({
        method: "GET",
        url: "/api/OpenELIS-Global/rest/analyzer/analyzers",
        auth: AUTH,
        failOnStatusCode: false,
      });
    });
  });

  beforeEach(() => {
    // Viewport management
    cy.viewport(1025, 900);
  });

  it("should navigate to analyzers list page", () => {
    // Navigate to analyzers list with basic auth
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);

    // Verify page loaded
    cy.get('[data-testid="analyzers-list"]').should("be.visible");
    cy.url().should("include", "/analyzers");
  });

  it("should navigate to error dashboard page", () => {
    // Navigate to error dashboard with basic auth
    cy.visit(
      `https://${AUTH.username}:${AUTH.password}@localhost/analyzers/errors`,
    );

    // Verify page loaded
    cy.get('[data-testid="error-dashboard"]').should("be.visible");
    cy.url().should("include", "/analyzers/errors");
  });

  it("should navigate to field mappings page via analyzer row", () => {
    // First go to analyzers list with basic auth
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Check if there are any analyzers in the table
    cy.get("body").then(($body) => {
      if (
        $body.find('[data-testid="analyzers-table-container"] tbody tr')
          .length > 0
      ) {
        // Click first field mappings action button
        cy.get('[data-testid="analyzers-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find('[data-testid^="analyzer-action-field-mappings"]')
          .should("be.visible")
          .click();

        // Verify field mappings page loaded
        cy.url().should("include", "/mappings");
        cy.get('[data-testid="field-mapping"]').should("be.visible");
      } else {
        cy.log(
          "No analyzers found in table - skipping field mappings navigation",
        );
      }
    });
  });
});
