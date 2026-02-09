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

  aggregateReports() {
    this.ensureSidenavMenuExpanded("#menu_reports_aggregate");
  }

  selectStatistics() {
    this.clickNavLink("#menu_reports_aggregate_statistics");
  }

  allReportsSummary() {
    this.clickNavLink("#menu_reports_aggregate_all");
  }

  summaryTestHIV() {
    this.clickNavLink("#menu_reports_aggregate_hiv");
  }

  navigateToManagementReports() {
    this.ensureSidenavMenuExpanded("#menu_reports_management");
  }
  selectRejectionReport() {
    this.clickNavLink("#menu_reports_management_rejection");
  }

  navigateToReportsActivity() {
    this.ensureSidenavMenuExpanded("#menu_reports_activity");
  }
  selectByTestType() {
    this.clickNavLink("#menu_activity_report_test");
  }
  validatePageHeader(expectedText) {
    cy.get("section > h3, h1").should("have.text", expectedText);
  }

  selectByPanel() {
    this.clickNavLink("#menu_activity_report_panel");
  }

  validateFieldVisibility(selector) {
    cy.get(selector, { timeout: 15000 }).should("be.visible");
  }

  selectByUnit() {
    this.clickNavLink("#menu_activity_report_bench");
  }
  selectReferredOutTestReport() {
    this.clickNavLink("#menu_reports_referred");
  }

  navigateToNCReports() {
    this.ensureSidenavMenuExpanded("#menu_reports_nonconformity");
  }

  selectNCReportByUnit() {
    this.clickNavLink("#menu_reports_nonconformity_section");
  }

  selectNCReportByDate() {
    this.clickNavLink("#menu_reports_nonconformity_date");
  }

  navigateToRoutineCSVReport() {
    this.clickNavLink("#menu_reports_export_routine");
  }
  validateButtonDisabled(selector) {
    cy.get(selector).should("be.disabled");
  }

  validateButtonVisible(selector) {
    cy.get(selector).should("be.visible");
  }

  visitRoutineReports() {
    cy.get("[data-cy='sidenav-button-menu_reports_routine']")
      .scrollIntoView()
      .should("be.visible")
      .click();
  }

  selectPatientStatusReport() {
    this.ensureSidenavMenuExpanded("#menu_reports");
    this.ensureSidenavMenuExpanded("#menu_reports_routine");
    this.clickNavLink("#menu_reports_status_patient");
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
