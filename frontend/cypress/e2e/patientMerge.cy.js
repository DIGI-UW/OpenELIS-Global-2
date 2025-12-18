import LoginPage from "../pages/LoginPage";
import PatientMergePage from "../pages/PatientMergePage";

/**
 * Patient Merge E2E Tests
 *
 * Tests the full patient merge workflow using isolated fixture data.
 * Verifies:
 * - FR-004: Clinical data consolidation (samples reassigned)
 * - FR-009: Demographics preservation and field inheritance
 * - FR-015: Merged patients still searchable by name
 * - Audit trail creation
 *
 * Prerequisites:
 * - Admin user logged in (ROLE_GLOBAL_ADMIN required)
 * - Test fixtures loaded via cy.loadPatientMergeFixtures()
 *
 * Test Data:
 * - Fixture: cypress/fixtures/PatientMerge.json
 * - SQL setup: cypress/support/patient-merge-setup.sql
 */

let loginPage = null;
let homePage = null;
let patientMergePage = null;

describe("Patient Merge", function () {
  let testData;

  before("login as admin and load fixtures", () => {
    // Ensure test fixtures exist (reload if needed)
    cy.loadPatientMergeFixtures();

    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();

    // Load test data fixture
    cy.fixture("PatientMerge").then((data) => {
      testData = data;
    });
  });

  it("should complete full merge workflow with test patients", function () {
    patientMergePage = new PatientMergePage();
    patientMergePage.visit();

    // Step 1: Select patients - patient1 (Alice) as primary, patient2 (Bob) to merge
    patientMergePage.searchAndSelectPatient1({
      nationalId: testData.patient1.nationalId,
    });
    patientMergePage.searchAndSelectPatient2({
      nationalId: testData.patient2.nationalId,
    });
    patientMergePage.clickNextStep();

    // Step 2: Select patient1 (Alice) as primary - she will keep her ID
    patientMergePage.selectPatient1AsPrimary();
    patientMergePage.clickNextStep();

    // Step 3: Confirm merge
    patientMergePage.enterMergeReason(testData.mergeReason);
    patientMergePage.checkConfirmationCheckbox();
    patientMergePage.clickConfirmMerge();

    // Verify success notification appears
    patientMergePage.verifyMergeSuccess();
  });

  it("should verify primary patient has consolidated samples (FR-004)", function () {
    // FR-004: Clinical data consolidation - all samples from merged patient
    // should now be linked to the primary patient
    // Alice had 2 samples, Bob had 1 sample = 3 total expected after merge

    cy.task("getPatientSampleCount", testData.patient1.nationalId).then(
      (sampleCount) => {
        // Assert: Primary patient MUST have all consolidated samples
        expect(sampleCount).to.eq(testData.expectedTotalSamples);

        cy.log(
          `✓ Primary patient has ${sampleCount} samples (expected: ${testData.expectedTotalSamples})`,
        );
      },
    );
  });

  it("should verify primary patient demographics preserved (FR-009)", function () {
    // FR-009: Primary patient values take precedence during merge
    // Alice's demographics should NOT be overwritten by Bob's data

    cy.task("getPatientDemographics", testData.patient1.nationalId).then(
      (demographics) => {
        // Assert: Demographics must exist
        expect(demographics).to.not.be.null;

        // Assert: Primary patient's core demographics are preserved
        expect(demographics.firstName).to.eq(testData.patient1.firstName);
        expect(demographics.lastName).to.eq(testData.patient1.lastName);

        // Assert: Primary patient's contact info is preserved (not overwritten by Bob's)
        // Since Alice had all fields populated, her data takes precedence per FR-009
        expect(demographics.phone).to.eq(testData.patient1.phone);
        expect(demographics.email).to.eq(testData.patient1.email);
        expect(demographics.city).to.eq(testData.patient1.city);

        cy.log(
          `✓ Primary patient demographics preserved: ${demographics.firstName} ${demographics.lastName}`,
        );
        cy.log(`✓ Phone preserved: ${demographics.phone}`);
        cy.log(`✓ Email preserved: ${demographics.email}`);
      },
    );
  });

  it("should verify merged patient has zero samples after consolidation", function () {
    // After merge, Bob's samples should be reassigned to Alice
    // Bob should have 0 samples remaining

    cy.task("getPatientSampleCount", testData.patient2.nationalId).then(
      (sampleCount) => {
        // Assert: Merged patient MUST have 0 samples (all reassigned to primary)
        expect(sampleCount).to.eq(0);

        cy.log(
          `✓ Merged patient (Bob) has ${sampleCount} samples (expected: 0 - all reassigned to Alice)`,
        );
      },
    );
  });

  it("should verify merge audit record was created", function () {
    // Verify that an audit trail was created for the merge operation

    cy.task("getMergeAuditRecord", testData.patient2.nationalId).then(
      (auditRecord) => {
        // Assert: Audit record MUST exist
        expect(auditRecord).to.not.be.null;
        expect(auditRecord.auditId).to.not.be.empty;
        expect(auditRecord.mergeReason).to.include("E2E Test");

        cy.log(`✓ Merge audit record created with ID: ${auditRecord.auditId}`);
        cy.log(`✓ Merge reason recorded: ${auditRecord.mergeReason}`);
      },
    );
  });

  it("should verify merged patient is marked as merged in database", function () {
    // After merge, Bob should be marked with is_merged = true

    cy.task("getPatientDemographics", testData.patient2.nationalId).then(
      (demographics) => {
        // Assert: Demographics must exist
        expect(demographics).to.not.be.null;

        // Assert: Merged patient is flagged as merged
        expect(demographics.isMerged).to.eq(true);

        cy.log(
          `✓ Merged patient (Bob) is_merged flag: ${demographics.isMerged}`,
        );
      },
    );
  });

  it("should verify primary patient inherited empty fields from merged patient (FR-009)", function () {
    // FR-009: Primary patient values take precedence, BUT empty fields get filled from merged patient
    // Alice had NO work_phone and NO fax - these should be inherited from Bob

    cy.task("getPatientDemographics", testData.patient1.nationalId).then(
      (demographics) => {
        // Assert: Demographics must exist
        expect(demographics).to.not.be.null;

        // Assert: Primary patient inherited work_phone from Bob (Alice had NULL)
        expect(demographics.workPhone).to.eq(
          testData.expectedInheritedFields.workPhone,
        );

        // Assert: Primary patient inherited fax from Bob (Alice had NULL)
        expect(demographics.fax).to.eq(testData.expectedInheritedFields.fax);

        cy.log(
          `✓ Primary patient inherited work_phone: ${demographics.workPhone}`,
        );
        cy.log(`✓ Primary patient inherited fax: ${demographics.fax}`);
      },
    );
  });

  it("should verify primary patient searchable in PatientManagement", function () {
    // Navigate to patient management to verify merged data is accessible
    cy.visit("/PatientManagement");
    cy.wait(2000);

    // Search for the primary patient (Alice) by name
    cy.get("input#lastName").clear().type(testData.patient1.lastName);
    cy.get("input#firstName").clear().type(testData.patient1.firstName);
    cy.get("#local_search").click();
    cy.wait(3000);

    // Assert: Patient MUST be found in results
    cy.get(".cds--data-table tbody tr").should("have.length.at.least", 1);
    cy.get(".cds--data-table tbody tr")
      .first()
      .should("contain.text", testData.patient1.firstName);
    cy.get(".cds--data-table tbody tr")
      .first()
      .should("contain.text", testData.patient1.lastName);
  });

  it("should still allow searching for merged patient by name (FR-015)", function () {
    // FR-015: Redirect only applies to identity lookups (National ID, External ID)
    // Name-based searches should return the merged patient AS-IS (no redirect)
    // This is confirmed in SearchResultsServiceTest.getSearchResults_shouldNotRedirect_whenSearchingByName

    // Navigate to patient management
    cy.visit("/PatientManagement");
    cy.wait(2000);

    // Search for the merged patient (Bob) by name
    cy.get("input#firstName").clear().type(testData.patient2.firstName);
    cy.get("input#lastName").clear().type(testData.patient2.lastName);
    cy.get("#local_search").click();
    cy.wait(3000);

    // Assert: Name search MUST return results containing the merged patient
    // This is expected per FR-015 - merged patients are still searchable by name
    cy.get(".cds--data-table tbody tr").should("have.length.at.least", 1);
    cy.get(".cds--data-table tbody tr")
      .first()
      .should("contain.text", testData.patient2.firstName);
    cy.get(".cds--data-table tbody tr")
      .first()
      .should("contain.text", testData.patient2.lastName);
  });

  it("should find merged patient in merge UI search results", function () {
    // Per FR-015: Name-based searches return merged patients (no redirect/filter)
    // The frontend does NOT filter out merged patients from search results
    // Backend validation will reject the merge attempt at execution time

    patientMergePage = new PatientMergePage();
    patientMergePage.visit();

    // Search for the merged patient (Bob) in the merge UI
    patientMergePage.enterPatient1LastName(testData.patient2.lastName);
    patientMergePage.enterPatient1FirstName(testData.patient2.firstName);
    patientMergePage.searchPatient1();
    cy.wait(3000);

    // Assert: The merged patient MUST appear in search results
    // This verifies FR-015 - name search doesn't filter/redirect merged patients
    cy.get(".patientSearchResults .cds--data-table tbody tr")
      .should("have.length.at.least", 1)
      .first()
      .should("contain.text", testData.patient2.firstName);

    cy.get(".patientSearchResults .cds--data-table tbody tr")
      .first()
      .should("contain.text", testData.patient2.lastName);
  });
});
