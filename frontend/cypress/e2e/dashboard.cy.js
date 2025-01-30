import LoginPage from "../pages/LoginPage";
import DashBoardPage from "../pages/DashBoard";

let homePage = null;
let loginPage = null;
let dashboard = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  dashboard = new DashBoardPage();
  homePage = loginPage.goToHomePage();

  cy.fixture("Order").as("dashBData");
});

describe("Dashboard for the Home Page", function () {
  it("User clicks search bar", function () {
    dashboard.homeSearchBar();
  });

  it("User types the patient to search and closes it", function () {
    cy.fixture("Order").then((dashBData) => {
      dashboard.searchBarInput(dashBData.searchName);
    });
    dashboard.homeSearchBar();
  });

  it("User interacts with the notifications icon", function () {
    dashboard.notificationIcon();
    dashboard.notificationIconClose();
  });

  it("User interacts with the user icon", function () {
    dashboard.userIcon();
    cy.fixture("Order").then((dashBData) => {
      dashboard.userSelectsEng(dashBData.engLang);
    });
    dashboard.userIcon();
  });
});

describe("Pathology Dashboard", function () {
  it("User  Visits the Dashboard", function () {
    dashboard = homePage.goToPathology();
  });

  it("User checks filters, selects cases and items per page", function () {
    dashboard.checkFilters();
    cy.fixture("Order").then((dashBData) => {
      dashboard.selectCases(dashBData.myCases);
      dashboard.enterLabNumber(dashBData.labNumb);
      dashboard.pageItems(dashBData.itemsPerPage);
    });
  });
});

describe("Immunohistochemistry Dashboard", function () {
  it("User  Visits the Dashboard", function () {
    dashboard = homePage.goToImmunoChemistryDashboard();
  });

  it("User checks filters, selects cases and items per page", function () {
    dashboard.checkFilters();
    cy.fixture("Order").then((dashBData) => {
      dashboard.selectCompletedCases(dashBData.myCases2);
      dashboard.enterLabNumber(dashBData.labNumb);
      dashboard.pageItems(dashBData.itemsPerPage);
    });
  });
});

describe("Cytology Dashboard", function () {
  it("User  Visits the Dashboard", function () {
    dashboard = homePage.goToCytology();
  });

  it("User checks filters, selects cases and items per page", function () {
    dashboard.checkFilters();
    cy.fixture("Order").then((dashBData) => {
      dashboard.selectScreeningCases(dashBData.myCases3);
      dashboard.enterLabNumber(dashBData.labNumb);
      dashboard.pageItems(dashBData.itemsPerPage);
    });
  });
});

describe("Back to home page", function () {
  it("User navigates back to home page", function () {
    dashboard = homePage.backToHomePage();
  });
});
