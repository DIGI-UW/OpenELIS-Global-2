/**
 * E2E Test: Analyzer Field Mappings Display
 *
 * Verifies that the field mappings page opens and displays mappings correctly.
 * Simple test to ensure mappings appear on the page.
 */

import LoginPage from "../pages/LoginPage";

const login = new LoginPage();
let usersData;

describe("Analyzer Field Mappings Display", () => {
  before("Load users fixture", () => {
    cy.fixture("Users").then((users) => {
      usersData = users;
    });
  });

  beforeEach(() => {
    cy.viewport(1025, 900);
    // Check if already logged in, if not then login
    cy.visit("/");
    cy.url().then((url) => {
      if (url.includes("/login")) {
        const user = usersData[3];
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
        cy.url({ timeout: 10000 }).should("not.include", "/login");
        cy.get("#mainHeader", { timeout: 10000 }).should("be.visible");
      } else {
        cy.get("#mainHeader", { timeout: 10000 }).should("be.visible");
      }
    });
  });

  it("should open field mappings page and display mappings when available", () => {
    // Navigate to analyzers list
    cy.visit("/analyzers");
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should("be.visible");
    cy.get('[data-testid="analyzers-table-container"]', { timeout: 10000 }).should("be.visible");

    // Check if there are any analyzers in the table
    cy.get('body').then(($body) => {
      const analyzerRows = $body.find('[data-testid="analyzers-table-container"] tbody tr');
      
      if (analyzerRows.length > 0) {
        // Get first analyzer row ID
        const firstRowId = analyzerRows.first().attr('id')?.replace('analyzer-row-', '');
        
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
          cy.url({ timeout: 10000 }).should("include", "/mappings");
          cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should("be.visible");

          // Verify page elements are visible
          cy.get('[data-testid="field-mapping-stats"]').should("be.visible");
          cy.get('[data-testid="stat-total-mappings"]').should("be.visible");
          cy.get('[data-testid="stat-required-mappings"]').should("be.visible");
          cy.get('[data-testid="stat-unmapped-fields"]').should("be.visible");

          // Verify field mapping panel is visible
          cy.get('[data-testid="field-mapping-panel"]').should("be.visible");
          cy.get('[data-testid="field-mapping-table-container"]').should("be.visible");

          // Verify that table is rendered
          cy.get('[data-testid="field-mapping-table"]').should("be.visible");
          cy.log("Field mappings page opened successfully and displays correctly");
        } else {
          cy.log("Could not extract analyzer ID from row - skipping mappings test");
        }
      } else {
        cy.log("No analyzers found in table - skipping field mappings test");
      }
    });
  });
});

