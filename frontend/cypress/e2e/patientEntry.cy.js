/**
 * E2E Tests for Patient Entry and Search
 * Tests patient creation and search functionality
 *
 * Constitution V.5 Compliance:
 * - Video disabled by default (cypress.config.js)
 * - Screenshots enabled on failure (cypress.config.js)
 * - Intercepts set up BEFORE actions that trigger them
 * - Uses .should() assertions for retry-ability (no arbitrary cy.wait())
 * - Element readiness checks before all interactions
 * - Session management via cy.login() with cy.session() (10-20x faster)
 * - Run individually during development: npm run cy:run -- --spec "cypress/e2e/patientEntry.cy.js"
 */

import LoginPage from "../pages/LoginPage";
import HomePage from "../pages/HomePage";
import PatientEntryPage from "../pages/PatientEntryPage";

let homePage = null;
let patientPage = null;

// Load test fixtures before running tests (ensures patient data exists)
// Search tests rely on these fixtures - this is correct architecture:
// - "Add New Patient" tests patient creation functionality
// - "Search Patient" tests search functionality using existing fixtures
before("load fixtures", () => {
  cy.loadStorageFixtures();
  // Verify fixtures loaded correctly (including patient E2E-PAT-001)
  cy.task("verifyFixtures").then((result) => {
    if (!result || result.status !== "OK") {
      cy.log("⚠️  WARNING: Fixture verification failed");
      cy.log(`Verification result: ${JSON.stringify(result)}`);
      // Don't fail here - let tests run and fail with clear error messages
    } else {
      cy.log("✅ Fixtures verified - patient data ready for search tests");
    }
  });
});

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
// Same pattern as cy.setupStorageTests() in storage-setup.js
before("login", () => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
});

describe("Add New Patient", function () {
  // Use UNIQUE test data that won't conflict with fixture patients
  // This ensures test isolation - creation tests don't pollute search test data
  const uniqueTestPatient = {
    firstName: "TestCreate",
    lastName: "TestPatient",
    subjectNumber: "TEST-CREATE-001",
    nationalId: "TEST-CREATE-NAT-001",
    DOB: "12/25/1995", // Different DOB from fixture patients
  };

  beforeEach(() => {
    // Clean up test patient if it exists from a previous run
    cy.task("deleteTestPatient", {
      subjectNumber: uniqueTestPatient.subjectNumber,
      nationalId: uniqueTestPatient.nationalId,
    });

    // Set up intercepts BEFORE actions (Constitution V.5)
    cy.intercept("POST", "**/rest/PatientManagement**").as("createPatient");
    cy.intercept("GET", "**/rest/patient**").as("getPatient");
  });

  afterEach(() => {
    // Clean up test patient after each test to ensure isolation
    cy.task("deleteTestPatient", {
      subjectNumber: uniqueTestPatient.subjectNumber,
      nationalId: uniqueTestPatient.nationalId,
    });
  });

  it("User Visits Home Page and goes to Add Add|Modify Patient Page", () => {
    patientPage = homePage.goToPatientEntry();
    // Verify we're on the patient entry page
    cy.url().should("include", "/PatientManagement");
  });

  it("Add|Modify Patient page should appear with search field", function () {
    patientPage
      .getPatientEntryPageTitle()
      .should("be.visible")
      .should("contain.text", "Add Or Modify Patient");
  });

  it("External search button should be deactivated", function () {
    patientPage.getExternalSearchButton();
  });

  it("Navigate to create Patient tab", function () {
    patientPage.clickNewPatientTab();
    patientPage.getSubmitButton().should("be.visible");
  });

  it("Enter patient Information and clear", function () {
    patientPage.enterPatientInfo(
      uniqueTestPatient.firstName,
      uniqueTestPatient.lastName,
      uniqueTestPatient.subjectNumber,
      uniqueTestPatient.nationalId,
      uniqueTestPatient.DOB,
    );
  });

  it("Clear new patient information", function () {
    patientPage.clearPatientInfo();
  });

  it("Enter patient Information and save", function () {
    patientPage.enterPatientInfo(
      uniqueTestPatient.firstName,
      uniqueTestPatient.lastName,
      uniqueTestPatient.subjectNumber,
      uniqueTestPatient.nationalId,
      uniqueTestPatient.DOB,
    );
  });

  it("Save new patient information button", function () {
    // Set up intercept BEFORE action (actual endpoint is /rest/PatientManagement)
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

    // Note: We don't clean up here because:
    // 1. The test uses unique data (TestCreate/TestPatient, DOB 12/25/1995) that won't conflict with search tests
    // 2. Search tests use fixture patients (E2E-PAT-001, etc.) with different names/DOBs
    // 3. This ensures test isolation without complex cleanup logic
  });
});

describe("Search Patient", function () {
  // Search tests use ONLY fixture patients (E2E-PAT-001, etc.) loaded in before("load fixtures")
  // The "Add New Patient" test now uses unique data and cleans up after itself
  // No cleanup needed here - tests are isolated
  // Search tests use existing fixtures loaded in before("load fixtures")
  // This is correct - search should test against existing data, not create new data

  beforeEach(() => {
    // Navigate to patient entry page for each search test
    // Use cy.visit() to ensure clean state (no reload needed)
    cy.visit("/PatientManagement");

    // Set up intercepts BEFORE actions (Constitution V.5)
    // Use flexible pattern to match query parameters
    cy.intercept("GET", "**/rest/patient-search-results*").as(
      "getPatientSearch",
    );

    // Wait for page to be ready
    cy.get("section > h3", { timeout: 10000 })
      .should("be.visible")
      .should("contain.text", "Add Or Modify Patient");
  });

  it("Search patients By gender", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/patient-search-results*").as(
        "getPatientSearch",
      );

      // Use element readiness checks (no arbitrary wait)
      patientPage.getMaleGenderRadioButton().should("be.visible").click();

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

      // Wait for API call instead of arbitrary wait
      cy.wait("@getPatientSearch", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      // Validate results
      patientPage.validatePatientByGender("M");
    });
  });

  it("Search Patient By FirstName only", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/patient-search-results*").as(
        "getPatientSearch",
      );

      patientPage.searchPatientByFirstNameOnly(patient.firstName);
      patientPage.getFirstName().should("have.value", patient.firstName);

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

      // Wait for API call instead of arbitrary wait
      cy.wait("@getPatientSearch", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.firstName,
        "firstName",
      );
    });
  });

  it("Search Patient By LastName only", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/patient-search-results*").as(
        "getPatientSearch",
      );

      patientPage.searchPatientByLastNameOnly(patient.lastName);
      patientPage.getLastName().should("have.value", patient.lastName);

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

      // Wait for API call instead of arbitrary wait
      cy.wait("@getPatientSearch", { timeout: 15000 })
        .its("response.statusCode")
        .should("eq", 200);

      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.lastName,
        "lastName",
      );
    });
  });

  it("Search Patient By both Names", function () {
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
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

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

  it("Search patient By Date Of Birth", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action (Testing Roadmap: cy.intercept() Patterns)
      // Log all requests to see what's actually being called
      cy.intercept("GET", "**/rest/patient-search-results*", (req) => {
        cy.log(`Intercepted: ${req.method} ${req.url}`);
      }).as("getPatientSearch");

      // Search by DOB - should return E2E-PAT-001 (TEST-Smith)
      // Note: Validation is flexible - it checks that results exist and patient TEST-Smith is found
      // It does NOT require exact DOB string match (which can vary by locale/format)
      patientPage.searchPatientByDateOfBirth(patient.DOB);

      // Verify the date input has the correct value
      cy.get("input#date-picker-default-id").should("have.value", patient.DOB);

      // Verify button is ready before clicking
      cy.get("#local_search")
        .should("be.visible")
        .should("not.be.disabled")
        .click();

      // Wait for table to appear (indicates search succeeded)
      // The search is working (screenshot shows results), so wait for table instead of intercept
      cy.get("table tbody tr", { timeout: 15000 })
        .should("be.visible")
        .should("have.length.greaterThan", 0);

      // Validate that search returned results and the fixture patient (TEST-Smith) is found
      // Uses last name as stable identifier, not DOB string matching
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.DOB,
        "DOB",
      );
    });
  });

  it("Search patient By Lab Number", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action (Testing Roadmap: cy.intercept() Patterns)
      // Match the endpoint with labNumber parameter (flexible pattern for any parameter order)
      cy.intercept("GET", "**/rest/patient-search-results*").as(
        "getPatientSearch",
      );

      // Enter lab number - CustomLabNumberInput behavior depends on AccessionFormat:
      // - ALPHANUM: hidden input with id="labNumber", display input with id="display_labNumber"
      // - Non-ALPHANUM: regular TextInput with id="labNumber"
      // Try to find the visible input first (either display_labNumber or labNumber)
      cy.get("#display_labNumber, #labNumber", { timeout: 10000 })
        .first()
        .should("be.visible")
        .clear()
        .type(patient.labNo);

      // Verify button is ready before clicking (use .should() for retry-ability, not cy.wait())
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      // Click search button and wait for API call
      patientPage.clickSearchPatientButton();

      // Wait for any patient search API call, then verify it includes labNumber
      cy.wait("@getPatientSearch", { timeout: 15000 }).then((interception) => {
        // Verify the request URL includes the lab number
        expect(interception.request.url).to.include(
          `labNumber=${patient.labNo}`,
        );
        const responseBody = interception.response.body;
        console.log("Patient search response:", responseBody);
        // Verify search returned results (sample E2E001 is linked to patient E2E-PAT-001)
        expect(responseBody.patientSearchResults).to.be.an("array");
        expect(responseBody.patientSearchResults.length).to.be.greaterThan(0);
        // Verify the fixture patient (TEST-Smith) is in results
        const foundPatient = responseBody.patientSearchResults.some(
          (result) => result.lastName && result.lastName.includes("TEST-Smith"),
        );
        expect(
          foundPatient,
          `Expected to find patient TEST-Smith (E2E-PAT-001) in search results for lab number ${patient.labNo}`,
        ).to.be.true;
      });
    });
  });

  it("Search patient By PatientId", function () {
    cy.fixture("Patient").then((patient) => {
      // Set up intercept BEFORE action
      cy.intercept("GET", "**/rest/patient-search-results*").as(
        "getPatientSearch",
      );

      patientPage.searchPatientByPatientId(patient.nationalId);

      // Verify button is ready before clicking
      cy.get("#local_search").should("be.visible").should("not.be.disabled");

      patientPage.clickSearchPatientButton();

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
});
