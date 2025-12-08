class ModifyOrderPage {
  constructor() {}

  visit() {
    cy.visit("/FindOrder");
  }

  enterAccessionNo(accessionNo) {
    cy.enterText("#labNumber", accessionNo);
  }
  clickSubmitButton() {
    cy.getElement("[data-cy='submit-order']")
      .should("be.visible")
      .click({ force: true });
  }
  clickSubmitAccessionButton() {
    cy.get("[data-cy='submit-button']").should("be.visible").click();
  }

  clickNextButton() {
    return cy.get("[data-cy='next-button']").should("be.visible").click();
  }

  selectSerumSample() {
    cy.get("#sampleId_0").select("Serum");
  }

  checkRemeberSiteAndRequester() {
    cy.contains("span", "Remember site and requester").click();
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
    return cy.get("[data-cy='printBarCode']").should("be.visible");
  }
  clickSearchPatientButton() {
    return cy.get("[data-cy='searchPatientButton']").click();
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
