class ModifyOrderPage {
  constructor() {}

  visit() {
    cy.visit("/FindOrder");
  }

  enterAccessionNo(accessionNo) {
    cy.enterText("#labNumber", accessionNo);
  }

  clickSubmitButton() {
    cy.get("#submitOrderButton").should("be.visible").click();
  }

  clickNextButton() {
    cy.get('button[type="button"].forwardButton.cds--btn.cds--btn--primary')
      .should("be.visible")
      .click();
  }

  generateLabOrderNumber(){
    //cy.get("#generate").should("have.class", "cds--link").click();
    cy.get("#generate", { timeout: 10000 }).should("be.visible").click();

  }

  selectSerum(){
    cy.get("#sampleId_0").select("Serum");
  }
  clickRejectSample() {
    cy.get("#reject_0").check({force: true} );
  }

  rejectReason() {
    cy.get("#rejectedReasonId_0").select("Other.");
  }

  checkProgramButton() {
    return cy.get("#additionalQuestionsSelect").should("be.disabled");
  }

  checkPatientEmail(){
    cy.get("#patientEmail_0_1", {timeout:10000}).should("be.visible").check({force:true});
  }

  checkRequesterSms(){
    cy.get("#providerSMS_0_1", {timeout:10000}).check({fore:true});
  }
  assignValues() {
    cy.get(
      ":nth-child(1) > :nth-child(4) > .cds--form-item > .cds--checkbox-label",
    ).click();
  }

  clickPrintBarcodeButton() {
    return cy.get(".orderEntrySuccessMsg > :nth-child(3) > .cds--btn").click();
  }
  clickSearchPatientButton() {
    return cy.get(":nth-child(12) > .cds--btn").click({ force: true });
  }

  clickRespectivePatient() {
    return cy
      .get("tbody tr")
      .first()
      .find(".cds--radio-button__appearance")
      .click();
  }
}

export default ModifyOrderPage;
