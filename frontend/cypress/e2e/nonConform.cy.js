/**
 * E2E Tests for Non-Conforming Event (NCE) Reporting
 * Tests NCE reporting workflow for different search types
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/nonConform.cy.js"
 */

import LoginPage from "../pages/LoginPage";

let homePage = null;
let nonConform = null;

// Load test fixtures before running tests (ensures patient data exists)
before("Load test fixtures", () => {
  // Wait for backend API to be available before loading fixtures
  cy.waitForBackend("/rest/storage/samples");
  // Load test data (patients, samples) needed for nonConform tests
  cy.loadStorageFixtures();
});

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Report Non-Conforming Event", function () {
  beforeEach(() => {
    // Navigate to NCE page for each test
    cy.visit("/ReportNonConformingEvent");

    // Set up intercepts BEFORE actions (Constitution V.5)
    // The actual API endpoint is /rest/nonconformevents?${type}=${value}
    cy.intercept("GET", "**/rest/nonconformevents?*").as("getNonConformSearch");
    // Submit endpoint is /rest/reportnonconformingevent (POST)
    cy.intercept("POST", "**/rest/reportnonconformingevent").as("submitNCE");
  });

  it("User visits Report Non-Conforming Event Page", function () {
    nonConform = homePage.goToReportNCE();

    // Verify we're on the NCE page
    cy.url().should("include", "/ReportNonConformingEvent");

    nonConform
      .getReportNonConformTitle()
      .should("be.visible")
      .should("contain.text", "Report Non-Conforming Event (NCE)");
  });

  it("Report NCE by Last Name and Enter Details", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action (actual endpoint is /rest/nonconformevents?${type}=${value})
      cy.intercept("GET", "**/rest/nonconformevents?*").as(
        "getNonConformSearch",
      );

      nonConform.selectSearchType("Last Name");
      nonConform.enterSearchField(patient.lastName);

      // Verify button is ready before clicking (use .should() for retry-ability)
      cy.get("button")
        .contains("Search")
        .should("be.visible")
        .should("not.be.disabled");

      nonConform.clickSearchButton();

      // Wait for API call (use default timeout - .should() provides retry-ability)
      cy.wait("@getNonConformSearch")
        .its("response.statusCode")
        .should("eq", 200);

      // Wait for search results to appear (use .should() for retry-ability)
      cy.get("[data-testid='nce-search-result']").should("be.visible");

      nonConform.validateSearchResult(patient.labNo);
      nonConform.clickCheckbox({ force: true });

      // Set up intercept BEFORE action (form loads data via API)
      cy.intercept("GET", "**/rest/reportnonconformingevent?*").as(
        "loadNCEForm",
      );

      // Verify button is ready before clicking (use .should() for retry-ability)
      cy.get("[data-testid='nce-goto-form-button']")
        .should("be.visible")
        .should("not.be.disabled");

      nonConform.clickGoToNceFormButton();

      // Wait for form data to load via API (use default timeout - .should() provides retry-ability)
      cy.wait("@loadNCEForm").its("response.statusCode").should("eq", 200);

      // Wait for NCE form to appear (form is conditionally rendered when nceForm.show && nceForm.data)
      // Use nce-number-result as indicator that form has loaded (use .should() for retry-ability)
      cy.get('[data-testid="nce-number-result"]').should("be.visible");

      nonConform.getAndSaveNceNumber();

      cy.fixture("NonConform").then((nonConformData) => {
        // Wait for date picker input to be ready (use .should() for retry-ability)
        cy.get("input#startDate").should("be.visible").should("be.enabled");

        nonConform.enterStartDate(nonConformData.dateOfEvent);

        // Wait for reporting unit dropdown to be ready (use .should() for retry-ability)
        cy.get("#reportingUnits")
          .should("be.visible")
          .should("not.be.disabled");

        nonConform.selectReportingUnit(nonConformData.reportingUnit);
        nonConform.enterDescription(nonConformData.description);
        nonConform.enterSuspectedCause(nonConformData.suspectedCause);
        nonConform.enterCorrectiveAction(
          nonConformData.proposedCorrectiveAction,
        );

        // Set up intercept BEFORE action (actual endpoint is /rest/reportnonconformingevent)
        cy.intercept("POST", "**/rest/reportnonconformingevent").as(
          "submitNCE",
        );

        // Verify submit button is ready before clicking (use .should() for retry-ability)
        cy.get("[data-testid='nce-submit-button']")
          .should("be.visible")
          .should("not.be.disabled");

        nonConform.submitForm();

        // Wait for API call (use default timeout - .should() provides retry-ability)
        cy.wait("@submitNCE")
          .its("response.statusCode")
          .should("be.oneOf", [200, 201]);
      });
    });
  });

  it("Report NCE by First Name and Enter Details", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action (actual endpoint is /rest/nonconformevents?${type}=${value})
      cy.intercept("GET", "**/rest/nonconformevents?*").as(
        "getNonConformSearch",
      );

      nonConform.selectSearchType("First Name");
      nonConform.enterSearchField(patient.firstName);

      // Verify button is ready before clicking (use .should() for retry-ability)
      cy.get("button")
        .contains("Search")
        .should("be.visible")
        .should("not.be.disabled");

      nonConform.clickSearchButton();

      // Wait for API call (use default timeout - .should() provides retry-ability)
      cy.wait("@getNonConformSearch")
        .its("response.statusCode")
        .should("eq", 200);

      // Wait for search results to appear (use .should() for retry-ability)
      cy.get("[data-testid='nce-search-result']").should("be.visible");

      nonConform.validateSearchResult(patient.labNo);
      nonConform.clickCheckbox({ force: true });

      // Set up intercept BEFORE action (form loads data via API)
      cy.intercept("GET", "**/rest/reportnonconformingevent?*").as(
        "loadNCEForm",
      );

      // Verify button is ready before clicking (use .should() for retry-ability)
      cy.get("[data-testid='nce-goto-form-button']")
        .should("be.visible")
        .should("not.be.disabled");

      nonConform.clickGoToNceFormButton();

      // Wait for form data to load via API (use default timeout - .should() provides retry-ability)
      cy.wait("@loadNCEForm").its("response.statusCode").should("eq", 200);

      // Wait for NCE form to appear (form is conditionally rendered when nceForm.show && nceForm.data)
      // Use nce-number-result as indicator that form has loaded (use .should() for retry-ability)
      cy.get('[data-testid="nce-number-result"]').should("be.visible");

      cy.fixture("NonConform").then((nonConformData) => {
        // Wait for date picker input to be ready (use .should() for retry-ability)
        cy.get("input#startDate").should("be.visible").should("be.enabled");

        nonConform.enterStartDate(nonConformData.dateOfEvent);

        // Wait for reporting unit dropdown to be ready (use .should() for retry-ability)
        cy.get("#reportingUnits")
          .should("be.visible")
          .should("not.be.disabled");

        nonConform.selectReportingUnit(nonConformData.reportingUnit);
        nonConform.enterDescription(nonConformData.description);
        nonConform.enterSuspectedCause(nonConformData.suspectedCause);
        nonConform.enterCorrectiveAction(
          nonConformData.proposedCorrectiveAction,
        );

        // Set up intercept BEFORE action (actual endpoint is /rest/reportnonconformingevent)
        cy.intercept("POST", "**/rest/reportnonconformingevent").as(
          "submitNCE",
        );

        // Verify submit button is ready before clicking (use .should() for retry-ability)
        cy.get("[data-testid='nce-submit-button']")
          .should("be.visible")
          .should("not.be.disabled");

        nonConform.submitForm();

        // Wait for API call (use default timeout - .should() provides retry-ability)
        cy.wait("@submitNCE")
          .its("response.statusCode")
          .should("be.oneOf", [200, 201]);
      });
    });
  });

  it("Report NCE by PatientID and Enter Details", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action (actual endpoint is /rest/nonconformevents?${type}=${value})
      cy.intercept("GET", "**/rest/nonconformevents?*").as(
        "getNonConformSearch",
      );

      nonConform.selectSearchType("Patient Identification Code");
      nonConform.enterSearchField(patient.nationalId);

      // Verify button is ready before clicking (use .should() for retry-ability)
      cy.get("button")
        .contains("Search")
        .should("be.visible")
        .should("not.be.disabled");

      nonConform.clickSearchButton();

      // Wait for API call (use default timeout - .should() provides retry-ability)
      cy.wait("@getNonConformSearch")
        .its("response.statusCode")
        .should("eq", 200);

      // Wait for search results to appear (use .should() for retry-ability)
      cy.get("[data-testid='nce-search-result']").should("be.visible");

      nonConform.clickCheckbox({ force: true });

      // Set up intercept BEFORE action (form loads data via API)
      cy.intercept("GET", "**/rest/reportnonconformingevent?*").as(
        "loadNCEForm",
      );

      // Verify button is ready before clicking (use .should() for retry-ability)
      cy.get("[data-testid='nce-goto-form-button']")
        .should("be.visible")
        .should("not.be.disabled");

      nonConform.clickGoToNceFormButton();

      // Wait for form data to load via API (use default timeout - .should() provides retry-ability)
      cy.wait("@loadNCEForm").its("response.statusCode").should("eq", 200);

      // Wait for NCE form to appear (form is conditionally rendered when nceForm.show && nceForm.data)
      // Use nce-number-result as indicator that form has loaded (use .should() for retry-ability)
      cy.get('[data-testid="nce-number-result"]').should("be.visible");

      cy.fixture("NonConform").then((nonConformData) => {
        // Wait for date picker input to be ready (use .should() for retry-ability)
        cy.get("input#startDate").should("be.visible").should("be.enabled");

        nonConform.enterStartDate(nonConformData.dateOfEvent);

        // Wait for reporting unit dropdown to be ready (use .should() for retry-ability)
        cy.get("#reportingUnits")
          .should("be.visible")
          .should("not.be.disabled");

        nonConform.selectReportingUnit(nonConformData.reportingUnit);
        nonConform.enterDescription(nonConformData.description);
        nonConform.enterSuspectedCause(nonConformData.suspectedCause);
        nonConform.enterCorrectiveAction(
          nonConformData.proposedCorrectiveAction,
        );

        // Set up intercept BEFORE action (actual endpoint is /rest/reportnonconformingevent)
        cy.intercept("POST", "**/rest/reportnonconformingevent").as(
          "submitNCE",
        );

        // Verify submit button is ready before clicking (use .should() for retry-ability)
        cy.get("[data-testid='nce-submit-button']")
          .should("be.visible")
          .should("not.be.disabled");

        nonConform.submitForm();

        // Wait for API call (use default timeout - .should() provides retry-ability)
        cy.wait("@submitNCE")
          .its("response.statusCode")
          .should("be.oneOf", [200, 201]);
      });
    });
  });
});
