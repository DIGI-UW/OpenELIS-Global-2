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
  });

  it("User interacts with the pathology dashboard", function () {
    dashboard.checkFilters();
    dashboard.selectSecreening();
    dashboard.typeLabNo();
  });
});

describe("ImmunoChemistry Dashboard", function () {
  it("User  navigates to ImmunoChemistry Dashboard", function () {
    homePage = loginPage.goToHomePage();
    dashboard = homePage.goToImmunoChemistryDashboard();
  });

  it("User interacts with the immunochemistry dashboard", function () {
    dashboard.checkFilters();
    dashboard.selectReadyForPathology();
    dashboard.typeLabNo();
  });
});

describe("Cytology Dashboard", function () {
  it("User  navigates to Cytology Dashboard", function () {
    homePage = loginPage.goToHomePage();
    dashboard = homePage.goToCytologyDashboard();
  });

  it("User interacts with the cytology dashboard", function () {
    dashboard.checkFilters();
    dashboard.selectPreparingSlides();
    dashboard.typeLabNo();
  });
});
