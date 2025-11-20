/**
 * E2E Tests for Batch Order Entry
 * Tests batch order entry workflows for On Demand/Serum and Pre Printed/EID form types
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/batchOrderEntry.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let batchOrder = null;
let adminPage = new AdminPage();

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

before(() => {
  cy.fixture("BatchOrder").as("batchOrderData");
});

const navigateToBatchOrderEntryPage = () => {
  batchOrder = homePage.goToBatchOrderEntry();
  // Verify we're on the batch order entry page
  cy.url().should("include", "/BatchOrderEntry");
};

describe("Batch Order Entry On Demand and Serum form type", function () {
  before("navigate to Batch Order Entry Page", function () {
    navigateToBatchOrderEntryPage();
  });

  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("saveOrder");
    cy.intercept("GET", "**/rest/patient**").as("getPatient");
  });

  it("User visits Batch Order Entry Setup Page", function () {
    batchOrder.visitSetupPage();
    batchOrder.checkNextButtonDisabled();
  });

  it("User selects Routine Form and Serum Sample", function () {
    const data = this.batchOrderData;

    // Wait for form dropdown to be ready
    cy.get("select", { timeout: 10000 }).should("be.visible");

    batchOrder.selectForm(data.formTypeRoutine);
    batchOrder.selectSampleType(data.serumSample);
  });

  it("User checks Panels and Tests", function () {
    batchOrder.checkBilanPanel();
    batchOrder.checkSerologiePanel();
    //tests picked at random
    batchOrder.checkDenguePCR();
    batchOrder.checkHIVViralLoad();
    batchOrder.checkCreatinine();
  });

  it("Should Select Methods, Site Name and Move to Next Page", function () {
    const data = this.batchOrderData;

    // Wait for method dropdown to be ready
    cy.get("select", { timeout: 10000 }).should("be.visible");

    batchOrder.selectMethod(data.methodOnDemand);
    batchOrder.checkFacilityCheckbox();
    batchOrder.checkPatientCheckbox();
    batchOrder.enterSiteName(data.siteName);
    batchOrder.checkNextButtonEnabled();
  });

  it("User adds New Patient", function () {
    batchOrder.clickNewPatientButton();

    // Wait for patient form to be ready
    cy.get("input", { timeout: 10000 }).should("be.visible");

    const data = this.batchOrderData;
    batchOrder.uniqueHealthIDNum(data.healthID);
    batchOrder.nationalID(data.nationalID);
    batchOrder.firstName(data.firstName);
    batchOrder.lastName(data.lastName);
    batchOrder.typePatientYears(data.years);
    batchOrder.typePatientMonths(data.months);
    batchOrder.typePatientDays(data.days);
    batchOrder.selectGender(); //female in this case
  });
  //Save button is lacking and needs to be added for this test to work
  //it("User should click save new patient information button", function () {
  // batchOrder.clickSavePatientButton();
  //});

  it("Generate BarCode", function () {
    const data = this.batchOrderData;

    // Wait for lab number input to be ready
    cy.get("input", { timeout: 10000 }).should("be.visible");

    batchOrder.typeLabNumber(data.labNumber);

    // Set up intercept BEFORE action
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("saveOrder");

    batchOrder.clickGenerateAndSaveBarcode();

    // Wait for API call instead of arbitrary wait
    cy.wait("@saveOrder", { timeout: 15000 })
      .its("response.statusCode")
      .should("be.oneOf", [200, 201]);

    batchOrder.checkNextLabel().should("be.visible");
  });

  it("User clicks the finish button", function () {
    // Verify finish button is ready before clicking
    cy.get("button", { timeout: 10000 })
      .contains("Finish")
      .should("be.visible")
      .should("not.be.disabled");

    batchOrder.clickFinishButton();
  });
});

describe("Batch Order Entry Pre Printed and EID form type", function () {
  before("navigate to Batch Order Entry Page", function () {
    navigateToBatchOrderEntryPage();
  });

  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/patient-search-results*").as(
      "getPatientSearch",
    );
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("saveOrder");
  });

  it("User visits Batch Order Entry Setup Page", function () {
    batchOrder.visitSetupPage();
    batchOrder.checkNextButtonDisabled();
  });

  it("User selects EID form, samples and test", function () {
    const data = this.batchOrderData;

    // Wait for form dropdown to be ready
    cy.get("select", { timeout: 10000 }).should("be.visible");

    batchOrder.selectForm(data.formTypeEID);
    batchOrder.selectDNAPCRTest();
    batchOrder.selectTubeSample();
    batchOrder.selectBloodSample();
  });

  it("User Selects Methods, Site Name and Move to Next Page", function () {
    const data = this.batchOrderData;

    // Wait for method dropdown to be ready
    cy.get("select", { timeout: 10000 }).should("be.visible");

    batchOrder.selectMethod(data.methodPrePrinted);
    batchOrder.checkFacilityCheckbox();
    batchOrder.checkPatientCheckbox();
    batchOrder.enterSiteName(data.siteName);
    batchOrder.checkNextButtonEnabled();
  });

  it("User Searches for Existing Patient", function () {
    batchOrder.clickSearchPatientButton();

    // Wait for search form to be ready
    cy.get("input", { timeout: 10000 }).should("be.visible");

    const data = this.batchOrderData;
    batchOrder.lastName(data.lastName);
    batchOrder.firstName(data.firstName);

    // Set up intercept BEFORE action
    cy.intercept("GET", "**/rest/patient-search-results*").as(
      "getPatientSearch",
    );

    batchOrder.localSearchButton();

    // Wait for API call instead of arbitrary wait
    cy.wait("@getPatientSearch", { timeout: 15000 })
      .its("response.statusCode")
      .should("eq", 200);

    // Wait for search results to appear
    cy.get("tbody tr", { timeout: 10000 }).should("have.length.greaterThan", 0);

    batchOrder.checkPatientRadio(); //the first on the list
  });

  it("Should Visit Batch Order Entry Page", function () {
    batchOrder.visitBatchOrderEntryPage();
  });

  it(" User enters Lab Number and Generates Barcode", function () {
    const data = this.batchOrderData;

    // Wait for lab number input to be ready
    cy.get("input", { timeout: 10000 }).should("be.visible");

    batchOrder.typeLabNumber(data.labNumber);
    batchOrder.visitBatchOrderEntryPage();

    // Set up intercept BEFORE action
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("saveOrder");

    batchOrder.clickGenerateButton();

    // Wait for API call instead of arbitrary wait
    cy.wait("@saveOrder", { timeout: 15000 })
      .its("response.statusCode")
      .should("be.oneOf", [200, 201]);

    batchOrder.saveOrder();
  });

  it("User clicks the finish button", function () {
    // Verify finish button is ready before clicking
    cy.get("button", { timeout: 10000 })
      .contains("Finish")
      .should("be.visible")
      .should("not.be.disabled");

    batchOrder.clickFinishButton();
  });
});
