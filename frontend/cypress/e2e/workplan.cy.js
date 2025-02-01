import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let workplan = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Work plan by Test", function () {
  it("User  selects work plan by test from main menu drop-down.And the page appears", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByTest();
  });

  it("User should select test from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestType(options.testName);
      cy.wait(10000);
      workplan.getPrintWorkPlanButton();
    });
  });
  it("Check orders to remove and print", () => {
    workplan.checkAndPrint();
    workplan.getPrintWorkPlanButton();
  });
});

describe("Work plan by Panel", function () {
  it("User can select work plan by test from main menu drop-down. Workplan by panel page appears.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPanel();
  });

  it("User should select panel from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestPanel(options.panelType);
      cy.wait(5000);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("Check orders to remove and print", () => {
    workplan.checkAndPrint();
    workplan.getPrintWorkPlanButton();
  });
});

describe("Work plan by Unit", function () {
  it("User can select work plan By Unit from main menu drop-down. Workplan By Unit page appears.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByUnit();
  });

  it("User should select unit type from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestTypeUnit(options.unitType);
      cy.wait(5000);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("Check orders to remove and print", () => {
    workplan.checkAndPrint();
    workplan.getPrintWorkPlanButton().click();
  });
});

describe("Work plan by Priority", function () {
  it("User can select work plan By Priority from main menu drop-down. Workplan By Priority page appears.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPriority();
  });

  it("User should select Priority from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestTypePriority(options.priority);
      cy.wait(5000);
      workplan.getPrintWorkPlanButton();
    });
  });

  it("Check orders to remove and print", () => {
    workplan.checkAndPrint();
    workplan.getPrintWorkPlanButton().click();
  });
});
