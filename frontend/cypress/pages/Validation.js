class Validation {
  constructor() {
    this.selectors = {
      pageHeading: "section h2",
      labUnitSelect: "#lab-unit-select",
      searchQueryInput: "#search-query",
      searchButton: 'button[type="submit"]',
      filterButton: 'button:contains("Filters")',
      labNumberFrom: "#lab-number-from",
      labNumberTo: "#lab-number-to",
      dateFrom: "#date-from",
      dateTo: "#date-to",
      testSectionSelect: "#test-section",
      analyzerSelect: "#analyzer",
      enteredBySelect: "#entered-by",
      flaggedCheckbox: "#filter-flagged",
      normalCheckbox: "#filter-normal",
      abnormalCheckbox: "#filter-abnormal",
      resultsTable: 'table[aria-label="validation results"]',
      tableContainer: ".cds--data-table-container",
      selectAllCheckbox: "#select-all",
      retestButton: 'button:contains("Retest")',
      acceptReleaseButton: 'button:contains("Accept")',
      tableRow: ".cds--data-table tbody tr",
      rowCheckbox: 'input[type="checkbox"]',
      retestModal: ".cds--modal",
      retestReasonInput: "#retest-reason",
      retestConfirmButton: ".cds--modal-footer .cds--btn--primary",
      retestCancelButton: ".cds--modal-footer .cds--btn--secondary",
      emptyState: ".validation-empty-state",
      notification: ".cds--inline-notification",
      pagination: ".cds--pagination",
    };
  }

  checkForHeading() {
    cy.get(this.selectors.pageHeading, { timeout: 15000 }).should("be.visible");
  }

  verifyPageTitle(expectedText = "Result Validation") {
    cy.get(this.selectors.pageHeading, { timeout: 15000 }).should(
      "contain.text",
      expectedText,
    );
  }

  selectLabUnit(unitName) {
    cy.get(this.selectors.labUnitSelect).should("be.visible").select(unitName);
  }

  selectLabUnitByIndex(index) {
    cy.get(this.selectors.labUnitSelect)
      .should("be.visible")
      .find("option")
      .eq(index)
      .then(($option) => {
        const value = $option.val();
        if (value) {
          cy.get(this.selectors.labUnitSelect).select(value);
        }
      });
  }

  enterSearchQuery(query) {
    cy.get(this.selectors.searchQueryInput).clear().type(query);
  }

  clickSearch() {
    cy.get(this.selectors.searchButton).click();
  }

  searchByLabNumber(labNumber) {
    this.enterSearchQuery(labNumber);
    this.clickSearch();
  }

  enterLabNumberAndSearch(labNo) {
    this.searchByLabNumber(labNo);
  }

  selectTestUnit(unitType) {
    this.selectLabUnit(unitType);
  }

  toggleAdvancedFilters() {
    cy.get(this.selectors.filterButton).click();
  }

  enterLabNumberRange(from, to) {
    if (from) {
      cy.get(this.selectors.labNumberFrom).clear().type(from);
    }
    if (to) {
      cy.get(this.selectors.labNumberTo).clear().type(to);
    }
  }

  selectFilterOption(filterType, value) {
    const selectorMap = {
      testSection: this.selectors.testSectionSelect,
      analyzer: this.selectors.analyzerSelect,
      enteredBy: this.selectors.enteredBySelect,
    };
    if (selectorMap[filterType]) {
      cy.get(selectorMap[filterType]).select(value);
    }
  }

  toggleQuickFilter(filterType) {
    const selectorMap = {
      flagged: this.selectors.flaggedCheckbox,
      normal: this.selectors.normalCheckbox,
      abnormal: this.selectors.abnormalCheckbox,
    };
    if (selectorMap[filterType]) {
      cy.get(selectorMap[filterType]).click();
    }
  }

  waitForResults() {
    cy.get(this.selectors.tableContainer, { timeout: 30000 }).should(
      "be.visible",
    );
  }

  verifyResultsDisplayed() {
    cy.get(this.selectors.resultsTable).should("be.visible");
    cy.get(this.selectors.tableRow).should("have.length.at.least", 1);
  }

  verifyEmptyState() {
    cy.get(this.selectors.emptyState).should("be.visible");
  }

  selectAllRows() {
    cy.get(this.selectors.selectAllCheckbox).check({ force: true });
  }

  deselectAllRows() {
    cy.get(this.selectors.selectAllCheckbox).uncheck({ force: true });
  }

  selectRowByIndex(index) {
    cy.get(this.selectors.tableRow)
      .eq(index)
      .find(this.selectors.rowCheckbox)
      .first()
      .check({ force: true });
  }

  clickRetest() {
    cy.get(this.selectors.retestButton).click();
  }

  clickAcceptRelease() {
    cy.get(this.selectors.acceptReleaseButton).click();
  }

  saveResults(note) {
    this.selectRowByIndex(0);
    this.clickAcceptRelease();
  }

  verifyRetestModalOpen() {
    cy.get(this.selectors.retestModal).should("be.visible");
  }

  enterRetestReason(reason) {
    cy.get(this.selectors.retestReasonInput).clear().type(reason);
  }

  confirmRetest() {
    cy.get(this.selectors.retestConfirmButton).click();
  }

  cancelRetest() {
    cy.get(this.selectors.retestCancelButton).click();
  }

  submitRetest(reason) {
    this.verifyRetestModalOpen();
    this.enterRetestReason(reason);
    this.confirmRetest();
  }

  verifySuccessNotification() {
    cy.get(".cds--inline-notification--success", { timeout: 10000 }).should(
      "be.visible",
    );
  }

  verifyErrorNotification() {
    cy.get(".cds--inline-notification--error", { timeout: 10000 }).should(
      "be.visible",
    );
  }

  interceptSearchAPI() {
    cy.intercept("GET", "/rest/AccessionValidation*").as("searchResults");
  }

  interceptAcceptAPI() {
    cy.intercept("POST", "/rest/AccessionValidation").as("acceptRequest");
  }

  interceptRetestAPI() {
    cy.intercept("POST", "/rest/AccessionValidation/retest").as(
      "retestRequest",
    );
  }

  waitForSearchResults() {
    cy.wait("@searchResults", { timeout: 30000 });
  }

  waitForAcceptResponse() {
    cy.wait("@acceptRequest", { timeout: 30000 });
  }

  waitForRetestResponse() {
    cy.wait("@retestRequest", { timeout: 30000 });
  }
}

export default Validation;
