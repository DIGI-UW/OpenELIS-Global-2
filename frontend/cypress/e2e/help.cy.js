/**
 * E2E Tests for Help Page
 * Tests help page navigation and documentation links
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/help.cy.js"
 */

import LoginPage from "../pages/LoginPage";

describe("Interacts with Help options", function () {
  let loginPage, homePage, helpPage;

  // Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
  // Same pattern as cy.setupStorageTests() in storage-setup.js
  before(() => {
    cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
    // Navigate to home page after login
    loginPage = new LoginPage();
    homePage = loginPage.goToHomePage();
    helpPage = homePage.goToHelp();
    
    // Verify we're on the help page
    cy.url().should("include", "/Help");
  });

  it("User navigates to User Manual", function () {
    cy.window().then((win) => {
      cy.stub(win, "open").as("windowOpen"); // Stub to prevent opening a new tab
    });

    // Wait for help page to be ready
    cy.get("body", { timeout: 10000 })
      .should("be.visible");

    helpPage.clickUserManual();

    cy.get("@windowOpen").should("be.calledWithMatch", /\/docs\/UserManual/);
  });

  describe("User navigates to Process Documentation", function () {
    it("Navigates to Help", function () {
      // Navigate to help page
      cy.visit("/Help");
      
      // Wait for help page to be ready
      cy.get("body", { timeout: 10000 })
        .should("be.visible");
      
      helpPage.clickProcessDocumentation();
    });

    it("User opens VL Form", function () {
      cy.window().then((win) => {
        cy.stub(win, "open").as("windowOpen"); // Stub to prevent opening a new tab
      });

      // Wait for process documentation to be ready
      cy.get("body", { timeout: 10000 })
        .should("be.visible");

      helpPage.clickVLForm();

      cy.get("@windowOpen").should(
        "be.calledWithMatch",
        /\/documentation\/FICHE_DEMANDE_CHARGE_VIRALE_VF_\d+\.pdf/,
      );
    });

    it("User opens DBS Form", function () {
      cy.window().then((win) => {
        cy.stub(win, "open").as("windowOpen"); // Stub to prevent opening a new tab
      });

      // Wait for process documentation to be ready
      cy.get("body", { timeout: 10000 })
        .should("be.visible");

      helpPage.clickDBSForm();

      cy.get("@windowOpen").should(
        "be.calledWithMatch",
        /\/documentation\/DBS_Identn_\d+[A-Za-z]+\d+\.pdf/,
      );
    });
  });
});
