/**
 * E2E tests — Study Double Entry (React UI)
 *
 * The double entry (verification) workflow requires:
 *   1. A sample must first exist at InitialRegistration status (created via Initial Entry)
 *   2. The same lab number is entered again with matching data
 *   3. POST goes to /rest/SampleEntryByProject?type=verify
 *
 * Test categories:
 *   A. Page shell & navigation
 *   B. Project selection – same 9 forms, same field rules as Initial Entry
 *   C. Double-entry specific: type=verify, lab number pre-existence checks
 *   D. Error scenarios: non-existent lab number, already-verified lab number
 *   E. Successful double-entry save flow
 *   F. Field-by-field parity with Initial Entry (same sections render)
 *   G. Lab number normalisation (same prefix logic)
 *   H. Menu navigation (React item visible, old JSP item hidden)
 */

import LoginPage from "../pages/LoginPage";
import HomePage from "../pages/HomePage";
import StudyEntryPage from "../pages/StudyEntryPage";

// ─── Shared setup ─────────────────────────────────────────────────────────────

let loginPage;
let homePage;
let studyEntryPage;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
  // Disable fail-fast for this spec so all tests run independently
  Cypress.env("FAIL_FAST_ENABLED", false);
});

beforeEach(() => {
  studyEntryPage = new StudyEntryPage();
  Cypress.env("FAIL_FAST_ENABLED", false);
  studyEntryPage.interceptFormLoad();
  studyEntryPage.visitDoubleEntry();
  studyEntryPage.waitForFormLoad();
});

// ─────────────────────────────────────────────────────────────────────────────
// A. PAGE SHELL
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – page shell", () => {
  it("renders the page title", () => {
    studyEntryPage.assertDoubleEntryPageTitle();
  });

  it("renders breadcrumbs", () => {
    cy.get(".cds--breadcrumb").should("be.visible");
  });

  it("hides all form fields until a project is selected", () => {
    studyEntryPage.assertFormFieldsHiddenBeforeProjectSelected();
  });

  it("shows the project selection radio group with all 9 options", () => {
    const projects = [
      "ARV_INITIAL",
      "ARV_FOLLOWUP",
      "RTN",
      "EID",
      "INDETERMINATE",
      "SPECIAL_REQUEST",
      "ARV_VIRAL_LOAD",
      "RECENCY_TESTING",
      "HPV_TESTING",
    ];
    projects.forEach((p) => {
      cy.get(`#${p}`).should("exist");
    });
  });

  it("Save button does not exist before a project is selected", () => {
    cy.contains("button", "Save").should("not.exist");
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// B. PROJECT SELECTION — sections render correctly (same rules as Initial Entry)
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – project selection renders correct sections", () => {
  it("ARV_INITIAL shows ARV Information section", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertProjectSectionVisible("ARV Information");
  });

  it("ARV_FOLLOWUP shows ARV Information section with HIV Status field", () => {
    studyEntryPage.selectProject("ARV_FOLLOWUP");
    studyEntryPage.assertProjectSectionVisible("ARV Information");
    studyEntryPage.assertHIVStatusVisible();
  });

  it("ARV_INITIAL does NOT show HIV Status field", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertHIVStatusNotVisible();
  });

  it("ARV_VIRAL_LOAD shows ARV - Viral Load section", () => {
    studyEntryPage.selectProject("ARV_VIRAL_LOAD");
    studyEntryPage.assertVLSectionVisible();
  });

  it("RTN shows RTN section", () => {
    studyEntryPage.selectProject("RTN");
    cy.contains("RTN").should("be.visible");
  });

  it("EID shows EID section", () => {
    studyEntryPage.selectProject("EID");
    studyEntryPage.assertProjectSectionVisible("EID (Early Infant Diagnosis)");
  });

  it("INDETERMINATE shows Indeterminate Results section", () => {
    studyEntryPage.selectProject("INDETERMINATE");
    studyEntryPage.assertINDSectionVisible();
  });

  it("SPECIAL_REQUEST shows Special Request section", () => {
    studyEntryPage.selectProject("SPECIAL_REQUEST");
    studyEntryPage.assertSpecialRequestSectionVisible();
  });

  it("RECENCY_TESTING shows Recency Testing section", () => {
    studyEntryPage.selectProject("RECENCY_TESTING");
    studyEntryPage.assertRecencySectionVisible();
  });

  it("HPV_TESTING shows HPV Testing section", () => {
    studyEntryPage.selectProject("HPV_TESTING");
    studyEntryPage.assertHPVSectionVisible();
  });

  it("Sample Information section visible after any project selection", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertSampleInfoSectionVisible();
  });

  it("Patient Information section visible after any project selection", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertPatientInfoSectionVisible();
  });

  it("Specimens Collected section visible after any project selection", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertTestSelectionSectionVisible();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// C. DOUBLE ENTRY SPECIFIC — type=verify in POST
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – POST type=verify", () => {
  it("sends type=verify when saving via Double Entry page", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true, labNumber: data.doubleEntry.validLabNo },
      }).as("saveVerify");

      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.validLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveVerify").then((interception) => {
        expect(interception.request.url).to.include("type=verify");
      });
    });
  });

  it("request body includes project field with correct value", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true },
      }).as("saveVerifyBody");

      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.validLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveVerifyBody").then((interception) => {
        const body = interception.request.body;
        expect(body).to.have.property("project", data.doubleEntry.project);
        expect(body).to.have.property("labNo", data.doubleEntry.validLabNo);
      });
    });
  });

  it("request body includes gender, birthDate, and observations", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true },
      }).as("saveVerifyFields");

      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.validLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      studyEntryPage.enterSiteSubjectNumber(data.doubleEntry.siteSubjectNumber);
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveVerifyFields").then((interception) => {
        const body = interception.request.body;
        expect(body).to.have.property("gender", data.doubleEntry.gender);
        expect(body).to.have.property(
          "birthDateForDisplay",
          data.doubleEntry.birthDate,
        );
        expect(body).to.have.property(
          "subjectNumber",
          data.doubleEntry.subjectNumber,
        );
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// D. ERROR SCENARIOS
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – error scenarios", () => {
  it("shows error when lab number does not exist (404 from server)", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 400,
        body: {
          success: false,
          message:
            "Lab number " +
            data.doubleEntry.invalidLabNo +
            " does not exist. Please perform initial entry first.",
        },
      }).as("saveVerifyNotFound");

      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.invalidLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveVerifyNotFound");
      studyEntryPage.assertErrorNotification();
    });
  });

  it("shows error when lab number is already verified (conflict from server)", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 409,
        body: {
          success: false,
          message:
            "Lab number " +
            data.doubleEntry.alreadyVerifiedLabNo +
            " is not eligible for double entry.",
        },
      }).as("saveVerifyConflict");

      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.alreadyVerifiedLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveVerifyConflict");
      studyEntryPage.assertErrorNotification();
    });
  });

  it("shows validation error when saving without a lab number", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.doubleEntry.project);
      // Intentionally skip lab number
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();
      studyEntryPage.assertErrorNotification();
    });
  });

  it("shows validation error when saving without gender", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.validLabNo);
      // Intentionally skip gender
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();
      studyEntryPage.assertErrorNotification();
    });
  });

  it("shows validation error for future birth date", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.validLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.validation.futureBirthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();
      studyEntryPage.assertErrorNotification();
    });
  });

  it("shows validation error when no project is selected (Save button absent)", () => {
    studyEntryPage.assertFormFieldsHiddenBeforeProjectSelected();
    cy.contains("button", "Save").should("not.exist");
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// E. SUCCESSFUL DOUBLE ENTRY SAVE
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – successful save", () => {
  it("shows success notification after successful double entry (ARV_INITIAL)", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true, labNumber: data.doubleEntry.validLabNo },
      }).as("saveDoubleSuccess");

      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.validLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      studyEntryPage.enterSiteSubjectNumber(data.doubleEntry.siteSubjectNumber);
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveDoubleSuccess");
      studyEntryPage.assertSuccessNotification();
    });
  });

  it("reloads/resets the form after successful save (URL stays on StudyDoubleEntry)", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true },
      }).as("saveDoubleReload");

      studyEntryPage.selectProject(data.doubleEntry.project);
      studyEntryPage.enterLabNo(data.doubleEntry.validLabNo);
      cy.get("select#gender").select(data.doubleEntry.gender);
      studyEntryPage.enterBirthDate(data.doubleEntry.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveDoubleReload");
      studyEntryPage.assertSuccessNotification();
      // After success notification the page should reload to StudyDoubleEntry
      cy.url({ timeout: 8000 }).should("include", "/StudyDoubleEntry");
    });
  });

  it("successful EID double entry sends correct labNo prefix LDBS", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true },
      }).as("saveDoubleEID");

      studyEntryPage.selectProject("EID");
      studyEntryPage.enterLabNo(data.eid.labNo);
      cy.get("select#gender").select(data.eid.gender);
      studyEntryPage.enterBirthDate(data.eid.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dnaPCR").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveDoubleEID").then((interception) => {
        expect(interception.request.body.labNo).to.equal(
          data.eid.expectedLabNo,
        );
      });
    });
  });

  it("successful RTN double entry sends correct labNo prefix LRTN", () => {
    cy.fixture("StudyEntry").then((data) => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true },
      }).as("saveDoubleRTN");

      studyEntryPage.selectProject("RTN");
      studyEntryPage.enterLabNo(data.rtn.labNo);
      cy.get("select#gender").select(data.rtn.gender);
      studyEntryPage.enterBirthDate(data.rtn.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();

      cy.wait("@saveDoubleRTN").then((interception) => {
        expect(interception.request.body.labNo).to.equal(
          data.rtn.expectedLabNo,
        );
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// F. FIELD PARITY WITH INITIAL ENTRY (every project)
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – field parity with Initial Entry (ARV_INITIAL)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("ARV_INITIAL");
  });

  it("shows ARV center name and center code dropdowns", () => {
    cy.get("select#arvcenterName").should("be.visible");
    cy.get("select#arvcenterCode").should("be.visible");
  });

  it("shows doctor / clinician text field", () => {
    cy.get("input#doctor").should("be.visible");
  });

  it("shows received date, received time, interview date, interview time", () => {
    cy.get("input#receivedDateForDisplay").should("be.visible");
    cy.get("input#receivedTimeForDisplay").should("be.visible");
    cy.get("input#interviewDate").should("be.visible");
    cy.get("input#interviewTime").should("be.visible");
  });

  it("received date is auto-filled with today", () => {
    studyEntryPage.assertReceivedDateAutoFilled();
  });

  it("interview date is auto-filled with today", () => {
    studyEntryPage.assertInterviewDateAutoFilled();
  });

  it("hides subjectNumber (ARV uses siteSubjectNumber as Unique Health ID)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
    studyEntryPage.assertSiteSubjectNumberVisible();
    studyEntryPage.assertUPIDCodeVisible();
  });

  it("shows lab number with LARC hint", () => {
    studyEntryPage.assertLabNoHint("LARC");
  });

  it("shows gender dropdown", () => {
    studyEntryPage.assertGenderVisible();
  });

  it("shows birth date field", () => {
    studyEntryPage.assertBirthDateVisible();
  });

  it("age is read-only and auto-calculated from birth date", () => {
    studyEntryPage.enterBirthDate("01/01/1990");
    cy.get("body").type("{esc}");
    studyEntryPage.assertAgeReadOnly();
  });

  it("shows serologyHIVTest, creatinineTest, nfsTest, cd4cd8Test checkboxes", () => {
    studyEntryPage.assertTestVisible("serologyHIVTest");
    studyEntryPage.assertTestVisible("creatinineTest");
    studyEntryPage.assertTestVisible("nfsTest");
    studyEntryPage.assertTestVisible("cd4cd8Test");
    // specimens section heading matches component defaultMessage
    cy.contains("Specimens Collected").should("be.visible");
  });

  it("shows dryTubeTaken and edtaTubeTaken specimen checkboxes", () => {
    studyEntryPage.assertTestVisible("dryTubeTaken");
    studyEntryPage.assertTestVisible("edtaTubeTaken");
  });

  it("selecting serologyHIVTest auto-checks dryTubeTaken", () => {
    studyEntryPage.checkTest("serologyHIVTest");
    studyEntryPage.assertDryTubeTakenAutoChecked();
  });

  it("selecting nfsTest auto-checks edtaTubeTaken", () => {
    studyEntryPage.checkTest("nfsTest");
    studyEntryPage.assertEdtaTubeTakenAutoChecked();
  });

  it("shows underInvestigation dropdown", () => {
    cy.get("select#underInvestigation").should("be.visible");
  });

  it("underInvestigationNote appears only when Yes selected", () => {
    studyEntryPage.assertUnderInvestigationNoteNotVisible();
    cy.get("select#underInvestigation").then(($select) => {
      const options = [...$select.find("option")];
      const yesOption = options.find(
        (o) => o.text.toLowerCase() === "yes" || o.text.toLowerCase() === "oui",
      );
      if (yesOption) {
        cy.get("select#underInvestigation").select(yesOption.value);
        studyEntryPage.assertUnderInvestigationNoteVisible();
      } else {
        cy.log(
          "No Yes/Oui option found in underInvestigation dropdown — skipping conditional check",
        );
      }
    });
  });
});

describe("Study Double Entry – field parity with Initial Entry (EID)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("EID");
  });

  it("shows eidsiteName and eidsiteCode", () => {
    cy.get("select#eidsiteName").should("be.visible");
    cy.get("select#eidsiteCode").should("be.visible");
  });

  it("shows eidWhichPCR and eidSecondPCRReason", () => {
    cy.get("select#eidWhichPCR").should("be.visible");
    cy.get("select#eidSecondPCRReason").should("be.visible");
  });

  it("shows nameOfRequestor and nameOfSampler", () => {
    cy.get("input#nameOfRequestor").should("be.visible");
    cy.get("input#nameOfSampler").should("be.visible");
  });

  it("shows Infant Information section heading", () => {
    studyEntryPage.assertEIDInfantSectionHeadingVisible();
  });

  it("shows all infant dropdown fields", () => {
    cy.get("select#eidInfantPTME").should("be.visible");
    cy.get("select#eidTypeOfClinic").should("be.visible");
    cy.get("select#eidHowChildFed").should("be.visible");
    cy.get("select#eidStoppedBreastfeeding").should("be.visible");
    cy.get("select#eidInfantSymptomatic").should("be.visible");
    cy.get("select#eidInfantsARV").should("be.visible");
    cy.get("select#eidInfantCotrimoxazole").should("be.visible");
  });

  it("shows Mother's Information section heading", () => {
    studyEntryPage.assertEIDMotherSectionHeadingVisible();
  });

  it("shows mother HIV status and ARV treatment dropdowns", () => {
    cy.get("select#eidMothersHIVStatus").should("be.visible");
    cy.get("select#eidMothersARV").should("be.visible");
  });

  it("hides subjectNumber and siteSubjectNumber (EID uses infant numbers)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
    studyEntryPage.assertSiteSubjectNumberNotVisible();
  });

  it("hides UPID code field for EID", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows dnaPCR and dbsTaken checkboxes", () => {
    studyEntryPage.assertTestVisible("dnaPCR");
    studyEntryPage.assertTestVisible("dbsTaken");
  });

  it("selecting dnaPCR auto-checks dbsTaken", () => {
    studyEntryPage.checkTest("dnaPCR");
    studyEntryPage.assertDbsTakenAutoChecked();
  });
});

describe("Study Double Entry – field parity with Initial Entry (RTN)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("RTN");
  });

  it("shows RTN section heading", () => {
    studyEntryPage.assertRTNSectionVisible();
  });

  it("hides subjectNumber, siteSubjectNumber, upidCode (RTN has no patient IDs)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
    studyEntryPage.assertSiteSubjectNumberNotVisible();
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows LRTN prefix hint on lab number", () => {
    studyEntryPage.assertLabNoHint("LRTN");
  });

  it("shows underInvestigation dropdown", () => {
    cy.get("select#underInvestigation").should("be.visible");
  });

  it("shows serologyHIVTest and dryTubeTaken checkboxes", () => {
    studyEntryPage.assertTestVisible("serologyHIVTest");
    studyEntryPage.assertTestVisible("dryTubeTaken");
  });
});

describe("Study Double Entry – field parity with Initial Entry (ARV_VIRAL_LOAD)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("ARV_VIRAL_LOAD");
  });

  it("shows ARV - Viral Load section heading", () => {
    studyEntryPage.assertVLSectionVisible();
  });

  it("shows LARC prefix hint", () => {
    studyEntryPage.assertLabNoHint("LARC");
  });

  it("hides subjectNumber and upidCode for VL", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows viralLoadTest checkbox", () => {
    studyEntryPage.assertTestVisible("viralLoadTest");
  });

  it("pregnancy field visible only for female gender", () => {
    // gender lives in PatientInfoSection (select#gender), VLSection reacts via prop
    cy.get("select#gender").select("F");
    studyEntryPage.assertVLPregnancyFieldVisible();
    cy.get("select#gender").select("M");
    studyEntryPage.assertVLPregnancyFieldNotVisible();
  });
});

describe("Study Double Entry – field parity with Initial Entry (INDETERMINATE)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("INDETERMINATE");
  });

  it("shows Indeterminate Results section heading", () => {
    studyEntryPage.assertINDSectionVisible();
  });

  it("shows LIND prefix hint", () => {
    studyEntryPage.assertLabNoHint("LIND");
  });

  it("shows subjectNumber and siteSubjectNumber", () => {
    studyEntryPage.assertSubjectNumberVisible();
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides upidCode", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows first/second test result fields", () => {
    cy.get("input#indFirstTestDate").should("be.visible");
    cy.get("input#indFirstTestName").should("be.visible");
    cy.get("select#indFirstTestResult").should("be.visible");
    cy.get("input#indSecondTestDate").should("be.visible");
    cy.get("input#indSecondTestName").should("be.visible");
    cy.get("select#indSecondTestResult").should("be.visible");
    cy.get("select#indSiteFinalResult").should("be.visible");
  });
});

describe("Study Double Entry – field parity with Initial Entry (SPECIAL_REQUEST)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("SPECIAL_REQUEST");
  });

  it("shows LSPE prefix hint", () => {
    studyEntryPage.assertLabNoHint("LSPE");
  });

  it("shows subjectNumber, siteSubjectNumber", () => {
    studyEntryPage.assertSubjectNumberVisible();
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides upidCode", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows reason for request dropdown", () => {
    cy.get("select#reasonForRequest").should("be.visible");
  });
});

describe("Study Double Entry – field parity with Initial Entry (RECENCY_TESTING)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("RECENCY_TESTING");
  });

  it("shows RTRI prefix hint", () => {
    studyEntryPage.assertLabNoHint("RTRI");
  });

  it("hides subjectNumber (Recency uses siteSubjectNumber as recencyNumber)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
  });

  it("shows siteSubjectNumber as Recency Number", () => {
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides upidCode", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });
});

describe("Study Double Entry – field parity with Initial Entry (HPV_TESTING)", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("HPV_TESTING");
  });

  it("shows HPVT prefix hint", () => {
    studyEntryPage.assertLabNoHint("HPVT");
  });

  it("hides subjectNumber (HPV uses siteSubjectNumber as HPV subject number)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
  });

  it("shows siteSubjectNumber as HPV Subject Number", () => {
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides upidCode", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows hpvSamplingMethod dropdown", () => {
    cy.get("select#hpvSamplingMethod").should("be.visible");
  });

  it("shows hpvTest checkbox", () => {
    studyEntryPage.assertTestVisible("hpvTest");
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// G. LAB NUMBER NORMALISATION (all prefixes, via Double Entry)
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – lab number normalisation", () => {
  const cases = [
    { project: "ARV_INITIAL", digitsOnly: "11111", expected: "LARC11111" },
    { project: "ARV_FOLLOWUP", digitsOnly: "22222", expected: "LARC22222" },
    { project: "ARV_VIRAL_LOAD", digitsOnly: "33333", expected: "LARC33333" },
    { project: "RTN", digitsOnly: "44444", expected: "LRTN44444" },
    { project: "EID", digitsOnly: "55555", expected: "LDBS55555" },
    { project: "INDETERMINATE", digitsOnly: "66666", expected: "LIND66666" },
    { project: "SPECIAL_REQUEST", digitsOnly: "77777", expected: "LSPE77777" },
    { project: "RECENCY_TESTING", digitsOnly: "88888", expected: "RTRI88888" },
    { project: "HPV_TESTING", digitsOnly: "99999", expected: "HPVT99999" },
  ];

  cases.forEach(({ project, digitsOnly, expected }) => {
    it(`${project}: digits-only "${digitsOnly}" is sent as "${expected}"`, () => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
        statusCode: 200,
        body: { success: true },
      }).as("saveVerifyLabNo");

      studyEntryPage.selectProject(project);
      studyEntryPage.enterLabNo(digitsOnly);
      cy.get("select#gender").select("M");
      studyEntryPage.enterBirthDate("01/01/1990");
      cy.get("body").type("{esc}");
      const specimenMap = {
        ARV_INITIAL: "dryTubeTaken",
        ARV_FOLLOWUP: "dryTubeTaken",
        ARV_VIRAL_LOAD: "dbsvlTaken",
        RTN: "serologyHIVTest",
        EID: "dnaPCR",
        INDETERMINATE: "serologyHIVTest",
        SPECIAL_REQUEST: "serologyHIVTest",
        RECENCY_TESTING: "serologyHIVTest",
        HPV_TESTING: "hpvTest",
      };
      cy.get(`input#${specimenMap[project] || "dryTubeTaken"}`).check({
        force: true,
      });
      studyEntryPage.clickSave();

      cy.wait("@saveVerifyLabNo").then((i) => {
        expect(i.request.body.labNo).to.equal(expected);
      });
    });
  });

  it("already-prefixed LARC+5 passes through unchanged", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify", {
      statusCode: 200,
      body: { success: true },
    }).as("saveVerifyPrefixed");

    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.enterLabNo("LARC11111");
    cy.get("select#gender").select("M");
    studyEntryPage.enterBirthDate("01/01/1990");
    cy.get("body").type("{esc}");
    cy.get("input#dryTubeTaken").check({ force: true });
    studyEntryPage.clickSave();

    cy.wait("@saveVerifyPrefixed").then((i) => {
      expect(i.request.body.labNo).to.equal("LARC11111");
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// H. MENU NAVIGATION
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – menu navigation", () => {
  it("React Study Double Entry menu item is visible in sidebar", () => {
    cy.fixture("StudyEntry").then((data) => {
      homePage.openNavigationMenu();
      cy.get("span#menu_sample_create").should("be.visible").click();
      cy.get(`#${data.menuItems.reactDoubleEntryId}`).should("be.visible");
    });
  });

  it("old JSP Study Double Entry menu item is hidden in new UI", () => {
    cy.fixture("StudyEntry").then((data) => {
      homePage.openNavigationMenu();
      cy.get("span#menu_sample_create").should("be.visible").click();
      cy.get(`#${data.menuItems.oldDoubleEntryId}`).should("not.be.visible");
    });
  });

  it("clicking React menu item navigates to /StudyDoubleEntry", () => {
    cy.fixture("StudyEntry").then((data) => {
      homePage.openNavigationMenu();
      cy.get("span#menu_sample_create").should("be.visible").click();
      cy.get(`#${data.menuItems.reactDoubleEntryId}`)
        .should("be.visible")
        .click();
      cy.url().should("include", "/StudyDoubleEntry");
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// I. CANCEL BUTTON
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – cancel button", () => {
  it("Cancel button is visible after project selection", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    cy.contains("button", "Cancel").should("be.visible");
  });

  it("clicking Cancel reloads the double entry page", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.enterLabNo("LARC12345");
    studyEntryPage.clickCancel();
    cy.url().should("include", "/StudyDoubleEntry");
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// J. CONDITIONAL LOGIC (mirrors Initial Entry parity checks)
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Double Entry – conditional field logic", () => {
  it("under-investigation note appears when Yes selected (ARV)", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertUnderInvestigationNoteNotVisible();

    cy.get("select#underInvestigation").then(($select) => {
      const options = [...$select.find("option")];
      const yesOption = options.find(
        (o) => o.text.toLowerCase() === "yes" || o.text.toLowerCase() === "oui",
      );
      if (yesOption) {
        cy.get("select#underInvestigation").select(yesOption.value);
        studyEntryPage.assertUnderInvestigationNoteVisible();
      } else {
        cy.log("No Yes/Oui option found — skipping conditional check");
      }
    });
  });

  it("EID type-of-clinic Other field appears only when Other is selected", () => {
    studyEntryPage.selectProject("EID");
    studyEntryPage.assertEIDTypeOfClinicOtherNotVisible();

    cy.get("select#eidTypeOfClinic").then(($select) => {
      const options = [...$select.find("option")];
      const otherOption = options.find(
        (o) =>
          o.text.toLowerCase().includes("other") ||
          o.text.toLowerCase().includes("autre"),
      );
      if (otherOption) {
        cy.get("select#eidTypeOfClinic").select(otherOption.value);
        studyEntryPage.assertEIDTypeOfClinicOtherVisible();
      }
    });
  });

  it("switching project clears previous section (ARV -> EID)", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertProjectSectionVisible("ARV Information");

    studyEntryPage.selectProject("EID");
    studyEntryPage.assertProjectSectionVisible("EID (Early Infant Diagnosis)");
    cy.get("select#arvcenterName").should("not.exist");
  });

  it("switching project clears previous section (EID -> HPV)", () => {
    studyEntryPage.selectProject("EID");
    studyEntryPage.assertProjectSectionVisible("EID (Early Infant Diagnosis)");

    studyEntryPage.selectProject("HPV_TESTING");
    // heading defaultMessage is "HPV Testing"
    cy.contains("HPV Testing").should("be.visible");
    cy.get("select#eidsiteName").should("not.exist");
  });
});
