import LoginPage from "../pages/LoginPage";
import HomePage from "../pages/HomePage";
import PatientEntryPage from "../pages/PatientEntryPage";
import Validation from "../pages/Validation";

let homePage = null;
let loginPage = null;
let validation = null;
let patientPage = new PatientEntryPage();

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

const navigateToValidationPage = (validationType) => {
  homePage = loginPage.goToHomePage();
  validation = homePage[`goToValidationBy${validationType}`]();
};

describe("Validation By Routine", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Routine");
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Should Select Test Unit From Drop-Down And Validate", function () {
    cy.fixture("workplan").then((order) => {
      validation.selectTestUnit(order.unitType);
      //validation.validateTestUnit(order.testName);
    });
  });
});

describe("Validation By Order", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Order");
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Enter Lab Number, search and validate", function () {
    cy.fixture("Patient").then((order) => {
      validation.enterLabNumberAndSearch(order.labNo);
    });
  });
});

describe("Validation By Range Of Order", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("RangeOrder");
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Should Enter Lab Number and perform a search", function () {
    cy.fixture("Patient").then((order) => {
      validation.enterLabNumberAndSearch(order.labNo);
    });
  });

  it("Should Save the results", function () {
    validation.saveResults("Test Note");
  });
});
