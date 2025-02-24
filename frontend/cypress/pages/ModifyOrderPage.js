class ModifyOrderPage {
  constructor() {}

  visit() {
    cy.visit("/FindOrder");
  }

  enterAccessionNo(accessionNo) {
    cy.enterText("#display_labNumber", accessionNo);
  }

  clickSubmitButton() {
    cy.get("#submit-button").should("be.visible").click({ force: true });
  }

  clickNextButton() {
    cy.get("#next-button").click();
  }

  generateLabOrderNumber() {
    cy.get("#generate", { timeout: 5000 }).should("be.visible").click();
  }

  selectSerum() {
    cy.get("#sampleId_0").select("Serum");
  }
  clickRejectSample() {
    cy.get("#reject_0").check({ force: true });
  }

  rejectReason() {
    cy.get("#rejectedReasonId_0").select("Other.");
  }

  checkProgramButton() {
    return cy.get("#additionalQuestionsSelect").should("be.disabled");
  }

  checkPatientEmail() {
    cy.get("#patientEmail_0_1", { timeout: 10000 })
      .should("be.visible")
      .check({ force: true });
  }

  assignValues() {
    // Wait for table to be visible first
    cy.get("table").should("be.visible");
    // Then find the checkbox within table cells
    cy.get('table input[type="checkbox"][name="add"]')
      .first()
      .click({ force: true });
  }

  barcodeButtonVisibility() {
    cy.get("[data-cy='printBarCode']").should("be.visible");
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
