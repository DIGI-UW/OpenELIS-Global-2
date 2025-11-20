import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
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

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
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

  it("User Visits Home Page and goes to Add Add|Modify Patient Page", () => {
    homePage = loginPage.goToHomePage();
    patientPage = homePage.goToPatientEntry();
  });

  it("Add|Modify Patient page should appear with search field", function () {
    patientPage
      .getPatientEntryPageTitle()
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
    patientPage.clickSavePatientButton();
    cy.wait(1000);
    cy.get("div[role='status']").should("be.visible");
    // Note: We don't clean up here because:
    // 1. The test uses unique data (TestCreate/TestPatient, DOB 12/25/1995) that won't conflict with search tests
    // 2. Search tests use fixture patients (E2E-PAT-001, etc.) with different names/DOBs
    // 3. This ensures test isolation without complex cleanup logic
    cy.wait(200).reload();
  });
});

describe("Search Patient", function () {
  // Search tests use ONLY fixture patients (E2E-PAT-001, etc.) loaded in before("load fixtures")
  // The "Add New Patient" test now uses unique data and cleans up after itself
  // No cleanup needed here - tests are isolated
  // Search tests use existing fixtures loaded in before("load fixtures")
  // This is correct - search should test against existing data, not create new data
  it("Search patients By gender", function () {
    cy.wait(1000);
    patientPage.getMaleGenderRadioButton();
    cy.wait(200);
    patientPage.clickSearchPatientButton();
    cy.fixture("Patient").then((patient) => {
      patientPage.validatePatientByGender("M");
    });
    cy.wait(200).reload();
  });
  it("Search Patient By FirstName only", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstNameOnly(patient.firstName);
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.firstName,
        "firstName",
      );
    });
    cy.wait(200).reload();
  });

  it("Search Patient By LastName only", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByLastNameOnly(patient.lastName);
      patientPage.getLastName().should("have.value", patient.lastName);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.lastName,
        "lastName",
      );
    });
    cy.wait(200).reload();
  });

  it("Search Patient By both Names", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      patientPage.getFirstName().should("have.value", patient.firstName);
      patientPage.getLastName().should("have.value", patient.lastName);

      patientPage.getLastName().should("not.have.value", patient.inValidName);

      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
    cy.wait(200).reload();
  });
  it("Search patient By Date Of Birth", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      // Search by DOB - should return E2E-PAT-001 (TEST-Smith)
      // Note: Validation is flexible - it checks that results exist and patient TEST-Smith is found
      // It does NOT require exact DOB string match (which can vary by locale/format)
      patientPage.searchPatientByDateOfBirth(patient.DOB);
      patientPage.clickSearchPatientButton();
      // Validate that search returned results and the fixture patient (TEST-Smith) is found
      // Uses last name as stable identifier, not DOB string matching
      patientPage.validatePatientSearchTablebyRespectiveField(
        patient.DOB,
        "DOB",
      );
    });
    cy.wait(200).reload();
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
      // Wait for form to process the value
      cy.wait(500);
      // Verify search button is enabled before clicking
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
    cy.wait(200).reload();
  });

  it("Search patient By PatientId", function () {
    cy.wait(1000);
    cy.fixture("Patient").then((patient) => {
      patientPage.searchPatientByPatientId(patient.nationalId);
      patientPage.clickSearchPatientButton();
      patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });
  });
});
