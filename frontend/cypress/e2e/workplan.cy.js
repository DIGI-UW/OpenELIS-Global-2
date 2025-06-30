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
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
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
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
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
      workplan
        .getWorkPlanResultsTable()
        .find("tr")
        .then((row) => {
          expect(row.text()).contains(options.labNo);
        });
    });
  });
});
