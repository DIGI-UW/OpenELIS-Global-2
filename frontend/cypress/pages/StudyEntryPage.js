/**
 * Page Object for Study Initial Entry and Study Double Entry React UI pages.
 *
 * Mirrors the field inventory of sampleAddByProject.jsp and the 9 study forms:
 *   InitialARV, FollowUpARV, RTN, EID, VL (ARV Viral Load),
 *   Indeterminate, Special Request, Recency Testing, HPV Testing
 *
 * All selectors target Carbon Design System components rendered by the
 * React components under frontend/src/components/sampleEntry/.
 */
class StudyEntryPage {
  // ─── Navigation ────────────────────────────────────────────────────────────

  visitInitialEntry() {
    cy.visit("/StudyInitialEntry");
  }

  visitDoubleEntry() {
    cy.visit("/StudyDoubleEntry");
  }

  // ─── Page-level assertions ─────────────────────────────────────────────────

  assertInitialEntryPageTitle() {
    cy.contains("Study - Initial Entry").should("be.visible");
  }

  assertDoubleEntryPageTitle() {
    cy.contains("Study - Double Entry").should("be.visible");
  }

  assertBreadcrumb(label) {
    cy.get(".cds--breadcrumb").contains(label).should("be.visible");
  }

  // ─── Project / Study Form Selection ───────────────────────────────────────

  /**
   * @param {string} projectValue - one of: ARV_INITIAL | ARV_FOLLOWUP | RTN |
   *   EID | INDETERMINATE | SPECIAL_REQUEST | ARV_VIRAL_LOAD |
   *   RECENCY_TESTING | HPV_TESTING
   */
  selectProject(projectValue) {
    cy.get(`#${projectValue}`).check({ force: true });
  }

  assertProjectSelected(projectValue) {
    cy.get(`#${projectValue}`).should("be.checked");
  }

  assertProjectSectionVisible(heading) {
    cy.contains(heading).should("be.visible");
  }

  assertProjectSectionNotVisible(sectionId) {
    cy.get(`#${sectionId}`).should("not.exist");
  }

  assertFormFieldsHiddenBeforeProjectSelected() {
    cy.get("#labNo").should("not.exist");
    cy.get("#gender").should("not.exist");
    cy.get("#receivedDateForDisplay").should("not.exist");
  }

  // ─── ARV Section (Initial & Follow-up ARV) ────────────────────────────────

  selectARVCenterName(value) {
    cy.get("select#arvcenterName").select(value);
  }

  selectARVCenterNameByText(text) {
    cy.get("select#arvcenterName")
      .should("be.visible")
      .then(($select) => {
        const options = [...$select.find("option")];
        const match = options.find(
          (o) => o.text.trim() === text || o.text.trim().includes(text),
        );
        if (match) {
          cy.get("select#arvcenterName").select(match.value);
        } else {
          // fallback: select first non-empty option
          const first = options.find((o) => o.value !== "");
          if (first) cy.get("select#arvcenterName").select(first.value);
        }
      });
  }

  selectARVCenterCode(value) {
    cy.get("select#arvcenterCode").select(value);
  }

  selectARVCenterCodeByText(text) {
    cy.get("select#arvcenterCode")
      .should("be.visible")
      .then(($select) => {
        const options = [...$select.find("option")];
        const match = options.find(
          (o) => o.text.trim() === text || o.text.trim().includes(text),
        );
        if (match) {
          cy.get("select#arvcenterCode").select(match.value);
        } else {
          const first = options.find((o) => o.value !== "");
          if (first) cy.get("select#arvcenterCode").select(first.value);
        }
      });
  }

  enterDoctor(value) {
    cy.get("input#doctor").clear().type(value);
  }

  assertAgeCalculated(expectedAge) {
    cy.get("input#age").should("have.value", expectedAge);
  }

  assertAgeReadOnly() {
    cy.get("input#age").should("have.attr", "readonly");
  }

  selectHIVStatus(value) {
    cy.get("select#hivStatus").select(value);
  }

  assertHIVStatusVisible() {
    cy.get("select#hivStatus").should("be.visible");
  }

  assertHIVStatusNotVisible() {
    cy.get("select#hivStatus").should("not.exist");
  }

  selectUnderInvestigation(value) {
    cy.get("select#underInvestigation").select(value);
  }

  assertUnderInvestigationNoteVisible() {
    cy.get("textarea#underInvestigationNote").should("be.visible");
  }

  assertUnderInvestigationNoteNotVisible() {
    cy.get("textarea#underInvestigationNote").should("not.exist");
  }

  enterUnderInvestigationNote(value) {
    cy.get("textarea#underInvestigationNote").clear().type(value);
  }

  assertARVCenterNameAutoPopulatedFromCode() {
    cy.get("select#arvcenterName").should("not.have.value", "");
  }

  assertARVCenterCodeAutoPopulatedFromName() {
    cy.get("select#arvcenterCode").should("not.have.value", "");
  }

  assertARVCenterNameHasOptions() {
    cy.get("select#arvcenterName option").should("have.length.gt", 1);
  }

  assertARVCenterCodeHasOptions() {
    cy.get("select#arvcenterCode option").should("have.length.gt", 1);
  }

  assertEIDSiteNameHasOptions() {
    cy.get("select#eidsiteName option").should("have.length.gt", 1);
  }

  assertEIDSiteCodeHasOptions() {
    cy.get("select#eidsiteCode option").should("have.length.gt", 1);
  }

  // ─── EID Section ──────────────────────────────────────────────────────────

  selectEIDSiteName(value) {
    cy.get("select#eidsiteName").select(value);
  }

  selectEIDSiteNameByText(text) {
    cy.get("select#eidsiteName")
      .should("be.visible")
      .then(($select) => {
        const options = [...$select.find("option")];
        const match = options.find(
          (o) => o.text.trim() === text || o.text.trim().includes(text),
        );
        if (match) {
          cy.get("select#eidsiteName").select(match.value);
        } else {
          const first = options.find((o) => o.value !== "");
          if (first) cy.get("select#eidsiteName").select(first.value);
        }
      });
  }

  selectEIDSiteCode(value) {
    cy.get("select#eidsiteCode").select(value);
  }

  selectEIDSiteCodeByText(text) {
    cy.get("select#eidsiteCode")
      .should("be.visible")
      .then(($select) => {
        const options = [...$select.find("option")];
        const match = options.find(
          (o) => o.text.trim() === text || o.text.trim().includes(text),
        );
        if (match) {
          cy.get("select#eidsiteCode").select(match.value);
        } else {
          const first = options.find((o) => o.value !== "");
          if (first) cy.get("select#eidsiteCode").select(first.value);
        }
      });
  }

  selectEIDWhichPCR(value) {
    cy.get("select#eidWhichPCR").select(value);
  }

  selectEIDSecondPCRReason(value) {
    cy.get("select#eidSecondPCRReason").select(value);
  }

  enterNameOfRequestor(value) {
    cy.get("input#nameOfRequestor").clear().type(value);
  }

  enterNameOfSampler(value) {
    cy.get("input#nameOfSampler").clear().type(value);
  }

  selectEIDInfantPTME(value) {
    cy.get("select#eidInfantPTME").select(value);
  }

  selectEIDTypeOfClinic(value) {
    cy.get("select#eidTypeOfClinic").select(value);
  }

  assertEIDTypeOfClinicOtherVisible() {
    cy.get("input#eidTypeOfClinicOther").should("be.visible");
  }

  assertEIDTypeOfClinicOtherNotVisible() {
    cy.get("input#eidTypeOfClinicOther").should("not.exist");
  }

  enterEIDTypeOfClinicOther(value) {
    cy.get("input#eidTypeOfClinicOther").clear().type(value);
  }

  selectEIDHowChildFed(value) {
    cy.get("select#eidHowChildFed").select(value);
  }

  selectEIDStoppedBreastfeeding(value) {
    cy.get("select#eidStoppedBreastfeeding").select(value);
  }

  selectEIDInfantSymptomatic(value) {
    cy.get("select#eidInfantSymptomatic").select(value);
  }

  selectEIDInfantsARV(value) {
    cy.get("select#eidInfantsARV").select(value);
  }

  selectEIDInfantCotrimoxazole(value) {
    cy.get("select#eidInfantCotrimoxazole").select(value);
  }

  selectEIDMothersHIVStatus(value) {
    cy.get("select#eidMothersHIVStatus").select(value);
  }

  selectEIDMothersARV(value) {
    cy.get("select#eidMothersARV").select(value);
  }

  assertEIDInfantSectionHeadingVisible() {
    cy.contains("Infant Information").should("be.visible");
  }

  assertEIDMotherSectionHeadingVisible() {
    cy.contains("Mother's Information").should("be.visible");
  }

  // ─── RTN Section ──────────────────────────────────────────────────────────

  assertRTNSectionVisible() {
    cy.contains("RTN").should("be.visible");
  }

  // RTN only exposes underInvestigation / underInvestigationNote (shared selectors above)

  // ─── Indeterminate Section ────────────────────────────────────────────────

  selectINDSite(value) {
    cy.get("select#indsiteName").select(value);
  }

  selectINDSiteByText(text) {
    cy.get("select#indsiteName")
      .should("be.visible")
      .then(($select) => {
        const options = [...$select.find("option")];
        const match = options.find(
          (o) => o.text.trim() === text || o.text.trim().includes(text),
        );
        if (match) {
          cy.get("select#indsiteName").select(match.value);
        } else {
          const first = options.find((o) => o.value !== "");
          if (first) cy.get("select#indsiteName").select(first.value);
        }
      });
  }

  assertINDSectionVisible() {
    cy.contains("Indeterminate Results").should("be.visible");
  }

  enterINDFirstTestDate(value) {
    cy.get("input#indFirstTestDate").clear().type(value);
  }

  enterINDFirstTestName(value) {
    cy.get("input#indFirstTestName").clear().type(value);
  }

  selectINDFirstTestResult(value) {
    cy.get("select#indFirstTestResult").select(value);
  }

  enterINDSecondTestDate(value) {
    cy.get("input#indSecondTestDate").clear().type(value);
  }

  enterINDSecondTestName(value) {
    cy.get("input#indSecondTestName").clear().type(value);
  }

  selectINDSecondTestResult(value) {
    cy.get("select#indSecondTestResult").select(value);
  }

  selectINDSiteFinalResult(value) {
    cy.get("select#indSiteFinalResult").select(value);
  }

  // ─── Special Request Section ───────────────────────────────────────────────

  assertSpecialRequestSectionVisible() {
    // Target the section heading, not the sidebar nav item
    cy.get(".cds--css-grid, .orderLegendBody, form")
      .contains("Special Request")
      .should("be.visible");
  }

  selectSpecialRequestReason(value) {
    cy.get("select#reasonForRequest").select(value);
  }

  // ─── Recency Testing Section ──────────────────────────────────────────────

  assertRecencySectionVisible() {
    cy.contains("Recency Testing").should("be.visible");
  }

  enterRecencyNumber(value) {
    // siteSubjectNumber is used as recencyNumber for RECENCY_TESTING project
    cy.get("input#siteSubjectNumber").clear().type(value);
  }

  // ─── HPV Testing Section ──────────────────────────────────────────────────

  assertHPVSectionVisible() {
    cy.contains("HPV Testing").should("be.visible");
  }

  selectHPVSamplingMethod(value) {
    cy.get("select#hpvSamplingMethod").select(value);
  }

  // ─── VL (ARV Viral Load) Section ─────────────────────────────────────────

  assertVLSectionVisible() {
    cy.contains("ARV - Viral Load").should("be.visible");
  }

  selectVLARVCenterName(value) {
    // VL section uses ARVcenterName (capital A)
    cy.get("select#ARVcenterName").select(value);
  }

  selectVLARVCenterCode(value) {
    cy.get("select#ARVcenterCode").select(value);
  }

  selectVLCurrentARVTreatment(value) {
    cy.get("select#currentARVTreatment").select(value);
  }

  selectVLReasonForRequest(value) {
    cy.get("select#vlReasonForRequest").select(value);
  }

  selectVLPregnancy(value) {
    cy.get("select#vlPregnancy").select(value);
  }

  selectVLSuckle(value) {
    cy.get("select#vlSuckle").select(value);
  }

  /**
   * VL pregnancy/suckle fields render only when gender is female.
   * Gender lives in PatientInfoSection (select#gender).
   * After selecting female gender, React re-renders VLSection via the
   * gender prop, making vlPregnancy visible.
   */
  assertVLPregnancyFieldVisible() {
    cy.get("select#vlPregnancy").should("exist").and("be.visible");
  }

  assertVLPregnancyFieldNotVisible() {
    cy.get("select#vlPregnancy").should("not.exist");
  }

  // ─── Sample Information Section ───────────────────────────────────────────

  assertSampleInfoSectionVisible() {
    cy.contains("Sample Information").should("be.visible");
  }

  enterReceivedDate(value) {
    cy.get("input#receivedDateForDisplay").clear().type(value);
  }

  enterReceivedTime(value) {
    cy.get("input#receivedTimeForDisplay").clear().type(value);
  }

  enterInterviewDate(value) {
    cy.get("input#interviewDate").clear().type(value);
  }

  enterInterviewTime(value) {
    cy.get("input#interviewTime").clear().type(value);
  }

  assertReceivedDateAutoFilled() {
    cy.get("input#receivedDateForDisplay").should("not.have.value", "");
  }

  assertInterviewDateAutoFilled() {
    cy.get("input#interviewDate").should("not.have.value", "");
  }

  // ─── Patient Information Section ──────────────────────────────────────────

  assertPatientInfoSectionVisible() {
    cy.contains("Patient Information").should("be.visible");
  }

  enterSubjectNumber(value) {
    cy.get("input#subjectNumber").clear().type(value);
  }

  assertSubjectNumberVisible() {
    cy.get("input#subjectNumber").should("be.visible");
  }

  assertSubjectNumberNotVisible() {
    cy.get("input#subjectNumber").should("not.exist");
  }

  enterSiteSubjectNumber(value) {
    cy.get("input#siteSubjectNumber").clear().type(value);
  }

  assertSiteSubjectNumberVisible() {
    cy.get("input#siteSubjectNumber").should("be.visible");
  }

  assertSiteSubjectNumberNotVisible() {
    cy.get("input#siteSubjectNumber").should("not.exist");
  }

  enterUPIDCode(value) {
    cy.get("input#upidCode").clear().type(value);
  }

  assertUPIDCodeVisible() {
    cy.get("input#upidCode").should("be.visible");
  }

  assertUPIDCodeNotVisible() {
    cy.get("input#upidCode").should("not.exist");
  }

  enterLabNo(value) {
    cy.get("input#labNo").clear().type(value);
  }

  getLabNoValue() {
    return cy.get("input#labNo").invoke("val");
  }

  assertLabNoVisible() {
    cy.get("input#labNo").should("be.visible");
  }

  assertLabNoHint(prefix) {
    cy.get("input#labNo")
      .closest(".cds--form-item")
      .contains(`${prefix}`)
      .should("be.visible");
  }

  selectGender(value) {
    cy.get("select#gender").select(value);
  }

  assertGenderVisible() {
    cy.get("select#gender").should("be.visible");
  }

  enterBirthDate(value) {
    // CustomDatePicker renders a standard input
    cy.get("input#birthDateForDisplay").clear().type(value);
  }

  assertBirthDateVisible() {
    cy.get("input#birthDateForDisplay").should("be.visible");
  }

  assertPatientAgeCalculated(expectedAge) {
    // The age field in PatientInfoSection (different from ARV section age)
    cy.get("input#age").should("have.value", expectedAge);
  }

  // ─── Test Selection Section ───────────────────────────────────────────────

  assertTestSelectionSectionVisible() {
    cy.contains("Specimens Collected").should("be.visible");
  }

  checkTest(testId) {
    cy.get(`input#${testId}[type="checkbox"]`).check({ force: true });
  }

  uncheckTest(testId) {
    cy.get(`input#${testId}[type="checkbox"]`).uncheck({ force: true });
  }

  assertTestChecked(testId) {
    cy.get(`input#${testId}[type="checkbox"]`).should("be.checked");
  }

  assertTestUnchecked(testId) {
    cy.get(`input#${testId}[type="checkbox"]`).should("not.be.checked");
  }

  assertTestVisible(testId) {
    cy.get(`input#${testId}[type="checkbox"]`).should("be.visible");
  }

  assertTestNotVisible(testId) {
    cy.get(`input#${testId}[type="checkbox"]`).should("not.exist");
  }

  // Specimen type helpers
  assertDryTubeTakenAutoChecked() {
    cy.get('input#dryTubeTaken[type="checkbox"]').should("be.checked");
  }

  assertEdtaTubeTakenAutoChecked() {
    cy.get('input#edtaTubeTaken[type="checkbox"]').should("be.checked");
  }

  assertDbsTakenAutoChecked() {
    cy.get('input#dbsTaken[type="checkbox"]').should("be.checked");
  }

  // ─── Action Buttons ───────────────────────────────────────────────────────

  clickSave() {
    cy.contains("button", "Save").should("not.be.disabled").click();
  }

  assertSaveButtonVisible() {
    cy.contains("button", "Save").should("be.visible");
  }

  assertSaveButtonDisabledWhileSaving() {
    cy.contains("button", "Saving...").should("be.disabled");
  }

  clickCancel() {
    cy.contains("button", "Cancel").click();
  }

  // ─── Notifications ────────────────────────────────────────────────────────

  assertSuccessNotification() {
    cy.get("[role='status'], .cds--toast-notification--success", {
      timeout: 10000,
    }).should("be.visible");
  }

  assertErrorNotification() {
    cy.get(
      "[role='status'], .cds--toast-notification--error, .cds--inline-notification--error",
      { timeout: 8000 },
    ).should("be.visible");
  }

  assertNotificationContains(text) {
    cy.get(
      "[role='status'], .cds--toast-notification, .cds--inline-notification",
      { timeout: 8000 },
    )
      .should("be.visible")
      .and("contain.text", text);
  }

  dismissNotification() {
    cy.get(".cds--toast-notification__close-button").click({ force: true });
  }

  // ─── API intercept helpers ────────────────────────────────────────────────

  interceptInitialSave() {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=initial").as(
      "saveInitial",
    );
  }

  interceptDoubleSave() {
    cy.intercept("POST", "**/rest/SampleEntryByProject?type=verify").as(
      "saveDouble",
    );
  }

  interceptFormLoad() {
    cy.intercept("GET", "**/rest/SampleEntryByProject*").as("loadForm");
  }

  waitForFormLoad() {
    cy.wait("@loadForm", { timeout: 15000 });
  }

  waitForInitialSave() {
    return cy.wait("@saveInitial", { timeout: 20000 });
  }

  waitForDoubleSave() {
    return cy.wait("@saveDouble", { timeout: 20000 });
  }

  assertSaveRequestBody(alias, assertions) {
    cy.get(`@${alias}`).then((interception) => {
      const body = interception.request.body;
      Object.entries(assertions).forEach(([key, value]) => {
        expect(body).to.have.property(key, value);
      });
    });
  }

  assertSaveResponseOk(alias) {
    cy.get(`@${alias}`).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 201]);
      expect(interception.response.body.success).to.equal(true);
    });
  }

  assertSaveResponseConflict(alias) {
    cy.get(`@${alias}`).then((interception) => {
      expect(interception.response.statusCode).to.equal(409);
    });
  }

  // ─── Menu navigation helpers ──────────────────────────────────────────────

  /**
   * Navigate to Study Initial Entry via the React sidebar menu.
   * Depends on hide_in_new_ui flag being set so the React items are visible.
   */
  goToStudyInitialEntryViaMenu(homePage) {
    homePage.openNavigationMenu();
    cy.get("span#menu_sample_create").should("be.visible").click();
    cy.get("#menu_study_initial_entry_react").should("be.visible").click();
  }

  goToStudyDoubleEntryViaMenu(homePage) {
    homePage.openNavigationMenu();
    cy.get("span#menu_sample_create").should("be.visible").click();
    cy.get("#menu_study_double_entry_react").should("be.visible").click();
  }

  assertOldJSPMenuHidden() {
    cy.get("#menu_sample_create_initial").should("not.be.visible");
    cy.get("#menu_sample_create_double").should("not.be.visible");
  }

  // ─── Full-form fill helpers (convenience) ────────────────────────────────

  /**
   * Fill the common fields shared by all projects:
   * Sample Info + Patient Info (labNo, gender, birthDate).
   */
  fillCommonFields({
    receivedDate,
    receivedTime,
    interviewDate,
    interviewTime,
    labNo,
    gender,
    birthDate,
  }) {
    if (receivedDate) this.enterReceivedDate(receivedDate);
    if (receivedTime) this.enterReceivedTime(receivedTime);
    if (interviewDate) this.enterInterviewDate(interviewDate);
    if (interviewTime) this.enterInterviewTime(interviewTime);
    if (labNo) this.enterLabNo(labNo);
    if (gender) this.selectGender(gender);
    if (birthDate) this.enterBirthDate(birthDate);
  }

  /**
   * Fill ARV-specific fields (center name + code + optional doctor).
   */
  fillARVFields({
    centerNameText,
    centerCodeText,
    doctor,
    underInvestigation,
  } = {}) {
    if (centerNameText) this.selectARVCenterNameByText(centerNameText);
    if (centerCodeText) this.selectARVCenterCodeByText(centerCodeText);
    if (doctor) this.enterDoctor(doctor);
    if (underInvestigation) this.selectUnderInvestigation(underInvestigation);
  }

  /**
   * Fill EID-specific fields.
   */
  fillEIDFields({
    siteNameText,
    siteCodeText,
    whichPCR,
    mothersHIVStatus,
    mothersARV,
  } = {}) {
    if (siteNameText) this.selectEIDSiteNameByText(siteNameText);
    if (siteCodeText) this.selectEIDSiteCodeByText(siteCodeText);
    if (whichPCR) this.selectEIDWhichPCR(whichPCR);
    if (mothersHIVStatus) this.selectEIDMothersHIVStatus(mothersHIVStatus);
    if (mothersARV) this.selectEIDMothersARV(mothersARV);
  }

  // ─── Lab-number format assertions ─────────────────────────────────────────

  /**
   * Assert that after saving the lab number was normalised to prefix+5digits.
   * @param {string} prefix - e.g. "LARC", "LDBS", "LRTN"
   */
  assertLabNoNormalisedFormat(prefix) {
    cy.get("input#labNo")
      .invoke("val")
      .should("match", new RegExp(`^${prefix}\\d{5}$`));
  }
}

export default StudyEntryPage;
