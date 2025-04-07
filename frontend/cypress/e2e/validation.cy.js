import LoginPage from "../pages/LoginPage";
import HomePage from "../pages/HomePage";
import PatientEntryPage from "../pages/PatientEntryPage";
import Validation from "../pages/Validation";

describe("Validation Tests", () => {
  let homePage = null;
  let loginPage = null;
  let validation = null;
  let patientPage = null;

  beforeEach(() => {
    // Setup before each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    patientPage = new PatientEntryPage();
  });

  const navigateToValidationPage = (validationType) => {
    validation = homePage[`goToValidationBy${validationType}`]();
  };

  describe("Validation By Routine", () => {
    beforeEach(() => {
      navigateToValidationPage("Routine");
    });

    it("should display validation page with correct heading", () => {
      validation.checkForHeading();
    });

    it("should select test unit from dropdown and validate", () => {
      cy.fixture("workplan").then((order) => {
        validation.selectTestUnit(order.unitType);
        // Uncomment when method is implemented
        // validation.validateTestUnit(order.testName);
      });
    });
  });

  describe("Validation By Order", () => {
    beforeEach(() => {
      navigateToValidationPage("Order");
    });

    it("should display validation page with correct heading", () => {
      validation.checkForHeading();
    });

    it("should enter lab number, make a search and validate", () => {
      cy.fixture("Patient").then((order) => {
        validation.enterLabNumberAndSearch(order.labNo);
      });
    });
  });

  describe("Validation By Range Of Order", () => {
    beforeEach(() => {
      navigateToValidationPage("RangeOrder");
    });

    it("should display validation page with correct heading", () => {
      validation.checkForHeading();
    });

    it("should enter lab number and perform a search", () => {
      cy.fixture("Patient").then((order) => {
        validation.enterLabNumberAndSearch(order.labNo);
      });
    });

    it("should save the results", () => {
      cy.fixture("Patient").then((order) => {
        validation.enterLabNumberAndSearch(order.labNo);
      });
      validation.saveResults("Test Note");
    });
  });
});
