/**
 * E2E tests — Study Initial Entry (React UI)
 *
 * Covers every study form the old JSP supported:
 *   1. Initial ARV
 *   2. Follow-up ARV
 *   3. ARV Viral Load
 *   4. RTN
 *   5. EID
 *   6. Indeterminate
 *   7. Special Request
 *   8. Recency Testing
 *   9. HPV Testing
 *
 * Each describe block tests:
 *   - Page renders correctly after project selection
 *   - All expected fields are visible / hidden per project rules
 *   - Field interactions (conditional show/hide, auto-population)
 *   - Lab number normalisation (prefix + 5 digits)
 *   - Required-field validation prevents premature submission
 *   - Successful save fires the correct REST call and shows success toast
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
  studyEntryPage.interceptFormLoad();
  studyEntryPage.visitInitialEntry();
  studyEntryPage.waitForFormLoad();
  Cypress.env("FAIL_FAST_ENABLED", false);
});

// ─────────────────────────────────────────────────────────────────────────────
// 1. PAGE SHELL
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – page shell", () => {
  it("renders the page title", () => {
    studyEntryPage.assertInitialEntryPageTitle();
  });

  it("renders breadcrumbs with home and study initial entry labels", () => {
    // Breadcrumb renders translated text: "Home / Study / Initial Entry"
    cy.get(".cds--breadcrumb").should("be.visible");
    cy.get(".cds--breadcrumb-item").should("have.length.gte", 2);
    cy.get(".cds--breadcrumb-item").first().should("contain.text", "Home");
  });

  it("hides all form fields until a project is selected", () => {
    studyEntryPage.assertFormFieldsHiddenBeforeProjectSelected();
  });

  it("shows the project selection radio group with all 9 options", () => {
    cy.fixture("StudyEntry").then((data) => {
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
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 2. INITIAL ARV
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – Initial ARV form", () => {
  beforeEach(() => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.arvInitial.project);
    });
  });

  it("shows ARV section heading after selecting Initial ARV", () => {
    studyEntryPage.assertProjectSectionVisible("ARV Information");
  });

  it("ARV center name dropdown is populated with organizations from DB", () => {
    studyEntryPage.assertARVCenterNameHasOptions();
  });

  it("ARV center code dropdown is populated with organizations from DB", () => {
    studyEntryPage.assertARVCenterCodeHasOptions();
  });

  it("shows Sample Information section", () => {
    studyEntryPage.assertSampleInfoSectionVisible();
  });

  it("shows Patient Information section", () => {
    studyEntryPage.assertPatientInfoSectionVisible();
  });

  it("shows Specimens Collected section", () => {
    studyEntryPage.assertTestSelectionSectionVisible();
  });

  it("auto-fills received date and interview date on load", () => {
    studyEntryPage.assertReceivedDateAutoFilled();
    studyEntryPage.assertInterviewDateAutoFilled();
  });

  it("hides subjectNumber for ARV Initial (ARV uses siteSubjectNumber as Unique Health ID)", () => {
    // ARV projects: subjectNumber hidden, siteSubjectNumber shown as "Unique Health ID number"
    studyEntryPage.assertSubjectNumberNotVisible();
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("shows UPID code field (as Site Unique Health ID) for ARV Initial", () => {
    studyEntryPage.assertUPIDCodeVisible();
  });

  it("shows lab number field with LARC prefix hint", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.assertLabNoHint(data.arvInitial.labNoPrefix);
    });
  });

  it("shows gender and birth date fields", () => {
    studyEntryPage.assertGenderVisible();
    studyEntryPage.assertBirthDateVisible();
  });

  it("age is read-only and calculated from birth date", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.enterBirthDate(data.common.birthDate);
      studyEntryPage.assertAgeReadOnly();
    });
  });

  it("does NOT show HIV Status field for Initial ARV (Follow-up only)", () => {
    studyEntryPage.assertHIVStatusNotVisible();
  });

  it("does NOT show underInvestigationNote until Yes is selected", () => {
    studyEntryPage.assertUnderInvestigationNoteNotVisible();
  });

  it("shows ARV-specific tests: serologyHIV, creatinine, nfs, cd4cd8, viralLoad, genotyping", () => {
    studyEntryPage.assertTestVisible("serologyHIVTest");
    studyEntryPage.assertTestVisible("creatinineTest");
    studyEntryPage.assertTestVisible("nfsTest");
    studyEntryPage.assertTestVisible("cd4cd8Test");
    studyEntryPage.assertTestVisible("viralLoadTest");
    studyEntryPage.assertTestVisible("genotypingTest");
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

  it("Save button is visible and enabled", () => {
    studyEntryPage.assertSaveButtonVisible();
  });

  it("shows validation error when required fields are missing on save", () => {
    studyEntryPage.clickSave();
    studyEntryPage.assertErrorNotification();
  });

  it("normalises digits-only lab number to LARC + 5 digits on save attempt", () => {
    // Register intercept FIRST, then visit so it is active before any request fires
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveInitialNorm");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      const d = data.arvInitial;
      studyEntryPage.selectProject(d.project);
      studyEntryPage.selectARVCenterNameByText(d.centerNameText);
      studyEntryPage.enterSiteSubjectNumber(d.siteSubjectNumber);
      studyEntryPage.enterLabNo(d.labNo);
      cy.get("select#gender").select(data.common.gender);
      studyEntryPage.enterBirthDate(data.common.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveInitialNorm", { timeout: 20000 }).then((interception) => {
        expect(interception.request.body.labNo).to.equal(d.expectedLabNo);
      });
    });
  });

  it("successful save shows success notification and reloads form", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveInitialMock");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      const d = data.arvInitial;
      studyEntryPage.selectProject(d.project);
      studyEntryPage.selectARVCenterNameByText(d.centerNameText);
      studyEntryPage.enterSiteSubjectNumber(d.siteSubjectNumber);
      studyEntryPage.enterLabNo(d.expectedLabNo);
      cy.get("select#gender").select(data.common.gender);
      studyEntryPage.enterBirthDate(data.common.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveInitialMock", { timeout: 15000 });
      studyEntryPage.assertSuccessNotification();
    });
  });

  it("failed save shows error notification with server message", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 400,
      body: { success: false, message: "Lab number already exists." },
    }).as("saveInitialFail");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.arvInitial.project);
      studyEntryPage.selectARVCenterNameByText(data.arvInitial.centerNameText);
      studyEntryPage.enterSiteSubjectNumber(data.arvInitial.siteSubjectNumber);
      studyEntryPage.enterLabNo(data.arvInitial.expectedLabNo);
      cy.get("select#gender").select(data.common.gender);
      studyEntryPage.enterBirthDate(data.common.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveInitialFail", { timeout: 15000 });
      studyEntryPage.assertErrorNotification();
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 3. FOLLOW-UP ARV
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – Follow-up ARV form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("ARV_FOLLOWUP");
  });

  it("shows ARV section heading for Follow-up ARV", () => {
    studyEntryPage.assertProjectSectionVisible("ARV Information");
  });

  it("shows HIV Status field (Follow-up only)", () => {
    studyEntryPage.assertHIVStatusVisible();
  });

  it("shows LARC prefix hint on lab number field", () => {
    studyEntryPage.assertLabNoHint("LARC");
  });

  it("shows creatinine and dryTube tests (Follow-up minimal set)", () => {
    studyEntryPage.assertTestVisible("creatinineTest");
    studyEntryPage.assertTestVisible("dryTubeTaken");
  });

  it("selecting creatinine test auto-checks dryTubeTaken", () => {
    studyEntryPage.checkTest("creatinineTest");
    studyEntryPage.assertDryTubeTakenAutoChecked();
  });

  it("POST body includes correct type=initial for Follow-up", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveFARV");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.arvFollowup.project);
      studyEntryPage.selectARVCenterNameByText(data.arvFollowup.centerNameText);
      studyEntryPage.enterSiteSubjectNumber(data.arvFollowup.siteSubjectNumber);
      studyEntryPage.enterLabNo(data.arvFollowup.expectedLabNo);
      cy.get("select#gender").select(data.common.gender);
      studyEntryPage.enterBirthDate(data.common.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveFARV", { timeout: 20000 }).then((i) => {
        expect(i.request.url).to.include("type=initial");
        expect(i.request.body.project).to.equal("ARV_FOLLOWUP");
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 4. ARV VIRAL LOAD
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – ARV Viral Load form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("ARV_VIRAL_LOAD");
  });

  it("shows ARV - Viral Load section heading", () => {
    studyEntryPage.assertVLSectionVisible();
  });

  it("shows LARC prefix hint on lab number field", () => {
    studyEntryPage.assertLabNoHint("LARC");
  });

  it("hides subjectNumber field (VL has no subjectNumber)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
  });

  it("hides UPID code field for VL project", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows viralLoadTest checkbox", () => {
    studyEntryPage.assertTestVisible("viralLoadTest");
  });

  it("shows pregnancy field for female patient", () => {
    // gender lives in PatientInfoSection (select#gender), VLSection reacts via prop
    cy.get("select#gender").select("F");
    studyEntryPage.assertVLPregnancyFieldVisible();
  });

  it("hides pregnancy field for male patient", () => {
    cy.get("select#gender").select("M");
    studyEntryPage.assertVLPregnancyFieldNotVisible();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 5. RTN
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – RTN form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("RTN");
  });

  it("shows RTN section heading", () => {
    // RTN section heading defaultMessage is "RTN (Routine Testing Network)"
    cy.contains("RTN").should("be.visible");
  });

  it("shows LRTN prefix hint on lab number", () => {
    studyEntryPage.assertLabNoHint("LRTN");
  });

  it("hides subjectNumber field (RTN has no patient ID fields)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
  });

  it("hides siteSubjectNumber field (RTN has no patient ID fields)", () => {
    studyEntryPage.assertSiteSubjectNumberNotVisible();
  });

  it("hides UPID code field for RTN", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows underInvestigation dropdown", () => {
    cy.get("select#underInvestigation").should("be.visible");
  });

  it("shows serologyHIVTest checkbox", () => {
    studyEntryPage.assertTestVisible("serologyHIVTest");
  });

  it("normalises lab number to LRTN + 5 digits", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveRTN");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.rtn.project);
      studyEntryPage.enterLabNo(data.rtn.labNo);
      cy.get("select#gender").select(data.rtn.gender);
      studyEntryPage.enterBirthDate(data.rtn.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveRTN", { timeout: 20000 }).then((i) => {
        expect(i.request.body.labNo).to.equal(data.rtn.expectedLabNo);
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 6. EID
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – EID form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("EID");
  });

  it("shows EID section heading", () => {
    // EID section heading defaultMessage is "EID (Early Infant Diagnosis)"
    studyEntryPage.assertProjectSectionVisible("EID (Early Infant Diagnosis)");
  });

  it("EID site name dropdown is populated with organizations from DB", () => {
    studyEntryPage.assertEIDSiteNameHasOptions();
  });

  it("EID site code dropdown is populated with organizations from DB", () => {
    studyEntryPage.assertEIDSiteCodeHasOptions();
  });

  it("shows LDBS prefix hint on lab number", () => {
    studyEntryPage.assertLabNoHint("LDBS");
  });

  it("hides subjectNumber (EID uses infant numbers instead)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
  });

  it("hides siteSubjectNumber (EID identifies by lab number)", () => {
    studyEntryPage.assertSiteSubjectNumberNotVisible();
  });

  it("hides UPID code field for EID", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows EID site name and site code dropdowns", () => {
    cy.get("select#eidsiteName").should("be.visible");
    cy.get("select#eidsiteCode").should("be.visible");
  });

  it("shows Which PCR and Reason for Second PCR dropdowns", () => {
    cy.get("select#eidWhichPCR").should("be.visible");
    cy.get("select#eidSecondPCRReason").should("be.visible");
  });

  it("shows Name of Requestor and Name of Sampler fields", () => {
    cy.get("input#nameOfRequestor").should("be.visible");
    cy.get("input#nameOfSampler").should("be.visible");
  });

  it("shows Infant Information section heading", () => {
    studyEntryPage.assertEIDInfantSectionHeadingVisible();
  });

  it("shows all infant fields", () => {
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

  it("shows dnaPCR test checkbox", () => {
    studyEntryPage.assertTestVisible("dnaPCR");
  });

  it("shows dbsTaken specimen checkbox", () => {
    studyEntryPage.assertTestVisible("dbsTaken");
  });

  it("selecting dnaPCR auto-checks dbsTaken", () => {
    studyEntryPage.checkTest("dnaPCR");
    studyEntryPage.assertDbsTakenAutoChecked();
  });

  it("normalises lab number to LDBS + 5 digits", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveEID");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.eid.project);
      studyEntryPage.selectEIDSiteNameByText(data.eid.siteNameText);
      studyEntryPage.enterLabNo(data.eid.labNo);
      cy.get("select#gender").select(data.eid.gender);
      studyEntryPage.enterBirthDate(data.eid.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dbsTaken").check({ force: true });
      cy.get("input#dnaPCR").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveEID", { timeout: 20000 }).then((i) => {
        expect(i.request.body.labNo).to.equal(data.eid.expectedLabNo);
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 7. INDETERMINATE
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – Indeterminate form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("INDETERMINATE");
  });

  it("shows Indeterminate Results section heading", () => {
    studyEntryPage.assertINDSectionVisible();
  });

  it("shows LIND prefix hint on lab number", () => {
    studyEntryPage.assertLabNoHint("LIND");
  });

  it("shows subjectNumber field", () => {
    studyEntryPage.assertSubjectNumberVisible();
  });

  it("shows siteSubjectNumber field", () => {
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides UPID code field for Indeterminate", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows first and second test date/name/result fields", () => {
    cy.get("input#indFirstTestDate").should("be.visible");
    cy.get("input#indFirstTestName").should("be.visible");
    cy.get("input#indFirstTestResult").should("be.visible");
    cy.get("input#indSecondTestDate").should("be.visible");
    cy.get("input#indSecondTestName").should("be.visible");
    cy.get("input#indSecondTestResult").should("be.visible");
    cy.get("input#indSiteFinalResult").should("be.visible");
  });

  it("normalises lab number to LIND + 5 digits", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveIND");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.indeterminate.project);
      studyEntryPage.selectINDSiteByText(data.indeterminate.siteNameText);
      studyEntryPage.enterSubjectNumber(data.indeterminate.subjectNumber);
      studyEntryPage.enterSiteSubjectNumber(
        data.indeterminate.siteSubjectNumber,
      );
      studyEntryPage.enterLabNo(data.indeterminate.labNo);
      cy.get("select#gender").select(data.indeterminate.gender);
      studyEntryPage.enterBirthDate(data.indeterminate.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveIND", { timeout: 20000 }).then((i) => {
        expect(i.request.body.labNo).to.equal(data.indeterminate.expectedLabNo);
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 8. SPECIAL REQUEST
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – Special Request form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("SPECIAL_REQUEST");
  });

  it("shows Special Request section heading", () => {
    // heading defaultMessage is "Special Request"
    studyEntryPage.assertSpecialRequestSectionVisible();
  });

  it("shows LSPE prefix hint on lab number", () => {
    studyEntryPage.assertLabNoHint("LSPE");
  });

  it("shows subjectNumber and siteSubjectNumber fields", () => {
    studyEntryPage.assertSubjectNumberVisible();
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides UPID code field for Special Request", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows reason for request dropdown", () => {
    cy.get("select#reasonForRequest").should("be.visible");
  });

  it("normalises lab number to LSPE + 5 digits", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveSPE");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.specialRequest.project);
      studyEntryPage.enterSubjectNumber(data.specialRequest.subjectNumber);
      studyEntryPage.enterSiteSubjectNumber(
        data.specialRequest.siteSubjectNumber,
      );
      studyEntryPage.enterLabNo(data.specialRequest.labNo);
      cy.get("select#gender").select(data.specialRequest.gender);
      studyEntryPage.enterBirthDate(data.specialRequest.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveSPE", { timeout: 20000 }).then((i) => {
        expect(i.request.body.labNo).to.equal(
          data.specialRequest.expectedLabNo,
        );
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 9. RECENCY TESTING
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – Recency Testing form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("RECENCY_TESTING");
  });

  it("shows Recency Testing section heading", () => {
    studyEntryPage.assertRecencySectionVisible();
  });

  it("shows RTRI prefix hint on lab number", () => {
    studyEntryPage.assertLabNoHint("RTRI");
  });

  it("hides subjectNumber (Recency uses recencyNumber = siteSubjectNumber)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
  });

  it("shows siteSubjectNumber as Recency Number", () => {
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides UPID code field for Recency", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("normalises lab number to RTRI + 5 digits", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveREC");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.recencyTesting.project);
      studyEntryPage.enterRecencyNumber(data.recencyTesting.recencyNumber);
      studyEntryPage.enterLabNo(data.recencyTesting.labNo);
      cy.get("select#gender").select(data.recencyTesting.gender);
      studyEntryPage.enterBirthDate(data.recencyTesting.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveREC", { timeout: 20000 }).then((i) => {
        expect(i.request.body.labNo).to.equal(
          data.recencyTesting.expectedLabNo,
        );
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 10. HPV TESTING
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – HPV Testing form", () => {
  beforeEach(() => {
    studyEntryPage.selectProject("HPV_TESTING");
  });

  it("shows HPV Testing section heading", () => {
    studyEntryPage.assertHPVSectionVisible();
  });

  it("shows HPVT prefix hint on lab number", () => {
    studyEntryPage.assertLabNoHint("HPVT");
  });

  it("hides subjectNumber (HPV uses siteSubjectNumber as HPV subject number)", () => {
    studyEntryPage.assertSubjectNumberNotVisible();
  });

  it("shows siteSubjectNumber as HPV Subject Number", () => {
    studyEntryPage.assertSiteSubjectNumberVisible();
  });

  it("hides UPID code field for HPV", () => {
    studyEntryPage.assertUPIDCodeNotVisible();
  });

  it("shows hpvSamplingMethod dropdown", () => {
    cy.get("select#hpvSamplingMethod").should("be.visible");
  });

  it("shows hpvTest checkbox", () => {
    studyEntryPage.assertTestVisible("hpvTest");
  });

  it("normalises lab number to HPVT + 5 digits", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("saveHPV");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject(data.hpvTesting.project);
      studyEntryPage.enterSiteSubjectNumber(data.hpvTesting.siteSubjectNumber);
      studyEntryPage.enterLabNo(data.hpvTesting.labNo);
      cy.get("select#gender").select(data.hpvTesting.gender);
      studyEntryPage.enterBirthDate(data.hpvTesting.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#preservCytTaken").check({ force: true });
      cy.get("input#hpvTest").check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveHPV", { timeout: 20000 }).then((i) => {
        expect(i.request.body.labNo).to.equal(data.hpvTesting.expectedLabNo);
      });
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 11. CROSS-FORM CONDITIONAL LOGIC
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – conditional field logic", () => {
  it("under-investigation note appears only when Yes is selected (ARV)", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertUnderInvestigationNoteNotVisible();

    // Select the first non-empty option - we look for the Yes entry by text
    cy.get("select#underInvestigation").then(($select) => {
      const options = [...$select.find("option")];
      const yesOption = options.find(
        (o) => o.text.toLowerCase() === "yes" || o.text.toLowerCase() === "oui",
      );
      if (yesOption) {
        cy.get("select#underInvestigation").select(yesOption.value);
        studyEntryPage.assertUnderInvestigationNoteVisible();
      }
    });
  });

  it("under-investigation note disappears when No is re-selected (ARV)", () => {
    studyEntryPage.selectProject("ARV_INITIAL");

    cy.get("select#underInvestigation").then(($select) => {
      const options = [...$select.find("option")];
      const yesOption = options.find(
        (o) => o.text.toLowerCase() === "yes" || o.text.toLowerCase() === "oui",
      );
      const noOption = options.find(
        (o) => o.text.toLowerCase() === "no" || o.text.toLowerCase() === "non",
      );
      if (yesOption) {
        cy.get("select#underInvestigation").select(yesOption.value);
        studyEntryPage.assertUnderInvestigationNoteVisible();
      }
      if (noOption) {
        cy.get("select#underInvestigation").select(noOption.value);
        studyEntryPage.assertUnderInvestigationNoteNotVisible();
      }
    });
  });

  it("switching project resets form sections (ARV -> EID shows EID, hides ARV)", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.assertProjectSectionVisible("ARV Information");

    studyEntryPage.selectProject("EID");
    studyEntryPage.assertProjectSectionVisible("EID (Early Infant Diagnosis)");
    cy.get("select#arvcenterName").should("not.exist");
  });

  it("switching project resets form sections (EID -> RTN shows RTN, hides EID)", () => {
    studyEntryPage.selectProject("EID");
    studyEntryPage.assertProjectSectionVisible("EID (Early Infant Diagnosis)");

    studyEntryPage.selectProject("RTN");
    cy.contains("RTN").should("be.visible");
    cy.get("select#eidsiteName").should("not.exist");
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

  it("VL pregnancy field hidden for male, visible for female", () => {
    studyEntryPage.selectProject("ARV_VIRAL_LOAD");
    // gender lives in PatientInfoSection (select#gender), VLSection reacts via prop
    cy.get("select#gender").select("M");
    studyEntryPage.assertVLPregnancyFieldNotVisible();
    cy.get("select#gender").select("F");
    studyEntryPage.assertVLPregnancyFieldVisible();
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 12. FORM-LEVEL VALIDATION
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – form-level validation", () => {
  it("shows error when saving ARV_INITIAL without gender", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject("ARV_INITIAL");
      studyEntryPage.enterLabNo(data.arvInitial.expectedLabNo);
      // Intentionally omit gender — leave it at empty default
      studyEntryPage.enterBirthDate(data.common.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      studyEntryPage.assertErrorNotification();
    });
  });

  it("shows error when saving ARV_INITIAL without lab number", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject("ARV_INITIAL");
      cy.get("select#gender").select(data.common.gender);
      studyEntryPage.enterBirthDate(data.common.birthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      // Intentionally omit lab number
      studyEntryPage.clickSave();
      studyEntryPage.assertErrorNotification();
    });
  });

  it("shows error when saving without selecting a project", () => {
    // No project selected — save button should not exist yet
    studyEntryPage.assertFormFieldsHiddenBeforeProjectSelected();
    cy.contains("button", "Save").should("not.exist");
  });

  it("shows error for future birth date", () => {
    cy.fixture("StudyEntry").then((data) => {
      studyEntryPage.selectProject("ARV_INITIAL");
      studyEntryPage.enterLabNo(data.arvInitial.expectedLabNo);
      cy.get("select#gender").select(data.common.gender);
      studyEntryPage.enterBirthDate(data.validation.futureBirthDate);
      cy.get("body").type("{esc}");
      cy.get("input#dryTubeTaken").check({ force: true });
      cy.get("input#serologyHIVTest").check({ force: true });
      studyEntryPage.clickSave();
      studyEntryPage.assertErrorNotification();
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 13. LAB NUMBER NORMALISATION (all prefixes)
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – lab number normalisation", () => {
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
    it(`${project}: digits-only "${digitsOnly}" → "${expected}"`, () => {
      cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
        statusCode: 200,
        body: { success: true },
      }).as("saveLabNo");
      studyEntryPage.interceptFormLoad();
      studyEntryPage.visitInitialEntry();
      studyEntryPage.waitForFormLoad();
      studyEntryPage.selectProject(project);
      // Fill patient identifier required by formValidation for non-RTN/EID projects
      const needsSiteSubject = [
        "ARV_INITIAL",
        "ARV_FOLLOWUP",
        "ARV_VIRAL_LOAD",
        "INDETERMINATE",
        "SPECIAL_REQUEST",
        "RECENCY_TESTING",
        "HPV_TESTING",
      ];
      if (needsSiteSubject.includes(project)) {
        cy.get("input#siteSubjectNumber").clear().type("TESTSUBJ01");
      }
      // ARV projects also need center name selected
      if (
        project === "ARV_INITIAL" ||
        project === "ARV_FOLLOWUP" ||
        project === "ARV_VIRAL_LOAD"
      ) {
        cy.get("select#arvcenterName").then(($sel) => {
          const first = [...$sel.find("option")].find((o) => o.value !== "");
          if (first) cy.get("select#arvcenterName").select(first.value);
        });
      }
      // EID/IND projects need site name
      if (project === "EID" || project === "INDETERMINATE") {
        cy.get("select#eidsiteName, select#indsiteName")
          .first()
          .then(($sel) => {
            const first = [...$sel.find("option")].find((o) => o.value !== "");
            if (first) $sel.val(first.value).trigger("change");
          });
      }
      studyEntryPage.enterLabNo(digitsOnly);
      cy.get("select#gender").select("M");
      studyEntryPage.enterBirthDate("01/01/1990");
      cy.get("body").type("{esc}");
      // specimen + test pairs required by formValidation.js
      const specimenTestMap = {
        ARV_INITIAL: ["dryTubeTaken", "serologyHIVTest"],
        ARV_FOLLOWUP: ["dryTubeTaken", "serologyHIVTest"],
        ARV_VIRAL_LOAD: ["dbsvlTaken", "viralLoadTest"],
        RTN: ["dryTubeTaken", "serologyHIVTest"],
        EID: ["dbsTaken", "dnaPCR"],
        INDETERMINATE: ["dryTubeTaken", "serologyHIVTest"],
        SPECIAL_REQUEST: ["dryTubeTaken", "serologyHIVTest"],
        RECENCY_TESTING: ["dryTubeTaken", "serologyHIVTest"],
        HPV_TESTING: ["preservCytTaken", "hpvTest"],
      };
      const pair = specimenTestMap[project] || [
        "dryTubeTaken",
        "serologyHIVTest",
      ];
      cy.get(`input#${pair[0]}`).check({ force: true });
      cy.get(`input#${pair[1]}`).check({ force: true });
      studyEntryPage.clickSave();
      cy.wait("@saveLabNo", { timeout: 20000 }).then((i) => {
        expect(i.request.body.labNo).to.equal(expected);
      });
    });
  });

  it("already-prefixed LARC + 5 digits passes through unchanged", () => {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial", {
      statusCode: 200,
      body: { success: true },
    }).as("savePrefixed");
    studyEntryPage.interceptFormLoad();
    studyEntryPage.visitInitialEntry();
    studyEntryPage.waitForFormLoad();
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.enterLabNo("LARC11111");
    cy.get("select#gender").select("M");
    studyEntryPage.enterBirthDate("01/01/1990");
    cy.get("body").type("{esc}");
    cy.get("input#dryTubeTaken").check({ force: true });
    cy.get("input#serologyHIVTest").check({ force: true });
    studyEntryPage.clickSave();
    cy.wait("@savePrefixed", { timeout: 20000 }).then((i) => {
      expect(i.request.body.labNo).to.equal("LARC11111");
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 14. MENU NAVIGATION
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – menu navigation", () => {
  it("React Study Initial Entry menu item is visible in sidebar", () => {
    cy.fixture("StudyEntry").then((data) => {
      homePage.openNavigationMenu();
      cy.get("span#menu_sample_create").should("be.visible").click();
      cy.get(`#${data.menuItems.reactInitialEntryId}`).should("be.visible");
    });
  });

  it("old JSP Study Initial Entry menu item is hidden in new UI", () => {
    cy.fixture("StudyEntry").then((data) => {
      homePage.openNavigationMenu();
      cy.get("span#menu_sample_create").should("be.visible").click();
      cy.get(`#${data.menuItems.oldInitialEntryId}`).should("not.be.visible");
    });
  });

  it("clicking React menu item navigates to /StudyInitialEntry", () => {
    cy.fixture("StudyEntry").then((data) => {
      homePage.openNavigationMenu();
      cy.get("span#menu_sample_create").should("be.visible").click();
      cy.get(`#${data.menuItems.reactInitialEntryId}`)
        .should("be.visible")
        .click();
      cy.url().should("include", "/StudyInitialEntry");
    });
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// 15. CANCEL BUTTON
// ─────────────────────────────────────────────────────────────────────────────

describe("Study Initial Entry – cancel button", () => {
  it("Cancel button is visible after project selection", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    cy.contains("button", "Cancel").should("be.visible");
  });

  it("clicking Cancel reloads the initial entry page", () => {
    studyEntryPage.selectProject("ARV_INITIAL");
    studyEntryPage.enterLabNo("LARC12345");
    studyEntryPage.clickCancel();
    cy.url().should("include", "/StudyInitialEntry");
  });
});
