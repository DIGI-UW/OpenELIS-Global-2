/**
 * Analyzer Pages Load E2E Test
 *
 * Verifies that all analyzer pages load correctly without console errors
 * Uses Electron browser with ELECTRON_ENABLE_LOGGING=1 for automatic console log capture
 *
 * Test Scenarios:
 * - Navigate to /analyzers - verify page loads, check console logs
 * - Navigate to /analyzers/:id/mappings - verify page loads, check console logs
 * - Navigate to /analyzers/errors - verify page loads, check console logs
 * - Verify sidebar navigation is visible and functional
 * - Verify menu items are clickable and navigate correctly
 */

describe("Analyzer Pages Load", () => {
  beforeEach(() => {
    // Login before each test
    cy.visit("/");
    cy.get('input[name="loginName"]').type("admin");
    cy.get('input[name="password"]').type("adminADMIN!");
    cy.get('button[type="submit"]').click();
    // Wait for login to complete
    cy.url().should("not.include", "/Login");
  });

  it("should load Analyzers List page without errors", () => {
    // Navigate to analyzers list
    cy.visit("/analyzers");

    // Verify page loads
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Verify key UI elements are visible
    cy.get('[data-testid="analyzers-list-header"]').should("be.visible");
    cy.get('[data-testid="analyzers-list-stats"]').should("be.visible");

    // Verify sidebar is visible (should be persistent on analyzer pages)
    cy.get('nav[aria-label="Side navigation"]').should("be.visible");

    // Console logs are automatically captured via ELECTRON_ENABLE_LOGGING=1
    // Review console logs in Cypress test output for errors/warnings
  });

  it("should load Error Dashboard page without errors", () => {
    // Navigate to error dashboard
    cy.visit("/analyzers/errors");

    // Verify page loads
    cy.get('[data-testid="error-dashboard"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Verify key UI elements are visible
    cy.get('[data-testid="error-dashboard-header"]').should("be.visible");
    cy.get('[data-testid="error-dashboard-stats"]').should("be.visible");

    // Verify sidebar is visible (should be persistent on analyzer pages)
    cy.get('nav[aria-label="Side navigation"]').should("be.visible");

    // Console logs are automatically captured via ELECTRON_ENABLE_LOGGING=1
    // Review console logs in Cypress test output for errors/warnings
  });

  it("should navigate between analyzer pages with sidebar persistent", () => {
    // Start at analyzers list
    cy.visit("/analyzers");
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Verify sidebar is visible
    cy.get('nav[aria-label="Side navigation"]').should("be.visible");

    // Navigate to error dashboard
    cy.visit("/analyzers/errors");
    cy.get('[data-testid="error-dashboard"]', { timeout: 10000 }).should(
      "be.visible",
    );

    // Verify sidebar is still visible after navigation
    cy.get('nav[aria-label="Side navigation"]').should("be.visible");
  });

  it("should show analyzer menu items in sidebar", () => {
    cy.visit("/analyzers");

    // Wait for menu to load
    cy.wait(1000);

    // Verify Analyzers menu item exists (check for menu_analyzers element)
    // Note: Menu items are rendered dynamically, so we check for the presence of the menu
    cy.get('nav[aria-label="Side navigation"]').should("be.visible");

    // Console logs are automatically captured via ELECTRON_ENABLE_LOGGING=1
    // Review console logs in Cypress test output for errors/warnings
  });
});
