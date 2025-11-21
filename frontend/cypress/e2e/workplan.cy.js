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
    // Actual API endpoint is /rest/WorkPlanByPanel?panel_id= (not /rest/workplan)
    cy.intercept("GET", "**/rest/WorkPlanByPanel**").as("getWorkplan");
    cy.intercept("POST", "**/rest/PrintWorkplanReport**").as("printWorkplan");
  });

  it("User can select work plan by test from main menu drop-down. Workplan by panel page appears.", function () {
    workplan = homePage.goToWorkPlanPlanByPanel();

    // Verify we're on the workplan page (URL may have capital P: /WorkPlanByPanel)
    cy.url().should("satisfy", (url) => {
      return url.toLowerCase().includes("workplanbypanel");
    });

    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.panelTile);
    });
  });

  it("User should select panel from drop-down selector option and verify orders are present", () => {
    cy.fixture("workplan").then((workplanOptions) => {
      cy.fixture("Order").then((orderOptions) => {
        // Set up intercept BEFORE action (actual endpoint is /rest/WorkPlanByPanel)
        cy.intercept("GET", "**/rest/WorkPlanByPanel**").as("getWorkplan");

        // Wait for dropdown to be ready
        cy.get("select#select-1", { timeout: 10000 })
          .should("be.visible")
          .should("not.be.disabled");

        workplan.selectDropdownOption(workplanOptions.bilanPanelType);

        // Wait for workplan API call after selection
        cy.wait("@getWorkplan", { timeout: 15000 })
          .its("response.statusCode")
          .should("eq", 200);

        // Wait for table to populate (button only appears when testsList.length > 0)
        cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 })
          .should("be.visible")
          .find("tbody tr")
          .should("have.length.greaterThan", 0)
          .then(($rows) => {
            // Verify expected order is present
            const found = Array.from($rows).some((row) =>
              row.textContent.includes(orderOptions.labNo),
            );
            expect(
              found,
              `Expected to find lab number ${orderOptions.labNo} in workplan results`,
            ).to.be.true;
          });

        // Now Print Workplan button should be visible (button id="print", text="Print Workplan")
        workplan.getPrintWorkPlanButton();
      });
    });
  });
});

describe("Work plan by Unit", function () {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    // Actual API endpoint is /rest/WorkPlanByTestSection?test_section_id= (for unit)
    cy.intercept("GET", "**/rest/WorkPlanByTestSection**").as("getWorkplan");
    cy.intercept("POST", "**/rest/PrintWorkplanReport**").as("printWorkplan");
  });

  it("User can select work plan By Unit from main menu drop-down. Workplan By Unit page appears.", function () {
    workplan = homePage.goToWorkPlanPlanByUnit();

    // Verify we're on the workplan page (actual route is /WorkPlanByTestSection for unit)
    cy.url().should("include", "WorkPlanByTestSection");

    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.unitTile);
    });
  });

  it("User should select unit type from drop-down selector option and verify orders are present", () => {
    cy.fixture("workplan").then((workplanOptions) => {
      cy.fixture("Order").then((orderOptions) => {
        // Ensure we're on the workplan page (test isolation means we need to navigate)
        workplan = homePage.goToWorkPlanPlanByUnit();
        cy.url().should("include", "WorkPlanByTestSection");

        // Set up intercept BEFORE action (actual endpoint is /rest/WorkPlanByTestSection)
        cy.intercept("GET", "**/rest/WorkPlanByTestSection**").as(
          "getWorkplan",
        );

        // Wait for dropdown to be ready
        cy.get("select#select-1", { timeout: 10000 })
          .should("be.visible")
          .should("not.be.disabled");

        workplan.selectDropdownOption(workplanOptions.unitType);

        // Wait for workplan API call after selection
        cy.wait("@getWorkplan", { timeout: 15000 })
          .its("response.statusCode")
          .should("eq", 200);

        // Wait for table to populate and verify expected order is present
        cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 })
          .should("be.visible")
          .find("tbody tr")
          .should("have.length.greaterThan", 0)
          .then(($rows) => {
            // Verify expected order is present
            const found = Array.from($rows).some((row) =>
              row.textContent.includes(orderOptions.labNo),
            );
            expect(
              found,
              `Expected to find lab number ${orderOptions.labNo} in workplan results`,
            ).to.be.true;
          });

        // Now Print Workplan button should be visible
        workplan.getPrintWorkPlanButton();
      });
    });
  });
});

describe("Work plan by Priority", function () {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    // Actual API endpoint is /rest/WorkPlanByPriority?priority=
    cy.intercept("GET", "**/rest/WorkPlanByPriority**").as("getWorkplan");
    cy.intercept("POST", "**/rest/PrintWorkplanReport**").as("printWorkplan");
  });

  it("User can select work plan By Priority from main menu drop-down. Workplan By Priority page appears.", function () {
    workplan = homePage.goToWorkPlanPlanByPriority();

    // Verify we're on the workplan page (URL may have capital P: /WorkPlanByPriority)
    cy.url().should("satisfy", (url) => {
      return url.toLowerCase().includes("workplanbypriority");
    });

    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.priorityTile);
    });
  });

  it("User should select Priority from drop-down selector option and verify orders are present", () => {
    cy.fixture("workplan").then((workplanOptions) => {
      cy.fixture("Order").then((orderOptions) => {
        // Set up intercept BEFORE action (actual endpoint is /rest/WorkPlanByPriority)
        cy.intercept("GET", "**/rest/WorkPlanByPriority**").as("getWorkplan");

        // Wait for dropdown to be ready
        cy.get("select#select-1", { timeout: 10000 })
          .should("be.visible")
          .should("not.be.disabled");

        workplan.selectDropdownOption(workplanOptions.priority);

        // Wait for workplan API call after selection
        cy.wait("@getWorkplan", { timeout: 15000 })
          .its("response.statusCode")
          .should("eq", 200);

        // Wait for table to populate and verify expected order is present
        cy.get('[data-cy="workplanResultsTable"]', { timeout: 10000 })
          .should("be.visible")
          .find("tbody tr")
          .should("have.length.greaterThan", 0)
          .then(($rows) => {
            // Verify expected order is present
            const found = Array.from($rows).some((row) =>
              row.textContent.includes(orderOptions.labNo),
            );
            expect(
              found,
              `Expected to find lab number ${orderOptions.labNo} in workplan results`,
            ).to.be.true;
          });

        // Now Print Workplan button should be visible
        workplan.getPrintWorkPlanButton();
      });
    });
  });
});
