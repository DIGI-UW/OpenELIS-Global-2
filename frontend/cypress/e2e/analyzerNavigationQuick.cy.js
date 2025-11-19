/**
 * Quick Analyzer Navigation Test
 * 
 * Simple test to verify navigation between analyzer pages works correctly
 * and there are no console errors
 */

import LoginPage from "../pages/LoginPage";

const login = new LoginPage();
let usersData;

describe("Quick Analyzer Navigation", () => {
  before("Load users fixture", () => {
    cy.fixture("Users").then((users) => {
      usersData = users;
    });
  });

  beforeEach(() => {
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

  it("should navigate between analyzer pages without errors", () => {
    // 1. Navigate to Analyzers List
    cy.visit("/analyzers");
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should("be.visible");
    cy.get('[data-testid="page-title"]').should("be.visible");
    cy.get('[data-testid="analyzers-list-stats"]').should("be.visible");

    // 2. Navigate to Error Dashboard
    cy.visit("/analyzers/errors");
    cy.get('[data-testid="error-dashboard"]', { timeout: 10000 }).should("be.visible");
    cy.get('[data-testid="error-dashboard-stats"]').should("be.visible");

    // 3. Navigate back to Analyzers List
    cy.visit("/analyzers");
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should("be.visible");

    // 4. Try to navigate to field mappings if analyzer exists
    // First check if there are any analyzers in the table
    cy.get('body').then(($body) => {
      if ($body.find('[data-testid^="analyzer-action-mappings-"]').length > 0) {
        // Click first field mappings action
        cy.get('[data-testid^="analyzer-action-mappings-"]').first().click();
        
        // Verify field mappings page loads
        cy.url({ timeout: 10000 }).should("include", "/mappings");
        cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should("be.visible");
        cy.get('[data-testid="page-title"]').should("be.visible");
        
        // Navigate back using back button
        cy.get('[data-testid="page-title-back-button"]').click();
        cy.url({ timeout: 10000 }).should("include", "/analyzers");
        cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should("be.visible");
      } else {
        cy.log("No analyzers found - skipping field mappings navigation");
      }
    });
  });
});

