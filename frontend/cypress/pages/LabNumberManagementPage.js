class LabNumberManagementPage {
  constructor() {}

  verifyPageLoaded() {
    cy.contains("h2", "Lab Number Management").should("be.visible");
  }

  selectLabNumber(labNumberType) {
    cy.get("#lab_number_type").should("be.visible").select(labNumberType); // Select the lab number type passed as an argument
  }

  checkPrefixCheckBox() {
    cy.get("#usePrefix").check({ force: true });
  }
  typePrefix(prefix) {
    this.checkPrefixCheckBox();

    // Wait for the input to become enabled
    cy.get("#alphanumPrefix").should("not.be.disabled").type(prefix);
  }
  clickSubmitButton() {
    cy.get("[data-testid='submit-button']").should("be.visible").click();
  }
}

export default LabNumberManagementPage;
