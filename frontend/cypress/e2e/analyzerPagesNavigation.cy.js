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
 * Credentials: Cypress.env('USERNAME') / Cypress.env('PASSWORD') (set via env or cypress.config.js)
 *
 * Execution:
 * - Development: npm run cy:single "cypress/e2e/analyzerPagesNavigation.cy.js"
 */

const auth = () => ({
  username: Cypress.env("USERNAME"),
  password: Cypress.env("PASSWORD"),
});

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
        auth: auth(),
        failOnStatusCode: false,
      });
    });
  });

  beforeEach(() => {
    // Viewport management
    cy.viewport(1025, 900);
  });

  it("should navigate to analyzers list page", () => {
    // Navigate to analyzers list with basic auth (credentials from env, not in URL)
    cy.visit("/analyzers", { auth: auth() });

    // Verify page loaded
    cy.get('[data-testid="analyzers-list"]').should("be.visible");
    cy.url().should("include", "/analyzers");
  });

  it("should navigate to error dashboard page", () => {
    // Navigate to error dashboard with basic auth
    cy.visit("/analyzers/errors", { auth: auth() });

    // Verify page loaded
    cy.get('[data-testid="error-dashboard"]').should("be.visible");
    cy.url().should("include", "/analyzers/errors");
  });

  it("should navigate to field mappings page via analyzer row", () => {
    // First go to analyzers list with basic auth
    cy.visit("/analyzers", { auth: auth() });
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Check if there are any analyzers in the table
    cy.get("body").then(($body) => {
      if (
        $body.find('[data-testid="analyzers-table-container"] tbody tr')
          .length > 0
      ) {
        // Click the overflow menu trigger button to open the actions menu
        // Carbon OverflowMenu requires clicking the trigger first
        cy.get('[data-testid="analyzers-table-container"]')
          .find("tbody")
          .find("tr")
          .first()
          .find('[data-testid^="analyzer-actions-"]')
          .find("button")
          .click();

        // Now click the field mappings menu item (visible after menu opens)
        cy.get('[data-testid^="analyzer-action-mappings-"]')
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
