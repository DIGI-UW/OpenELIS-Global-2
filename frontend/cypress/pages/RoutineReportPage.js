class RoutineReportPage {
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

  aggregateReports() {
    this.ensureSidenavMenuExpanded("#menu_reports_aggregate");
  }

  selectStatistics() {
    cy.get("#menu_reports_aggregate_statistics", { timeout: 15000 })
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  allReportsSummary() {
    cy.get("#menu_reports_aggregate_all_nav", { timeout: 15000 })
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  summaryTestHIV() {
    cy.get("#menu_reports_aggregate_hiv_nav")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  navigateToManagementReports() {
    this.ensureSidenavMenuExpanded("#menu_reports_management");
  }
  selectRejectionReport() {
    cy.get("#menu_reports_management_rejection_nav", { timeout: 15000 })
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  navigateToReportsActivity() {
    this.ensureSidenavMenuExpanded("#menu_reports_activity");
  }
  selectByTestType() {
    cy.get("#menu_activity_report_test")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }
  validatePageHeader(expectedText) {
    cy.get("section > h3, h1").should("have.text", expectedText);
  }

  selectByPanel() {
    cy.get("#menu_activity_report_panel")
      .scrollIntoView({ behavior: "smooth" })
      .click();
  }

  validateFieldVisibility(selector) {
    cy.get(selector, { timeout: 15000 }).should("be.visible");
  }

  selectByUnit() {
    cy.get("#menu_activity_report_bench")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }
  selectReferredOutTestReport() {
    cy.get("#menu_reports_referred")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  navigateToNCReports() {
    this.ensureSidenavMenuExpanded("#menu_reports_nonconformity");
  }

  selectNCReportByUnit() {
    cy.get("#menu_reports_nonconformity_section")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  selectNCReportByDate() {
    cy.get("#menu_reports_nonconformity_date")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  navigateToRoutineCSVReport() {
    cy.get("#menu_reports_export_routine")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }
  validateButtonDisabled(selector) {
    cy.get(selector).should("be.disabled");
  }

  validateButtonVisible(selector) {
    cy.get(selector).should("be.visible");
  }

  visitRoutineReports() {
    cy.get("[data-cy='sidenav-button-menu_reports_routine']")
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
  }

  selectPatientStatusReport() {
    this.ensureSidenavMenuExpanded("#menu_reports");
    this.ensureSidenavMenuExpanded("#menu_reports_routine");
    cy.get("#menu_reports_status_patient", { timeout: 15000 })
      .scrollIntoView({ behavior: "smooth" })
      .should("be.visible")
      .click();
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
    cy.get(selector, { timeout: 20000 }).select(value, { force: true });
  }

  selectDropdownExt() {
    cy.get(".cds--select-input").should("be.visible");
    cy.contains(".cds--select-option").select("CEDRES");
  }

  typeInDatePicker(selector, date) {
    cy.get(selector).type(date);
  }
}

export default RoutineReportPage;
