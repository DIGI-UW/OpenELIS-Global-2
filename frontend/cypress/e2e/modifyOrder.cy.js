/**
 * E2E Tests for Modify Order
 * Tests order modification workflow including patient search and order updates
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/modifyOrder.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";
import OrderEntityPage from "../pages/OrderEntityPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let adminPage = new AdminPage();
let modifyOrderPage = null;
let orderEntityPage = new OrderEntityPage();
let patientPage = new PatientEntryPage();

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Add New Patient", function () {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("POST", "**/rest/PatientManagement**").as("createPatient");
  });

  it("User Visits Home Page and goes to Add Add|Modify Patient Page", () => {
    patientPage = homePage.goToPatientEntry();
    // Verify we're on the patient entry page
    cy.url().should("include", "/PatientManagement");
  });

  it("Navigate to create Patient tab", function () {
    patientPage.clickNewPatientTab();
    patientPage.getSubmitButton().should("be.visible");
  });

  it("Enter patient Information and clear", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.enterPatientInfo(
        patient.firstName,
        patient.lastName,
        patient.subjectNumber,
        patient.nationalId,
        patient.DOB,
      );
    });
  });

  it("Clear new patient information", function () {
    patientPage.clearPatientInfo();
  });

  it("Enter patient Information and save", function () {
    cy.fixture("Patient").then((patient) => {
      patientPage.enterPatientInfo(
        patient.firstName,
        patient.lastName,
        patient.subjectNumber,
        patient.nationalId,
        patient.DOB,
      );
    });
  });

  it("Save new patient information button", function () {
    // Set up intercept BEFORE action
    cy.intercept("POST", "**/rest/PatientManagement**").as("createPatient");

    patientPage.clickSavePatientButton();

    // Wait for API call instead of arbitrary wait
    cy.wait("@createPatient", { timeout: 15000 })
      .its("response.statusCode")
      .should("eq", 200);

    // Verify success message appears (use .should() for retry-ability)
    cy.get("div[role='status']", { timeout: 10000 })
      .should("be.visible")
      .should("contain.text", "success");
  });
});

describe("Modify Order search by patient ", function () {
  beforeEach(() => {
    // Navigate to modify order page for each test
    cy.visit("/FindOrder");

    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/patient-search-results*").as(
      "getPatientSearch",
    );
    cy.intercept("GET", "**/rest/user-programs**").as("getPrograms");
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("submitOrder");
  });

  it("User Visits Home Page and goes to Modify Order Page ", function () {
    modifyOrderPage = homePage.goToModifyOrderPage();
    // Verify we're on the modify order page
    cy.url().should("include", "/FindOrder");
  });

  it("Should search Patient By First and LastName", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/patient-search-results*").as(
        "getPatientSearch",
      );

      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.getLastName().should("have.value", patient.lastName);
      patientPage.getLastName().should("not.have.value", patient.inValidName);

      // Verify button is ready before clicking
      cy.get("[data-cy='searchPatientButton']")
        .should("be.visible")
        .should("not.be.disabled");

      modifyOrderPage.clickSearchPatientButton();

      // Wait for API call instead of arbitrary wait
      cy.wait("@getPatientSearch", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });

  it("Should be able to search patients By gender", function () {
    // Set up intercept BEFORE action
    cy.intercept("GET", "**/rest/patient-search-results*").as(
      "getPatientSearch",
    );

    patientPage.getMaleGenderRadioButton().should("be.visible").click();

    // Verify button is ready before clicking
    cy.get("[data-cy='searchPatientButton']")
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.clickSearchPatientButton();

    // Wait for API call instead of arbitrary wait
    cy.wait("@getPatientSearch", { timeout: 15000 })
      .its("response.statusCode")
      .should("eq", 200);

    cy.fixture("Patient").then((patient) => {
      patientPage.validatePatientByGender("M");
    });
  });

  it("should search patient By PatientId", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/patient-search-results*").as(
        "getPatientSearch",
      );

      patientPage.searchPatientByPatientId(patient.nationalId);

      // Verify button is ready before clicking
      cy.get("[data-cy='searchPatientButton']")
        .should("be.visible")
        .should("not.be.disabled");

      modifyOrderPage.clickSearchPatientButton();

      // Wait for API call instead of arbitrary wait
      cy.wait("@getPatientSearch", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });

  it("Should be able to search by respective patient ", function () {
    // Wait for table to be ready (use .should() for retry-ability)
    cy.get("table", { timeout: 10000 })
      .should("be.visible")
      .find("tbody tr")
      .should("have.length.greaterThan", 0);

    modifyOrderPage.clickRespectivePatient();
  });

  it("Validate program dropdown button not visible and click next", function () {
    // Wait for program dropdown to be ready
    cy.get("#additionalQuestionsSelect", { timeout: 10000 })
      .should("be.visible")
      .should("be.disabled");

    modifyOrderPage.checkProgramButton();

    // Verify next button is ready before clicking
    cy.get("[data-cy='next-button']", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.clickNextButton();
  });

  it("should be able to record", function () {
    // Wait for table to be ready and have rows (use .should() for retry-ability)
    cy.get("table", { timeout: 10000 })
      .should("be.visible")
      .find("tbody")
      .should("exist");

    // Wait for table rows to appear
    cy.get("table tbody tr", { timeout: 10000 }).should(
      "have.length.greaterThan",
      0,
    );

    // Wait for checkboxes to be ready
    cy.get('table input[type="checkbox"][name="add"]', { timeout: 10000 })
      .first()
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.assignValues();
  });

  it("Click next, go add order page then submit order", function () {
    // Verify next button is ready before clicking
    cy.get("[data-cy='next-button']", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.clickNextButton();

    // Wait for form to be ready
    cy.get("#siteName", { timeout: 10000 }).should("be.visible");

    orderEntityPage.rememberSiteAndRequester();

    // Set up intercept BEFORE action
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("submitOrder");

    // Verify submit button is ready before clicking
    cy.get("[data-cy='submit-order']", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.clickSubmitButton();

    // Wait for order submission API call
    cy.wait("@submitOrder", { timeout: 15000 })
      .its("response.statusCode")
      .should("be.oneOf", [200, 201]);
  });
});

describe("Modify Order search by accession Number", function () {
  beforeEach(() => {
    // Navigate to modify order page for each test
    cy.visit("/FindOrder");

    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/order**").as("getOrderByAccession");
    cy.intercept("GET", "**/rest/user-programs**").as("getPrograms");
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("submitOrder");
  });

  it("User Visits Home Page and goes to Modify Order Page ", function () {
    modifyOrderPage = homePage.goToModifyOrderPage();
    // Verify we're on the modify order page
    cy.url().should("include", "/FindOrder");
  });

  it("Searche with accession number and submit", () => {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/order**").as("getOrderByAccession");

      modifyOrderPage.enterAccessionNo(patient.labNo);

      // Verify button is ready before clicking
      cy.get("[data-cy='submit-button']", { timeout: 10000 })
        .should("be.visible")
        .should("not.be.disabled");

      modifyOrderPage.clickSubmitAccessionButton();

      // Wait for API call instead of arbitrary wait
      cy.wait("@getOrderByAccession", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      // Wait for form to load with patient data (indicated by form fields being populated)
      cy.get("input, select", { timeout: 15000 })
        .should("be.visible")
        .first()
        .should("not.be.empty");
    });
  });

  it("Validate program dropdown button not visible and click next", function () {
    // Wait for program dropdown to be ready
    cy.get("#additionalQuestionsSelect", { timeout: 10000 })
      .should("be.visible")
      .should("be.disabled");

    modifyOrderPage.checkProgramButton();

    // Verify next button is ready before clicking
    cy.get("[data-cy='next-button']", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.clickNextButton();
  });

  it("Add Sample", function () {
    // Wait for sample form to be ready
    cy.get("#sampleId_0", { timeout: 10000 }).should("be.visible");

    modifyOrderPage.selectSerumSample();
    orderEntityPage.checkPanelCheckBoxField();

    // Verify next button is ready before clicking
    cy.get("[data-cy='next-button']", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.clickNextButton();
  });

  it("Add Order", function () {
    // Wait for form to be ready
    cy.get("#siteName", { timeout: 10000 }).should("be.visible");

    orderEntityPage.generateLabOrderNumber();

    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
      orderEntityPage.enterRequesterLastAndFirstName(
        order.requester.fullName,
        order.requester.firstName,
        order.requester.lastName,
      );
    });

    orderEntityPage.rememberSiteAndRequester();

    // Set up intercept BEFORE action
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("submitOrder");

    // Verify submit button is ready before clicking
    cy.get("[data-cy='submit-order']", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");

    modifyOrderPage.clickSubmitButton();

    // Wait for order submission API call
    cy.wait("@submitOrder", { timeout: 15000 })
      .its("response.statusCode")
      .should("be.oneOf", [200, 201]);
  });
});
