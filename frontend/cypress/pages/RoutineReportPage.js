class RoutineReportPage {
  aggregateReports() {
    cy.get("#menu_reports_aggregate").click();
  }

  selectStatistics() {
    cy.get("#menu_reports_aggregate_statistics").click();
  }

  allReportsSummary() {
    cy.get("#menu_reports_aggregate_all_nav").click();
  }

  summaryTestHIV() {
    cy.get("#menu_reports_aggregate_hiv_nav").click();
  }

  navigateToManagementReports() {
    cy.get("#menu_reports_management").click();
  }
  selectRejectionReport() {
    cy.get("#menu_reports_management_rejection_nav").click();
  }

  navigateToReportsActivity() {
    cy.get("#menu_reports_activity").click();
  }
  selectByTestType() {
    cy.get("#menu_activity_report_test").click();
  }
  validatePageHeader(expectedText) {
    cy.get("section > h3, h1").should("have.text", expectedText);
  }

  selectByPanel() {
    cy.get("#menu_activity_report_panel").click();
  }

  validateFieldVisibility(selector) {
    cy.get(selector).should("be.visible");
  }

  selectByUnit() {
    cy.get("#menu_activity_report_bench").click();
  }
  selectReferredOutTestReport() {
    cy.get("#menu_reports_referred").click();
  }

  navigateToNCReports() {
    cy.get("#menu_reports_nonconformity").click();
  }

  selectNCReportByUnit() {
    cy.get("#menu_reports_nonconformity_section").click();
  }

  selectNCReportByDate() {
    cy.get("#menu_reports_nonconformity_date").click();
  }

  navigateToRoutineCSVReport() {
    cy.get("#menu_reports_export_routine").click();
  }
  validateButtonDisabled(selector) {
    cy.get(selector).should("be.disabled");
  }

  validateButtonVisible(selector) {
    cy.get(selector).should("be.visible");
  }

  visitRoutineReports() {
    cy.get("[data-cy='sidenav-button-menu_reports_routine']").click({
      force: true,
    });
  }

  selectPatientStatusReport() {
    cy.get("#menu_reports_status_patient_nav").click();
  }

  toggleAccordion(accordionNumber) {
    cy.get(
      `:nth-child(${accordionNumber})> .cds--accordion__item > .cds--accordion__heading`,
    ).click();
  }

  toggleAccordionPatient(accordionNumber) {
    cy.get(
      `:nth-child(${accordionNumber}) >.cds--accordion > .cds--accordion__item > .cds--accordion__heading`,
    ).click();
  }

  toggleCheckbox(checkboxNumber, containerSelector) {
    cy.get(
      `${containerSelector} > :nth-child(${checkboxNumber}) input[type="checkbox"]`,
    ).click({ force: true });
  }

  checkAllCheckboxes(start, end, containerSelector) {
    for (let i = start; i <= end; i++) {
      this.toggleCheckbox(i, containerSelector);
    }
  }

  validateAllCheckBox(check) {
    cy.get(
      ":nth-child(1)> .cds--sm\\:col-span-4 > :nth-child(2) > :nth-child(1) > .cds--checkbox-label",
    ).should(check);
  }

  uncheckCheckbox(checkboxNumber, containerSelector) {
    this.toggleCheckbox(checkboxNumber, containerSelector);
  }

  selectDropdown(selector, value) {
    cy.get(selector).select(value, { force: true });
  }

  typeInDatePicker(selector, date) {
    cy.get(selector).type(date);
  }
}

export default RoutineReportPage;
