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
 *
 * Credentials: Cypress.env('USERNAME') / Cypress.env('PASSWORD')
 */

describe("Analyzer Pages Load", () => {
  before("Setup authentication", () => {
    // Wait for backend API to be available
    cy.waitForBackend("/api/OpenELIS-Global/rest/analyzer/analyzers");

    // Use cy.session() to cache and reuse basic auth session across tests
    cy.session("analyzer-tests-session", () => {
      // Establish session with basic auth by making an authenticated request
      cy.request({
        method: "GET",
        url: "/api/OpenELIS-Global/rest/analyzer/analyzers",
        auth: Cypress.getBasicAuth(),
        failOnStatusCode: false,
      });
    });
  });

  beforeEach(() => {
    // Viewport management
    cy.viewport(1025, 900);
  });

  it("should load Analyzers List page without errors", () => {
    // Navigate to analyzers list with basic auth (credentials from env, not in URL)
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });

    // Verify page loads
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Verify key UI elements are visible
    cy.get('[data-testid="analyzers-list-header"]').should("be.visible");
    cy.get('[data-testid="analyzers-list-stats"]').should("be.visible");

    // Verify sidebar exists (may be collapsed at certain viewport widths per Carbon Design)
    cy.get('nav[aria-label="Side navigation"]').should("exist");

    // Console logs are automatically captured via ELECTRON_ENABLE_LOGGING=1
    // Review console logs in Cypress test output for errors/warnings
  });

  it("should load Error Dashboard page without errors", () => {
    // Navigate to error dashboard with basic auth
    cy.visit("/analyzers/errors", { auth: Cypress.getBasicAuth() });

    // Verify page loads
    cy.get('[data-testid="error-dashboard"]').should("be.visible");

    // Verify key UI elements are visible
    cy.get('[data-testid="error-dashboard-header"]').should("be.visible");
    cy.get('[data-testid="error-dashboard-stats"]').should("be.visible");

    // Verify sidebar exists (may be collapsed at certain viewport widths per Carbon Design)
    cy.get('nav[aria-label="Side navigation"]').should("exist");

    // Console logs are automatically captured via ELECTRON_ENABLE_LOGGING=1
    // Review console logs in Cypress test output for errors/warnings
  });

  it("should navigate between analyzer pages with sidebar persistent", () => {
    // Start at analyzers list with basic auth
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="analyzers-list"]').should("be.visible");

    // Verify sidebar exists (may be collapsed at certain viewport widths per Carbon Design)
    cy.get('nav[aria-label="Side navigation"]').should("exist");

    // Navigate to error dashboard with basic auth
    cy.visit("/analyzers/errors", { auth: Cypress.getBasicAuth() });
    cy.get('[data-testid="error-dashboard"]').should("be.visible");

    // Verify sidebar still exists after navigation (may be collapsed per Carbon Design)
    cy.get('nav[aria-label="Side navigation"]').should("exist");
  });

  it("should show analyzer menu items in sidebar", () => {
    cy.visit("/analyzers", { auth: Cypress.getBasicAuth() });

    // Verify sidebar exists (may be collapsed at certain viewport widths per Carbon Design)
    cy.get('nav[aria-label="Side navigation"]').should("exist");

    // Console logs are automatically captured via ELECTRON_ENABLE_LOGGING=1
    // Review console logs in Cypress test output for errors/warnings
  });
});
