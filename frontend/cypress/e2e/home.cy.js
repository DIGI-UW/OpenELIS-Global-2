/**
 * E2E Tests for Home Page
 * Tests navigation bar interactions and dashboard tile navigation
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/home.cy.js"
 */

import LoginPage from "../pages/LoginPage";

let loginPage = null;
let home = null;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before(() => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  loginPage = new LoginPage();
  home = loginPage.goToHomePage();
});

describe("User interacts with the navigation bar", function () {
  beforeEach(() => {
    // Navigate to home page for each test
    cy.visit("/");
    // Wait for home page to be ready
    cy.get("#mainHeader, [data-cy='menuButton']", { timeout: 10000 })
      .should("exist");
  });

  it("User searches for patient and closes search bar", function () {
    home.searchBar();
  });

  it("User checks for notifications and closes it", function () {
    home.clickNotifications();
  });

  it("User interacts with the user icon", function () {
    home.clickUserIcon();
  });

  it("User interacts with the help icon", function () {
    home.clickHelpIcon();
  });
});

describe("User navigates to different tiles", function () {
  beforeEach(() => {
    // Navigate to home page for each test
    cy.visit("/");
    // Wait for home page to be ready
    cy.get("#mainHeader, [data-cy='menuButton']", { timeout: 10000 })
      .should("exist");
  });

  it("User navigates to the In Progress", function () {
    home.selectInProgress();
  });

  it("User navigates to Ready for Validation", function () {
    home.selectReadyforValidation();
  });

  it("User navigates to Orders Completed Today", function () {
    home.selectOrdersCompletedToday();
  });

  it("User navigates to Partially Completed Today", function () {
    home.selectPartiallyCompletedToday();
  });

  it("User navigates to Orders Entered By Users", function () {
    home.selectOrdersEnteredByUsers();
  });

  it("User navigates to Orders Rejected", function () {
    home.selectOrdersRejected();
  });

  it("User navigates to UnPrinted Results", function () {
    home.selectUnPrintedResults();
  });

  it("User navigates to Electronic Orders", function () {
    home.selectElectronicOrders();
  });

  it("User navigates to Average Turn Around time", function () {
    home.selectAverageTurnAroundTime();
  });

  it("User navigates to Delayed Turn Around", function () {
    home.selectDelayedTurnAround();
  });
});
