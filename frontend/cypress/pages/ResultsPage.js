class Result {
  constructor() {
    this.selectors = {
      resultTitle: "h3",
      unitType: "#unitType",
      testMethod: "#testMethod0",
      collectionDate: "input#collectionDate",
      receivedDate: "input#recievedDate",
      startDate: "input#startDate",
      endDate: "input#endDate",
      referralReason: "#referralReason0",
      institute: "#institute0",
      sampleStatus: "#sampleStatusType",
      analysisStatus: "#analysisStatus",
      testName: "#testName",
      labNumberInput: "#labNumberInput",
      resultValue: "#ResultValue0",
      testNamesInput: "#testnames-input",
      testNamesItem: "#testnames-item-0-item",
      testUnitsInput: "#testunits-input",
      testUnitsItem: "#testunits-item-0-item",
      resultValueSelect: "#resultValue0",
      searchResults: "#searchResults",
      saveResults: "#saveResults",
      printReport: ":nth-child(6) > :nth-child(2) > .cds--btn",
      selectAllButton: "[data-cy='select-all-button']",
      selectNoneButton: "[data-cy='select-none-button']",
      printReportButton: "[data-cy='print-report']",
      referralsByPatient: "[data-cy='referralsByPatient']",
      referralsByTestAndName: "[data-cy='byUnitsAndTests']",
      referralsByLabNumber: "[data-cy='byLabNumber']",
      expanderButton: "[data-testid='expander-button-0']",
      acceptCheckbox: "#cell-accept-0 > .cds--form-item > .cds--checkbox-label",
      startLabNumber: "[data-cy='startAccession']",
      endLabNumber: "[data-cy='endAccession']",
      testResultCheckbox: "[data-cy='checkTestResult']",
      referredTestCheckbox:
        "tbody > tr > .cds--table-column-checkbox > .cds--checkbox--inline > .cds--checkbox-label",
      dateToggleButton: "#downshift-1-toggle-button",
      dateOption: (index) => `#downshift-1-item-${index}`,
      patientLastNameCell: "tbody > :nth-child(1) > :nth-child(2)",
      patientFirstNameCell: "tbody > :nth-child(1) > :nth-child(3)",
    };
  }

  getResultTitle(title) {
    cy.get(this.selectors.resultTitle).contains(title).should("be.visible");
  }

  selectUnitType(unitType) {
    cy.get(this.selectors.unitType).should("be.visible").select(unitType);
  }

  acceptSample(index = 1) {
    return cy.get(this.selectors.testResultCheckbox).eq(index).check();
  }

  acceptResult() {
    cy.get(this.selectors.acceptCheckbox).click();
  }

  expandSampleDetails() {
    return cy.get(this.selectors.expanderButton).should("be.visible").click();
  }

  selectTestMethod(method) {
    cy.get(this.selectors.testMethod).select(method);
  }

  referTests(referTests) {
    cy.get("[data-cy='referalcheckbox']").check({ force: true });
  }

  searchResults() {
    cy.get(this.selectors.searchResults).should("be.visible").click();
    cy.wait(900);
  }

  enterCollectionDate() {
    cy.get(this.selectors.collectionDate)
      .should("be.visible")
      .type("30/04/2025");
  }

  enterReceivedDate() {
    cy.get(this.selectors.receivedDate).should("be.visible").type("01/05/2025");
  }

  clickReceivedDate() {
    cy.get(this.selectors.receivedDate).should("be.visible").click();
  }

  validatePatientResult(lName, fName) {
    cy.get(this.selectors.patientLastNameCell).should("contain.text", lName);
    cy.get(this.selectors.patientFirstNameCell).should("contain.text", fName);
  }

  selectSentDate() {
    cy.get(this.selectors.dateToggleButton).click();
    cy.get(this.selectors.dateOption(0)).click();
  }

  selectResultDate() {
    cy.get(this.selectors.dateToggleButton).click();
    cy.get(this.selectors.dateOption(1)).click();
  }

  clickDateButton() {
    cy.get(this.selectors.dateToggleButton).click();
  }
  startLabNumber(value) {
    cy.get(this.selectors.startLabNumber).type(value);
  }

  endLabNo(value) {
    cy.get(this.selectors.endLabNumber).type(value);
  }
  startDate(startDate) {
    cy.get(this.selectors.startDate).type(startDate);
  }

  endDate(endDate) {
    cy.get(this.selectors.endDate).type(endDate);
  }

  selectAllButtonEnabled() {
    cy.get(this.selectors.selectAllButton)
      .should("be.visible")
      .and("be.enabled");
  }

  clickSelectAllButton() {
    cy.get(this.selectors.selectAllButton).click();
  }

  printReportsButtonEnabled() {
    cy.get(this.selectors.printReportButton)
      .should("be.visible")
      .and("be.enabled");
  }

  selectNoneButtonEnabled() {
    cy.get(this.selectors.selectNoneButton)
      .should("be.visible")
      .and("be.enabled");
  }

  referralReason(reason) {
    cy.get(this.selectors.referralReason).select(reason);
  }

  selectInstitute(institute) {
    cy.get(this.selectors.institute).select(institute);
  }

  getPatientSearchResultsTable() {
    return cy.getElement(
      ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody",
    );
  }

  selectPatientFromSearchResults() {
    this.getPatientSearchResultsTable()
      .find("tr")
      .first()
      .find("td:nth-child(1)")
      .find("[data-cy='radioButton']")
      .click({ force: true });
  }

  selectRefferedTest() {
    cy.get(this.selectors.referredTestCheckbox).click();
  }

  sampleStatus(sample) {
    cy.get(this.selectors.sampleStatus).should("be.visible").select(sample);
  }

  selectAnalysisStatus(status) {
    cy.get(this.selectors.analysisStatus).should("be.visible").select(status);
  }

  selectTestName(testName) {
    cy.get(this.selectors.testName).should("be.visible").select(testName);
  }

  printReport() {
    cy.get(this.selectors.printReport).should("be.visible").click();
  }

  clickReferralsByPatient() {
    cy.get(this.selectors.referralsByPatient).should("be.visible").click();
  }

  clickReferralsByTestAndName() {
    cy.get(this.selectors.referralsByTestAndName).should("be.visible").click();
  }

  resultsByLabNumber(number) {
    cy.get(this.selectors.labNumberInput).should("be.visible").type(number);
  }

  clickReferralsByLabNumber() {
    cy.get(this.selectors.referralsByLabNumber).should("be.visible").click();
  }

  setResultValue(value) {
    cy.get(this.selectors.resultValue).should("be.visible").type(value);
  }

  testName(value) {
    cy.get(this.selectors.testNamesInput).type(value);
  }

  testNameItem() {
    cy.get(this.selectors.testNamesItem).click();
  }

  unitType(value) {
    cy.get(this.selectors.testUnitsInput).type(value);
  }

  unitTypeItem() {
    cy.get(this.selectors.testUnitsItem).click();
  }

  selectResultValue(value) {
    cy.get(this.selectors.resultValueSelect).select(value);
  }

  submitResults() {
    cy.get(this.selectors.saveResults)
      .should("be.visible")
      .click({ force: true });
  }
}

export default Result;
