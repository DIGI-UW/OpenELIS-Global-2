import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let reflexTestsConfigPage = null;

const navigateToReflexTestsManagement = () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPageProgram();
  reflexTestsConfigPage = adminPage.goToReflexTestsManagement();
};

before(() => {
  navigateToReflexTestsManagement();
});

describe("Reflex Tests Management", () => {
  it("Add Reflex Rule Conditions", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.verifyPageLoads(test.reflexTets);
      reflexTestsConfigPage.validateToggleStatus(test.toggleOn);
      reflexTestsConfigPage.enterRuleName(test.ruleName);
      reflexTestsConfigPage.selectOverAllOptions(test.overAllOptions);
      reflexTestsConfigPage.selectSample(test.sample);
      reflexTestsConfigPage.searchTest(test.searchTest);
      reflexTestsConfigPage.selectRelation(test.relation);
      reflexTestsConfigPage.enterNumericValue(test.numericValue);
    });
  });

  it("Perform the following actions", () => {
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.selectSecondSample(test.sample);
      reflexTestsConfigPage.searchReflexTest(test.searchTest);
      reflexTestsConfigPage.addInternatNote(test.internalNote);
      reflexTestsConfigPage.addExternatNote(test.externalNote);
      reflexTestsConfigPage.submitButton();
    });
  });

  it("Validate Added Rule", () => {
    reflexTestsConfigPage.reloadAndWait();
    cy.fixture("ReflexTestsConfig").then((test) => {
      reflexTestsConfigPage.validateToggleStatus(test.toggleOff);
      reflexTestsConfigPage.validateRuleName(test.ruleName);
    });
  });
});
