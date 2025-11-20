import LoginPage from "../pages/LoginPage";
import OrderEntityPage from "../pages/OrderEntityPage";
import AdminPage from "../pages/AdminPage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let loginPage = null;
let workplan = null;
let orderEntityPage = new OrderEntityPage();
let patientEntryPage = new PatientEntryPage();
let adminPage = new AdminPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Work plan by Panel", function () {
  it("User can select work plan by test from main menu drop-down. Workplan by panel page appears.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPanel();
    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.panelTile);
    });
  });

  it("User should select panel from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.bilanPanelType);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      // Testing Roadmap: Use element readiness checks, wait for table rows
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
  it("User can select work plan By Unit from main menu drop-down. Workplan By Unit page appears.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByUnit();
    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.unitTile);
    });
  });

  it("User should select unit type from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.unitType);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      // Testing Roadmap: Use element readiness checks, wait for table rows
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
  it("User can select work plan By Priority from main menu drop-down. Workplan By Priority page appears.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPriority();
    cy.fixture("workplan").then((options) => {
      workplan.getWorkPlanFilterTitle(options.priorityTile);
    });
  });

  it("User should select Priority from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.selectDropdownOption(options.priority);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("All known orders are present", () => {
    cy.fixture("Order").then((options) => {
      // Testing Roadmap: Use element readiness checks, wait for table rows
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
