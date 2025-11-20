/**
 * E2E Tests for Order Entity
 * Tests order creation workflow from patient search to submission
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/orderEntity.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import AdminPage from "../pages/AdminPage";

let homePage = null;
let adminPage = new AdminPage();
let orderEntityPage = null;
let patientEntryPage = null;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Order Entity", function () {
  beforeEach(() => {
    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("GET", "**/rest/patient-search-results*").as("getPatientSearch");
    cy.intercept("GET", "**/rest/user-programs**").as("getPrograms");
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("submitOrder");
    cy.intercept("GET", "**/rest/SamplePatientEntry**").as("getOrderPage");
  });

  it("Navigate to Home Page then to Order entity Page ", function () {
    orderEntityPage = homePage.goToOrderPage();
    // Verify we're on the order page
    cy.url().should("satisfy", (url) => {
      return url.includes("/AddOrder") || url.includes("/SamplePatientEntry");
    });
  });

  it("Search patient in the search box", function () {
    patientEntryPage = orderEntityPage.getPatientPage();
    
    // Wait for patient entry form to be ready (use .should() for retry-ability)
    cy.get('[data-cy="searchPatientTabButton"]', { timeout: 10000 })
      .should("be.visible");
    
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/patient-search-results*").as("getPatientSearch");
      
      patientEntryPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      
      // Verify button is ready before clicking
      cy.get("#local_search")
        .should("be.visible")
        .should("not.be.disabled");
      
      patientEntryPage.clickSearchPatientButton();
      
      // Wait for API call instead of arbitrary wait
      cy.wait("@getPatientSearch", { timeout: 15000 }).its("response.statusCode").should("eq", 200);
      
      patientEntryPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
      
      patientEntryPage.selectPatientFromSearchResults();
      
      // Wait for form to populate (use .should() for retry-ability)
      patientEntryPage.getFirstName()
        .should("have.value", patient.firstName);
      patientEntryPage.getLastName()
        .should("have.value", patient.lastName);
    });
    
    // Verify next button is ready before clicking
    cy.get("button.forwardButton", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");
    
    orderEntityPage.clickNextButton();
  });

  it("Navigate to program selection", function () {
    // Wait for program dropdown to be ready
    cy.get("#additionalQuestionsSelect", { timeout: 10000 })
      .should("be.visible");
    
    // Wait for programs to load
    cy.wait("@getPrograms", { timeout: 10000 });
    
    // Wait for dropdown to be populated
    cy.get("#additionalQuestionsSelect option", { timeout: 10000 })
      .should("have.length.greaterThan", 1);
    
    orderEntityPage.selectCytology();
    
    // Verify next button is ready before clicking
    cy.get("button.forwardButton", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");
    
    orderEntityPage.clickNextButton();
  });

  it("Select sample type", function () {
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.selectSampleTypeOption(sample.sampleType);
        orderEntityPage.checkPanelCheckBoxField();
        orderEntityPage.collectionDate(sample.collectionDate);
      });
    });
    orderEntityPage.referTest();
    orderEntityPage.selectReferralReason();
    orderEntityPage.selectInstitute();
    
    // Verify next button is ready before clicking
    cy.get("button.forwardButton", { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");
    
    orderEntityPage.clickNextButton();
  });

  it("Generate Lab Order Number, Request and Received Dates", function () {
    cy.fixture("Order").then((order) => {
      order.samples.forEach((sample) => {
        orderEntityPage.requestDate(sample.receivedDate);
        orderEntityPage.receivedDate(sample.receivedDate);
      });
    });
    orderEntityPage.generateLabOrderNumber();
  });

  it("Select site name", function () {
    // Wait for site name input to be ready
    cy.get("#siteName", { timeout: 10000 })
      .should("be.visible")
      .should("be.enabled");
    
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterSiteName(order.siteName);
    });
  });

  it("Enter requester first and last names", function () {
    cy.fixture("Order").then((order) => {
      orderEntityPage.enterRequesterLastAndFirstName(
        order.requester.fullName,
        order.requester.firstName,
        order.requester.lastName,
      );
    });
    orderEntityPage.rememberSiteAndRequester();
  });

  it("should click submit order button", function () {
    // Set up intercept BEFORE action
    cy.intercept("POST", "**/rest/SamplePatientEntry**").as("submitOrder");
    
    // Verify submit button is ready before clicking
    cy.get("button.forwardButton", { timeout: 10000 })
      .contains("Submit")
      .should("be.visible")
      .should("not.be.disabled");
    
    orderEntityPage.clickSubmitOrderButton();
    
    // Wait for order submission API call
    cy.wait("@submitOrder", { timeout: 15000 }).then((interception) => {
      // Verify order was submitted successfully
      expect(interception.response.statusCode).to.be.oneOf([200, 201]);
    });
    
    // Wait for navigation to success page
    cy.url({ timeout: 15000 }).should("include", "/SamplePatientEntry");
    
    // Wait for success message or Print Barcode button (data-cy="printBarCode" from OrderSuccessMessage.js)
    // Use data-cy selector (PREFERRED - most stable)
    cy.get('[data-cy="printBarCode"]', { timeout: 15000 })
      .should("be.visible")
      .should("not.be.disabled");
  });
});
