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

import LoginPage from "../pages/LoginPage";

const login = new LoginPage();
let usersData;

describe("Analyzer Pages Navigation", () => {
  before("Load users fixture", () => {
    cy.fixture("Users").then((users) => {
      usersData = users;
    });
  });

  beforeEach(() => {
    // Viewport management
    cy.viewport(1025, 900);

    // Login if needed
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

  it("should navigate to analyzers list page", () => {
    // Navigate to analyzers list
    cy.visit("/analyzers");

    // Verify page loaded
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );
    cy.url().should("include", "/analyzers");
  });

  it("should navigate to error dashboard page", () => {
    // Navigate to error dashboard
    cy.visit("/analyzers/errors");

    // Verify page loaded
    cy.get('[data-testid="error-dashboard"]', { timeout: 10000 }).should(
      "be.visible",
    );
    cy.url().should("include", "/analyzers/errors");
  });

  it("should navigate to field mappings page via analyzer row", () => {
    // First go to analyzers list
    cy.visit("/analyzers");
    cy.get('[data-testid="analyzers-list"]', { timeout: 10000 }).should(
      "be.visible",
    );

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
        cy.url({ timeout: 10000 }).should("include", "/mappings");
        cy.get('[data-testid="field-mapping"]', { timeout: 10000 }).should(
          "be.visible",
        );
      } else {
        cy.log(
          "No analyzers found in table - skipping field mappings navigation",
        );
      }
    });
  });
});
