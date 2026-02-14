class StudyReportPage {
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
      .scrollIntoView({ behavior: "smooth" })
      .click();
  }

  selectPatientStatusReport() {
    cy.get("#openreports\\.patientreports\\.title_dropdown")
      .scrollIntoView({ behavior: "smooth" })
      .click({
        force: true,
      });
  }

  selectARV() {
    cy.get("#project\\.ARVStudies\\.name_dropdown")
      .scrollIntoView({ behavior: "smooth" })
      .click({ force: true });
  }

  selectVersion1() {
    cy.get("#menu_reports_arv_initial1_nav")
      .scrollIntoView({ behavior: "smooth" })
      .click({ force: true });
  }

  selectVersion2() {
    cy.get("#menu_reports_arv_initial2_nav")
      .scrollIntoView({ behavior: "smooth" })
      .click({ force: true });
  }
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

  selectFollowUpVersion1() {
    cy.get("#menu_reports_arv_followup1_nav")
      .scrollIntoView({ behavior: "smooth" })
      .click({ force: true });
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

  selectFollowUpVersion2() {
    cy.get("#menu_reports_arv_followup2_nav").click({ force: true });
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

  selectVersion3() {
    cy.get("#menu_reports_arv_all_nav").click({ force: true });
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

  visitAuditTrailReport() {
    cy.get("#menu_reports_auditTrail\\.study_nav")
      .scrollIntoView({ behavior: "smooth" })
      .click({ force: true });
  }

  validateAudit() {
    cy.get("section > .cds--btn").click();
    cy.get(":nth-child(8) > :nth-child(2)").should(
      "have.text",
      "Optimus Prime",
    );
  }

  selectEID() {
    cy.get("#project\\.EIDStudy\\.name_dropdown").click({ force: true });
  }

  selectEIDVersion1() {
    cy.get("#menu_reports_eid_version1_nav").click({ force: true });
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

  selectEIDVersion2() {
    cy.get("#menu_reports_eid_version2_nav").click({ force: true });
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

  selectVL() {
    cy.get("#project\\.VLStudy\\.name_dropdown").click({ force: true });
  }

  selectVLVersion() {
    cy.get("#menu_reports_vl_version1_nav").click({ force: true });
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

  selectIndetermenate() {
    cy.get("#project\\.IndeterminateStudy\\.name_dropdown").click({
      force: true,
    });
  }

  selectIndeterminateV1() {
    cy.get("#menu_reports_indeterminate_version1_nav").click({ force: true });
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

  selectIndeterminateV2() {
    cy.get("#menu_reports_indeterminate_version2_nav").click({ force: true });
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

  selectIndetermenateByService() {
    cy.get("#menu_reports_indeterminate_location_nav").click({ force: true });
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

  selectSpecialRequest() {
    cy.get("#menu_reports_special_nav").click({ force: true });
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

  selectCollectedARVPatientReport() {
    cy.get("#menu_reports_patient_collection_nav").click({ force: true });
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

  selectAssociatedPatientReport() {
    cy.get("#menu_reports_patient_associated_nav").click({ force: true });
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

  selectNCReports() {
    cy.get("[data-cy='menu_reports_nonconformity_study']")
      .scrollIntoView({ behavior: "smooth" })
      .click();
  }

  selectNCReportsByDate() {
    cy.get("[data-cy='menu_reports_nonconformity_date_study']").click();
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

  selectNCReportsByUnitAndReason() {
    cy.get("[data-cy='menu_reports_nonconformity_section_study']").click();
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

  selectNCReportsByLabNo() {
    cy.get("[data-cy='menu_reports_nonconformity_Labno']").click();
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

  selectNCReportsByNotification() {
    cy.get("[data-cy='menu_reports_nonconformity_notification_study']").click();
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

  selectNCFollowUp() {
    cy.get(
      "[data-cy='menu_reports_followupRequired_ByLocation_study']",
    ).click();
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

  visitWhonetPage() {
    cy.get("#menu_reports_whonet_export_nav").click({ force: true });
  }
  visitWhonetReport() {
    this.visitWhonetPage();
    this.verifyHeaderText("h1", "Export a CSV File by Date");
    this.verifyButtonDisabled();
    this.typeInDate("#startDate", "01/02/2023");
    this.typeEndDate("#endDate", "02/02/2023");
    this.verifyButtonVisible();
  }
  selectGeneralReport() {
    cy.get("#menu_reports_export_general").click();
  }

  selectExportByDate() {
    cy.get("#menu_reports_export").click();
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
