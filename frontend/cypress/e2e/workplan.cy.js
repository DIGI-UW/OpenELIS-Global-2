/**
 * E2E Tests for Workplan (By Panel, By Unit, By Priority)
 * Tests workplan filtering and printing functionality
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/workplan.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import OrderEntityPage from "../pages/OrderEntityPage";
import AdminPage from "../pages/AdminPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let workplan = null;
let orderEntityPage = new OrderEntityPage();
let patientEntryPage = new PatientEntryPage();
let adminPage = new AdminPage();

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Work plan by Panel", function () {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/workplan**").as("getWorkplan");
    cy.intercept("POST", "**/rest/PrintWorkplanReport**").as("printWorkplan");
  });

  it("User can select work plan by test from main menu drop-down. Workplan by panel page appears.", function () {
    workplan = homePage.goToWorkPlanPlanByPanel();

    // Verify we're on the workplan page
    cy.url().should("include", "/WorkplanByPanel");

    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.panelTile);
    });
  });

  it("User should select panel from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/workplan**").as("getWorkplan");

      // Wait for dropdown to be ready
      cy.get("select#select-1", { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled");

      workplan.selectDropdownOption(options.bilanPanelType);

      // Wait for workplan API call after selection
      cy.wait("@getWorkplan", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      // Wait for table to populate (button only appears when testsList.length > 0)
      cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 })
        .should("be.visible")
        .find("tbody tr")
        .should("have.length.greaterThan", 0);

      // Now Print Workplan button should be visible (button id="print", text="Print Workplan")
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      // Testing Roadmap: Use element readiness checks, wait for table rows
      // Wait for table to be ready
      cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 }).should(
        "be.visible",
      );

      workplan
        .getWorkPlanResultsTable()
        .find("tbody tr", { timeout: 10000 })
        .should("have.length.greaterThan", 0)
        .then(($rows) => {
          // Check if any row contains the lab number
          const found = Array.from($rows).some((row) =>
            row.textContent.includes(options.labNo),
          );
          expect(
            found,
            `Expected to find lab number ${options.labNo} in workplan results`,
          ).to.be.true;
        });
    });
  });
});

describe("Work plan by Unit", function () {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/workplan**").as("getWorkplan");
    cy.intercept("POST", "**/rest/PrintWorkplanReport**").as("printWorkplan");
  });

  it("User can select work plan By Unit from main menu drop-down. Workplan By Unit page appears.", function () {
    workplan = homePage.goToWorkPlanPlanByUnit();

    // Verify we're on the workplan page
    cy.url().should("include", "/WorkplanByUnit");

    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.unitTile);
    });
  });

  it("User should select unit type from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/workplan**").as("getWorkplan");

      // Wait for dropdown to be ready
      cy.get("select#select-1", { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled");

      workplan.selectDropdownOption(options.unitType);

      // Wait for workplan API call after selection
      cy.wait("@getWorkplan", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      // Wait for table to populate (button only appears when testsList.length > 0)
      cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 })
        .should("be.visible")
        .find("tbody tr")
        .should("have.length.greaterThan", 0);

      // Now Print Workplan button should be visible
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      // Testing Roadmap: Use element readiness checks, wait for table rows
      // Wait for table to be ready
      cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 }).should(
        "be.visible",
      );

      workplan
        .getWorkPlanResultsTable()
        .find("tbody tr", { timeout: 10000 })
        .should("have.length.greaterThan", 0)
        .then(($rows) => {
          // Check if any row contains the lab number
          const found = Array.from($rows).some((row) =>
            row.textContent.includes(options.labNo),
          );
          expect(
            found,
            `Expected to find lab number ${options.labNo} in workplan results`,
          ).to.be.true;
        });
    });
  });
});

describe("Work plan by Priority", function () {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/workplan**").as("getWorkplan");
    cy.intercept("POST", "**/rest/PrintWorkplanReport**").as("printWorkplan");
  });

  it("User can select work plan By Priority from main menu drop-down. Workplan By Priority page appears.", function () {
    workplan = homePage.goToWorkPlanPlanByPriority();

    // Verify we're on the workplan page
    cy.url().should("include", "/WorkplanByPriority");

    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.priorityTile);
    });
  });

  it("User should select Priority from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/workplan**").as("getWorkplan");

      // Wait for dropdown to be ready
      cy.get("select#select-1", { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled");

      workplan.selectDropdownOption(options.priority);

      // Wait for workplan API call after selection
      cy.wait("@getWorkplan", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      // Wait for table to populate (button only appears when testsList.length > 0)
      cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 })
        .should("be.visible")
        .find("tbody tr")
        .should("have.length.greaterThan", 0);

      // Now Print Workplan button should be visible
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      // Testing Roadmap: Use element readiness checks, wait for table rows
      // Wait for table to be ready
      cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 }).should(
        "be.visible",
      );

      workplan
        .getWorkPlanResultsTable()
        .find("tbody tr", { timeout: 10000 })
        .should("have.length.greaterThan", 0)
        .then(($rows) => {
          // Check if any row contains the lab number
          const found = Array.from($rows).some((row) =>
            row.textContent.includes(options.labNo),
          );
          expect(
            found,
            `Expected to find lab number ${options.labNo} in workplan results`,
          ).to.be.true;
        });
    });
  });
});
