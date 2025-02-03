import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let workplan = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Work plan by Test", function () {
  it("User  selects work plan by test from main drop-down menu.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByTest();
  });

  it("User selects test from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestType(options.testName);
    });
    cy.wait(5000);
    workplan.getPrintWorkPlanButton();
  });
  it("Check orders to remove and print", () => {
    workplan.checkToRemove();
    workplan.getFinalPrintWorkPlanButton();
  });
});

describe("Work plan by Panel", function () {
  it("User selects work plan by test from main menu drop-down.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPanel();
  });

  it("User selects panel from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestPanel(options.panelType);
    });
    cy.wait(5000);
    workplan.getPrintWorkPlanButton();
  });

  it("Check orders to remove and print", () => {
    workplan.checkToRemove();
    workplan.getFinalPrintWorkPlanButton();
  });
});

describe("Work plan by Unit", function () {
  it("User selects work plan By Unit from main menu drop-down.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByUnit();
  });

  it("User selects unit type from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestTypeUnit(options.unitType);
    });
    cy.wait(5000);
    workplan.getPrintWorkPlanButton();
  });

  it("Check orders to remove and print", () => {
    workplan.checkToRemove();
    workplan.getFinalPrintWorkPlanButton();
  });
});

describe("Work plan by Priority", function () {
  it("User selects work plan By Priority from main menu drop-down.", function () {
    homePage = loginPage.goToHomePage();
    workplan = homePage.goToWorkPlanPlanByPriority();
  });

  it("User selects Priority from drop-down selector option", () => {
    cy.fixture("workplan").then((options) => {
      workplan.getTestTypePriority(options.priority);
    });
    cy.wait(5000);
    workplan.getPrintWorkPlanButton();
  });

  it("Check orders to remove and print", () => {
    workplan.checkToRemove();
    workplan.getFinalPrintWorkPlanButton();
  });
});
