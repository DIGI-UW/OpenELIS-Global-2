class Result {
  getResultTitle() {
    return cy.get("section > h3");
  }

  selectUnitType(unitType) {
    cy.get("#unitType").select(unitType);
  }

  clickExpanderBtn() {
    cy.get('[data-testid="expander-button-0"]').click();
  }
  acceptSample() {
    //div.cds--form-item.cds--checkbox-wrapper > input[type="checkbox"]
    cy.get(
      'div.cds--form-item.cds--checkbox-wrapper > input[type="checkbox"]',
    ).check({ force: true });
  }

  acceptResult() {
    cy.get("#cell-accept-0 > .cds--form-item > .cds--checkbox-label").click();
  }

  selectTestMethod(index = 0, method) {
    cy.get(`#testMethod${index}`).select(method);
  }

  selectPatient() {
    cy.get(
      "tbody > :nth-child(1) > :nth-child(1) > .cds--radio-button-wrapper > .cds--radio-button__label > .cds--radio-button__appearance",
    ).click();
  }

  search() {
    cy.get(":nth-child(1) > :nth-child(5) > .cds--btn").click();
  }

  searchByTest() {
    cy.get(":nth-child(8) > #submit").click();
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

  selectRefferedTest() {
    cy.get(
      "tbody > tr > .cds--table-column-checkbox > .cds--checkbox--inline > .cds--checkbox-label",
    ).click();
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

  setResultValue(index = 0, value) {
    cy.get(`#resultValue${index}`).select(value);
  }

  submitResults() {
    cy.get("#submit").click();
  }
}

export default Result;
