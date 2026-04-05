class StudyReportPage {
  /**
   * Idempotent expand: only clicks the Carbon SideNavMenu toggle if it is
   * currently collapsed (aria-expanded !== "true"). This avoids the
   * toggle-trap where a second click collapses an already-open menu.
   */
  ensureSidenavMenuExpanded(menuId) {
    cy.get(menuId, { timeout: 15000 })
      .find("button[aria-expanded]")
      .first()
      .then(($btn) => {
        if ($btn.attr("aria-expanded") !== "true") {
          cy.wrap($btn).click();
        }
      });
    // Verify expansion completed (retries until true)
    cy.get(menuId).find('button[aria-expanded="true"]').first().should("exist");
  }

  /**
   * Click a sidenav leaf item's <a> link via native DOM click.
   *
   * Carbon's SideNavMenu wraps children in a <span> with an onClick that
   * calls stopPropagation(). Cypress's coordinate-based .click() can
   * hit that blocker span instead of the inner <a>. Using the native
   * HTMLElement.click() dispatches directly from the <a> element,
   * guaranteeing the React onClick handler (handleLabelClick) fires.
   */
  clickNavLink(selector) {
    cy.get(selector, { timeout: 15000 })
      .find("a")
      .first()
      .scrollIntoView()
      .should("exist")
      .then(($a) => {
        $a[0].click();
      });
  }

  visitHomePage() {
    homePage = loginPage.goToHomePage();
  }

  verifyButtonDisabled() {
    cy.contains("button", "Generate Printable Version").should("be.disabled");
  }

  typeInField(selector, value) {
    cy.get(selector).type(value);
  }

  verifyButtonVisible() {
    cy.get("[data-cy='printableVersion']").should("be.visible");
  }

  verifyHeaderText(selector, expectedText) {
    cy.get(selector).contains(expectedText);
  }

  typeInDate(selector, value) {
    cy.get(selector).type(value);
  }

  typeEndDate(selector, value) {
    cy.get(selector).type(value);
  }

  clickAccordionItem(nthChild) {
    cy.get(
      `:nth-child(${nthChild}) >.cds--accordion__item > .cds--accordion__heading`,
    ).click();
  }

  clickAccordionPatient(nthChild) {
    cy.get(
      `:nth-child(${nthChild}) >.cds--accordion > .cds--accordion__item > .cds--accordion__heading`,
    ).click();
  }

  verifyElementVisible(selector) {
    cy.get(selector).should("be.visible");
  }

  selectDropdown(optionId, value) {
    cy.get(`#${optionId}`).select(value);
  }

  visitStudyReports() {
    cy.get("[data-cy='sidenav-button-menu_reports_study']")
      .scrollIntoView()
      .click();
  }

  /**
   * Expand the full parent hierarchy (Reports → Study) so that
   * Study-specific child elements are rendered in the DOM.
   * Carbon's SideNavMenu only renders children when expanded.
   */
  ensureStudyMenuReady() {
    this.ensureSidenavMenuExpanded("#menu_reports");
    this.ensureSidenavMenuExpanded("#menu_reports_study");
  }

  // --- Parent menu expanders (SideNavMenu toggles) ---

  selectPatientStatusReport() {
    this.ensureStudyMenuReady();
    this.ensureSidenavMenuExpanded("#menu_reports_patients");
  }

  selectARV() {
    this.ensureSidenavMenuExpanded("#menu_reports_arv");
  }

  selectEID() {
    this.ensureSidenavMenuExpanded("#menu_reports_eid");
  }

  selectVL() {
    this.ensureSidenavMenuExpanded("#menu_reports_vl");
  }

  selectIndetermenate() {
    this.ensureSidenavMenuExpanded("#menu_reports_indeterminate");
  }

  // --- Leaf nav items (navigate via native DOM click on inner <a>) ---

  selectVersion1() {
    this.clickNavLink("#menu_reports_arv_initial1");
  }

  selectVersion2() {
    this.clickNavLink("#menu_reports_arv_initial2");
  }

  selectFollowUpVersion1() {
    this.clickNavLink("#menu_reports_arv_followup1");
  }

  selectFollowUpVersion2() {
    this.clickNavLink("#menu_reports_arv_followup2");
  }

  selectVersion3() {
    this.clickNavLink("#menu_reports_arv_all");
  }

  selectEIDVersion1() {
    this.clickNavLink("#menu_reports_eid_version1");
  }

  selectEIDVersion2() {
    this.clickNavLink("#menu_reports_eid_version2");
  }

  selectVLVersion() {
    this.clickNavLink("#menu_reports_vl_version1");
  }

  selectIndeterminateV1() {
    this.clickNavLink("#menu_reports_indeterminate_version1");
  }

  selectIndeterminateV2() {
    this.clickNavLink("#menu_reports_indeterminate_version2");
  }

  selectIndetermenateByService() {
    this.clickNavLink("#menu_reports_indeterminate_location");
  }

  selectSpecialRequest() {
    this.clickNavLink("#menu_reports_special");
  }

  selectCollectedARVPatientReport() {
    this.clickNavLink("#menu_reports_patient_collection");
  }

  selectAssociatedPatientReport() {
    this.clickNavLink("#menu_reports_patient_associated");
  }

  visitAuditTrailReport() {
    this.ensureStudyMenuReady();
    this.clickNavLink("#menu_reports_auditTrail\\.study");
  }

  visitWhonetPage() {
    this.ensureSidenavMenuExpanded("#menu_reports");
    this.clickNavLink("#menu_reports_whonet_export");
  }

  // --- NC Reports ---

  selectNCReports() {
    this.ensureStudyMenuReady();
    this.ensureSidenavMenuExpanded("#menu_reports_nonconformity\\.study");
  }

  selectNCReportsByDate() {
    this.clickNavLink("[data-cy='menu_reports_nonconformity_date_study']");
  }

  selectNCReportsByUnitAndReason() {
    this.clickNavLink("[data-cy='menu_reports_nonconformity_section_study']");
  }

  selectNCReportsByLabNo() {
    this.clickNavLink("[data-cy='menu_reports_nonconformity_Labno']");
  }

  selectNCReportsByNotification() {
    this.clickNavLink(
      "[data-cy='menu_reports_nonconformity_notification_study']",
    );
  }

  selectNCFollowUp() {
    this.clickNavLink(
      "[data-cy='menu_reports_followupRequired_ByLocation_study']",
    );
  }

  selectExportByDate() {
    this.ensureStudyMenuReady();
    this.ensureSidenavMenuExpanded("#menu_reports_export");
  }

  selectGeneralReport() {
    this.clickNavLink("#menu_reports_export_general");
  }

  // --- Composite visit methods ---

  visitARVInitialVersion1() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectARV();
    this.selectVersion1();
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  visitARVInitialVersion2() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectARV();
    this.selectVersion2();
    this.verifyHeaderText("h3", "ARV-initial");
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  visitARVFollowUpVersion1() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectARV();
    this.selectFollowUpVersion1();
    this.verifyHeaderText("h3", "ARV-Follow-up");
    this.verifyButtonDisabled();
    // this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  visitARVFollowUpVersion2() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectARV();
    this.selectFollowUpVersion2();
    this.verifyHeaderText("h3", "ARV-Follow-up");
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    // this.verifyButtonVisible();
  }

  visitARVFollowUpVersion3() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectARV();
    this.selectVersion3();
    this.verifyHeaderText("h3", "ARV -->Initial-FollowUp-VL");
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  validateAudit() {
    cy.get("section > .cds--btn").click();
    cy.get(":nth-child(8) > :nth-child(2)").should(
      "have.text",
      "Optimus Prime",
    );
  }

  visitEIDVersion1() {
    this.selectPatientStatusReport();
    this.selectEID();
    this.selectEIDVersion1();
    this.verifyHeaderText("h3", "Diagnostic for children with DBS-PCR");
    this.clickAccordionPatient(2);
    this.verifyElementVisible("#patientId");
    this.verifyElementVisible("#local_search");
    this.clickAccordionPatient(2);
    this.clickAccordionItem(3);
    // this.verifyElementVisible("#from");
    //this.verifyElementVisible("#to");
    this.clickAccordionItem(3);
    this.clickAccordionItem(6);
    this.verifyElementVisible("#siteName");
    this.clickAccordionItem(6);
    cy.get(":nth-child(7) > :nth-child(2) > .cds--btn").should("be.visible");
  }

  visitEIDVersion2() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectEID();
    this.selectEIDVersion2();
    this.verifyHeaderText("h3", "Diagnostic for children with DBS-PCR");
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  visitVLVersion() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectVL();
    this.selectVLVersion();
    this.verifyHeaderText(
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(1) > section > h3",
      "Viral Load",
    );
    this.clickAccordionPatient(2);
    this.verifyElementVisible("#patientId");
    this.verifyElementVisible("#local_search");
    this.clickAccordionPatient(2);
    this.clickAccordionItem(3);
    //this.verifyElementVisible("#from");
    //this.verifyElementVisible("#to");
    this.clickAccordionItem(3);
    this.clickAccordionItem(6);
    this.verifyElementVisible("#siteName");
    this.clickAccordionItem(6);
    cy.get(":nth-child(7) > :nth-child(2) > .cds--btn").should("be.visible");
  }

  visitIntermediateVersion1() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectIndetermenate();
    this.selectIndeterminateV1();
    this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Indeterminate",
    );
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  visitIntermediateVersion2() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectIndetermenate();
    this.selectIndeterminateV2();
    this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Indeterminate",
    );
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  visitIntermediateByService() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectIndetermenate();
    this.selectIndetermenateByService();
    this.verifyHeaderText(
      ".cds--lg\\:col-span-16 > section > h3",
      "Indeterminate",
    );
    this.typeInDate("#startDate", "01/02/2023");
    this.typeEndDate("#endDate", "06/02/2023");
    this.typeInField("#siteName", "CAME");
    this.verifyElementVisible("#siteName");
  }

  visitSpecialRequest() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectSpecialRequest();
    this.verifyHeaderText("h3", "Special Request");
    this.verifyButtonDisabled();
    // this.typeInField("#from", "DEV0124000000000000");
    // this.verifyButtonVisible();
  }

  visitCollectedARVPatientReport() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectCollectedARVPatientReport();
    this.verifyHeaderText("h3", "Collected ARV Patient Report");
    this.verifyButtonDisabled();
    // this.typeInField("#nationalID", "UG-23SLHD7DBD");
    //this.verifyButtonVisible();
  }

  visitAssociatedPatientReport() {
    //this.visitStudyReports();
    this.selectPatientStatusReport();
    this.selectAssociatedPatientReport();
    this.verifyHeaderText(
      ":nth-child(1) > .cds--sm\\:col-span-4 > :nth-child(1) > section > h3",
      "Associated Patient Report",
    );
    this.verifyButtonDisabled();
    //this.typeInField("#nationalID", "UG-23SLHD7DBD");
    //this.verifyButtonVisible();
  }

  visitNonConformityReportByDate() {
    //this.visitStudyReports();
    this.selectNCReports();
    this.selectNCReportsByDate();
    this.verifyHeaderText("h1", "Non-conformity Report By Date");
    this.verifyButtonDisabled();
    this.typeInDate("#startDate", "01/02/2023");
    this.verifyButtonVisible();
  }

  visitNonConformityReportByUnitAndReason() {
    //this.visitStudyReports();
    this.selectNCReports();
    this.selectNCReportsByUnitAndReason();
    this.verifyHeaderText("h1", "Non Conformity Report by Unit and Reason");
    this.verifyButtonDisabled();
    this.typeInDate("#startDate", "01/02/2023");
    this.verifyButtonVisible();
  }

  visitNonConformityReportByLabNo() {
    //this.visitStudyReports();
    this.selectNCReports();
    this.selectNCReportsByLabNo();
    this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "ARV -->Initial-FollowUp-VL",
    );
    this.verifyButtonDisabled();
    //this.typeInField("#from", "DEV0124000000000000");
    //this.verifyButtonVisible();
  }

  visitNonConformityReportByNotification() {
    //this.visitStudyReports();
    this.selectNCReports();
    this.selectNCReportsByNotification();
    this.verifyHeaderText(
      ".cds--sm\\:col-span-4 > section > h3",
      "Non-conformity notification",
    );
    this.verifyButtonVisible();
  }

  visitNonConformityReportFollowUpRequired() {
    //this.visitStudyReports();
    this.selectNCReports();
    this.selectNCFollowUp();
    this.verifyHeaderText("h1", "Follow-up Required");
    this.verifyButtonDisabled();
    this.typeInDate("#startDate", "01/02/2023");
    this.verifyButtonVisible();
  }

  visitWhonetReport() {
    this.visitWhonetPage();
    this.verifyHeaderText("h1", "Export a CSV File by Date");
    this.verifyButtonDisabled();
    this.typeInDate("#startDate", "01/02/2023");
    this.typeEndDate("#endDate", "02/02/2023");
    this.verifyButtonVisible();
  }

  visitGeneralReportInExportByDate() {
    //this.visitStudyReports();
    this.selectExportByDate();
    this.selectGeneralReport();
    this.verifyHeaderText("h1", "Export a CSV File by Date");
    this.selectDropdown("studyType", "Testing");
    this.selectDropdown("dateType", "Order Date");
  }
}

export default StudyReportPage;
