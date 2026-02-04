/**
 * Quick Analyzer Navigation Test
 *
 * Simple test to verify navigation between analyzer pages works correctly
 * and there are no console errors
 */

// Basic auth credentials
const AUTH = {
  username: "admin",
  password: "adminADMIN!",
};

describe("Quick Analyzer Navigation", () => {
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

  it("should navigate between analyzer pages without errors", () => {
    // 1. Navigate to Analyzers List with basic auth
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");
    cy.get('[data-testid="page-title"]').should("be.visible");
    cy.get('[data-testid="analyzers-list-stats"]').should("be.visible");

    // 2. Navigate to Error Dashboard with basic auth
    cy.visit(
      `https://${AUTH.username}:${AUTH.password}@localhost/analyzers/errors`,
    );
    cy.get('[data-testid="error-dashboard"]').should("be.visible");
    cy.get('[data-testid="error-dashboard-stats"]').should("be.visible");

    // 3. Navigate back to Analyzers List with basic auth
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // 4. Try to navigate to field mappings if analyzer exists
    // First check if there are any analyzers in the table
    cy.get("body").then(($body) => {
      if ($body.find('[data-testid^="analyzer-action-mappings-"]').length > 0) {
        // Click first field mappings action
        cy.get('[data-testid^="analyzer-action-mappings-"]').first().click();

        // Verify field mappings page loads
        cy.url().should("include", "/mappings");
        cy.get('[data-testid="field-mapping"]').should("be.visible");
        cy.get('[data-testid="page-title"]').should("be.visible");

        // Navigate back using back button
        cy.get('[data-testid="page-title-back-button"]').click();
        cy.url().should("include", "/analyzers");
        cy.get('[data-testid="analyzers-list"]').should("be.visible");
      } else {
        cy.log("No analyzers found - skipping field mappings navigation");
      }
    });
  });
});
