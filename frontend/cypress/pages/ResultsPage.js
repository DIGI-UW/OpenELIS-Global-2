class Result {
  getResultTitle(title) {
    cy.get("h3").contains(title).should("be.visible");
  }

  selectUnitType(unitType) {
    cy.get("#unitType").select(unitType);
  }

  acceptSample(index = 1) {
    return cy.get("[data-cy='checkTestResult']").eq(index).check();
  }

  acceptResult() {
    cy.get("#cell-accept-0 > .cds--form-item > .cds--checkbox-label").click();
  }

  expandSampleDetails() {
    return cy
      .get("[data-testid='expander-button-1']")
      .should("be.visible")
      .click({ force: true });
  }

  selectTestMethod(index = 1, method) {
    cy.get(`#testMethod${index}`).select(method);
  }

  searchResults() {
    cy.get("#searchResults").click();
  }

  enterCollectionDate() {
    cy.get("#collectionDate").type("24/03/2025");
  }

  enterReceivedDate() {
    cy.get("#recievedDate").type("26/03/2025");
  }

  validatePatientResult(patient) {
    cy.get("tbody > :nth-child(1) > :nth-child(2)").should(
      "contain.text",
      patient.lastName,
    );
    cy.get("tbody > :nth-child(1) > :nth-child(3)").should(
      "contain.text",
      patient.firstName,
    );
  }

  referSample(index = 0, reason, institute) {
    cy.get(`#referralReason${index}`).select(reason);
    cy.get(`#institute${index}`).select(institute);
  }

  referTests() {
    cy.contains("span", "Refer test to a reference lab")
      .should("be.visible")
      .click();
  }
  selectPatientFromSearchResults() {
    cy.get(
      "tbody > :nth-child(1) > :nth-child(1) > .cds--radio-button-wrapper > .cds--radio-button__label > .cds--radio-button__appearance",
    ).click();
  }

  selectRefferedTest() {
    cy.get(
      "tbody > tr > .cds--table-column-checkbox > .cds--checkbox--inline > .cds--checkbox-label",
    ).click();
  }

  sampleStatus(sample) {
    cy.get("#sampleStatusType").select(sample);
  }
  selectAnalysisStatus(status) {
    cy.get("#analysisStatus").select(status);
  }
  selectTestName(testName) {
    cy.get("#testName").select(testName);
  }

  printReport() {
    cy.get(":nth-child(6) > :nth-child(2) > .cds--btn")
      .should("be.visible")
      .click();
  }

  clickReferralsByPatient() {
    cy.get("[data-cy='referralsByPatient']").should("be.visible").click();
  }
  clickReferralsByTestAndName() {
    cy.get("[data-cy='byUnitsAndTests']").should("be.visible").click();
  }

  resultsByLabNumber(number) {
    cy.get("#labNumberInput").type(number, { force: true });
  }
  clickReferralsByLabNumber() {
    cy.get("[data-cy='byLabNumber']")
      .should("be.visible")
      .click({ force: true });
  }

  setResultValue() {
    //(value) ignored for now
    cy.get("#ResultValue0").should("be.visible");
    //.type(value);
  }

  selectResultValue(index = 0, value) {
    cy.get(`#ResultValue${index}`).select(value);
  }
  submitResults() {
    cy.get("#saveResults").should("be.visible").click({ force: true });
  }
}

export default Result;
