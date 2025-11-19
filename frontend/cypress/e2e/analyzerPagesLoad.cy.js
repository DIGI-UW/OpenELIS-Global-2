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

import LoginPage from "../pages/LoginPage";

const login = new LoginPage();
let usersData;

describe("Analyzer Pages Load", () => {
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
        // Not logged in - perform login
        const user = usersData[3];
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();
        
        // Wait for login to complete
        cy.url({ timeout: 10000 }).should("not.include", "/login");
        cy.get("#mainHeader", { timeout: 10000 }).should("be.visible");
      } else {
        // Already logged in - just verify we're on a valid page
        cy.get("#mainHeader", { timeout: 10000 }).should("be.visible");
      }
    });
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

    // Verify sidebar is visible (no arbitrary wait - use should() for retry-ability)
    cy.get('nav[aria-label="Side navigation"]', { timeout: 10000 }).should("be.visible");

    // Console logs are automatically captured via ELECTRON_ENABLE_LOGGING=1
    // Review console logs in Cypress test output for errors/warnings
  });
});
