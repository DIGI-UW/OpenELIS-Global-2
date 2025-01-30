import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let dashboard = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

describe("Pathology Dashboard", function () {
  it("User  Visits Pathology Dashboard", function () {
    homePage = loginPage.goToHomePage();
    dashboard = homePage.goToPathologyDashboard();

    dashboard.checkForHeader("Pathology");
  });

  it("User adds a new Pathology order", function () {
    homePage.goToOrderPage();
    dashboard.addOrder("Histopathology");
  });
  it("Check For Order", () => {
    homePage.goToPathologyDashboard();
    dashboard.checkForHeader("Pathology");

    cy.fixture("DashBoard").then((order) => {
      dashboard.validatePreStatus(order.labNo);
    });
  });

  it("Change The Status of Order and save it", () => {
    dashboard.changeStatus("Completed");
    dashboard.enterDetails();
    dashboard.saveOrder();
  });

  it("Validate the Status of Order", () => {
    cy.fixture("DashBoard").then((order) => {
      //  dashboard.validateOrderStatus(order.labNo, 4);
    });
  });
});

describe("ImmunoChemistry Dashboard", function () {
  it("User  Visits ImmunoChemistry Dashboard", function () {
    homePage = loginPage.goToHomePage();
    dashboard = homePage.goToImmunoChemistryDashboard();
    dashboard.checkForHeader("Immunohistochemistry");

    //  cy.fixture("DashBoard").then((order) => {
    //     dashboard.validatePreStatus(order.labNo);

    //     });
  });

  it("User adds a new ImmunioChemistry order", function () {
    homePage.goToOrderPage();
    dashboard.addOrder("Immunohistochemistry");
  });

  it("Check For Order", () => {
    homePage.goToImmunoChemistryDashboard();

    dashboard.checkForHeader("Immunohistochemistry");

    cy.fixture("DashBoard").then((order) => {
      dashboard.validatePreStatus(order.labNo);
    });
  });

  it("Change The Status of Order and save it", () => {
    dashboard.changeStatus("Completed");
    dashboard.selectPathologist("ELIS,Open");
    dashboard.saveOrder();
  });

  it("Validate the Status of Order", () => {
    cy.fixture("DashBoard").then((order) => {
      //TO DO : needs to be properly re-writen with proper selector
      //dashboard.validateOrderStatus(order.labNo, 3);
    });
  });
});
