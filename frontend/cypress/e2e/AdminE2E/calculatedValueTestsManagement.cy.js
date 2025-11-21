import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

let homePage = null;
let adminPage = null;
let reflexTestsConfigPage = null;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
before(() => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPageProgram();
  reflexTestsConfigPage = adminPage.goToCalculatedValueTestsManagement();
});

describe("Calculated Value Tests Management", () => {
  it("Add Test Result", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      // Wait for page to load and data to be fetched
      cy.intercept("GET", "**/rest/test-calculations").as("loadCalculations");
      cy.intercept("GET", "**/rest/math-functions").as("loadMathFunctions");
      cy.intercept("GET", "**/rest/samples").as("loadSamples");
      reflexTestsConfigPage.verifyPageLoads(test.calcValue);
      // Wait for all data to load before interacting
      cy.wait(["@loadCalculations", "@loadMathFunctions", "@loadSamples"]);
      // Wait for form to be ready (name input visible means form is loaded)
      cy.get('[id="0_name"]').should("be.visible");
      // Toggle might be "On" or "Off" depending on loaded data - check actual state
      // For new forms it should be "On", but if existing calculations load it might differ
      cy.get(".cds--toggle__text").should("be.visible");
      reflexTestsConfigPage.enterCalcName(test.ruleName);
      reflexTestsConfigPage.verifyRemoveOperationButton();
      // Wait for sample dropdown to be ready
      cy.get("[data-cy='add-sample']").should("be.visible");
      reflexTestsConfigPage.selectFourthSample(test.sample);
      // Wait for test input to be ready before searching
      cy.get('[id="0_0_testresult"]').should("be.visible");
      reflexTestsConfigPage.searchNumTest(test.searchTest);
    });
  });

  it("Add Mathematical Function", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      // Button appears after adding test result - wait for it to be ready
      cy.get('[id="0_mathfunction"]').should("be.visible");
      reflexTestsConfigPage.clickMathFunctionButton();
      // Wait for math function dropdown to appear
      cy.get('[id="0_1_mathfunction"]').should("be.visible");
      reflexTestsConfigPage.mathFunction(test.mtcFunction);
    });
  });

  it("Add Patient Attribute", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      // Button appears after previous operations
      cy.get('[id="0_patientattribute"]').should("be.visible");
      reflexTestsConfigPage.clickPatientAttributeButton();
      // Wait for patient attribute dropdown
      cy.get('[id="0_2_patientattribute"]').should("be.visible");
      reflexTestsConfigPage.selectPatientAttribute(test.patientAttribute);
    });
  });

  it("Add Mathematical Function Option", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      // Wait for operation dropdown to be ready
      cy.get('[id="0_2_addoperation"]').should("be.visible");
      reflexTestsConfigPage.selectMathFunction(test.mathFunction);
      // Wait for second math function dropdown
      cy.get('[id="0_3_mathfunction"]').should("be.visible");
      reflexTestsConfigPage.secMathFunction(test.secMtcFunction);
    });
  });

  it("Add Integer", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      // Button appears after previous operations
      cy.get('[id="0_integer"]').should("be.visible");
      reflexTestsConfigPage.clickIntegerButton();
      // Wait for integer input to appear
      cy.get('[id="0_4_integer"]').should("be.visible");
      reflexTestsConfigPage.enterInteger(test.numericValue);
    });
  });

  it("Enter Final Result and Submit", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      // Wait for sample dropdown to be ready
      cy.get('[id="0_sample"]').should("be.visible");
      reflexTestsConfigPage.selectThirdSample(test.sample);
      // Wait for final result input
      cy.get('[id="0_finalresult"]').should("be.visible");
      reflexTestsConfigPage.enterFinalResult(test.searchTest);
      reflexTestsConfigPage.addFinalExternatNote(test.externalNote);
      reflexTestsConfigPage.submitButton();
    });
  });

  it("Validate Added Rule", () => {
    reflexTestsConfigPage.reloadAndWait();
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.validateToggleStatus(test.toggleOff);
      reflexTestsConfigPage.validateCalcName(test.ruleName);
    });
  });
});
