/**
 * E2E Test: Analyzer Field Mappings Display
 *
 * Verifies that the field mappings page opens and displays mappings correctly.
 * Simple test to ensure mappings appear on the page.
 */

// Basic auth credentials
const AUTH = {
  username: "admin",
  password: "adminADMIN!",
};

describe("Analyzer Field Mappings Display", () => {
  before("Setup authentication", () => {
    // Wait for backend API to be available
    cy.waitForBackend("/rest/analyzer/analyzers");

    // Use cy.session() to cache and reuse basic auth session across tests
    cy.session("analyzer-tests-session", () => {
      // Establish session with basic auth by making an authenticated request
      cy.request({
        method: "GET",
        url: "/rest/analyzer/analyzers",
        auth: AUTH,
        failOnStatusCode: false,
      });
    });
  });

  beforeEach(() => {
    cy.viewport(1025, 900);
  });

  it("should open field mappings page and display mappings when available", () => {
    // Navigate to analyzers list with basic auth
    cy.visit(`https://${AUTH.username}:${AUTH.password}@localhost/analyzers`);
    cy.get('[data-testid="analyzers-list"]').should("be.visible");
    cy.get('[data-testid="analyzers-table-container"]', {
      timeout: 10000,
    }).should("be.visible");

    // Check if there are any analyzers in the table
    cy.get("body").then(($body) => {
      const analyzerRows = $body.find(
        '[data-testid="analyzers-table-container"] tbody tr',
      );

      if (analyzerRows.length > 0) {
        // Get first analyzer row ID
        const firstRowId = analyzerRows
          .first()
          .attr("id")
          ?.replace("analyzer-row-", "");

        if (firstRowId) {
          // Open overflow menu for first analyzer
          cy.get(`#analyzer-row-${firstRowId}`)
            .find('[data-testid^="analyzer-actions-"]')
            .find('[role="button"]')
            .click();

          // Click the mappings menu item using the correct test ID: analyzer-action-mappings-{row.id}
          cy.get(`[data-testid="analyzer-action-mappings-${firstRowId}"]`)
            .should("be.visible")
            .click();

          // Wait for field mappings page to load
          cy.url().should("include", "/mappings");
          cy.get('[data-testid="field-mapping"]').should("be.visible");

          // Verify page elements are visible
          cy.get('[data-testid="field-mapping-stats"]').should("be.visible");
          cy.get('[data-testid="stat-total-mappings"]').should("be.visible");
          cy.get('[data-testid="stat-required-mappings"]').should("be.visible");
          cy.get('[data-testid="stat-unmapped-fields"]').should("be.visible");

          // Verify field mapping panel is visible
          cy.get('[data-testid="field-mapping-panel"]').should("be.visible");
          cy.get('[data-testid="field-mapping-table-container"]').should(
            "be.visible",
          );

          // Verify that table is rendered
          cy.get('[data-testid="field-mapping-table"]').should("be.visible");
          cy.log(
            "Field mappings page opened successfully and displays correctly",
          );
        } else {
          cy.log(
            "Could not extract analyzer ID from row - skipping mappings test",
          );
        }
      } else {
        cy.log("No analyzers found in table - skipping field mappings test");
      }
    });
  });
});
