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

  clickRejectSample() {
    cy.get("#reject_0").check();
  }

  rejectReason() {
    cy.get("#rejectedReasonId_0").select("Other.");
  }

  checkProgramButton() {
    return cy.get("#additionalQuestionsSelect").should("be.disabled");
  }

  assignValues() {
    // Wait for table to be visible first
    cy.get("table").should("be.visible");
    // Then find the checkbox within table cells
    cy.get('table input[type="checkbox"][name="add"]')
      .first()
      .click({ force: true });
  }

  clickPrintBarcodeButton() {
    return cy.get("[data-cy='printBarCode']").click();
  }
  clickSearchPatientButton() {
    return cy.get("[data-cy='searchPatientButton']").click({ force: true });
  }

  clickRespectivePatient() {
    // Wait for the table to be visible first
    cy.get("table").should("be.visible");

    // Wait for at least one radio button to be present
    cy.get('input[type="radio"][name="radio-group"]').should("exist");

    // Click the first radio button with a more specific selector
    return cy
      .get("tbody tr")
      .first()
      .within(() => {
        cy.get('input[type="radio"][name="radio-group"]')
          .should("exist")
          .click({ force: true });
      });
  }
}

export default ModifyOrderPage;
