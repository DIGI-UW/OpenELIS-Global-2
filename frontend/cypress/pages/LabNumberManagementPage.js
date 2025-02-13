class LabNumberManagementPage {
  constructor() {}

  verifyPageLoaded() {
    // Confirm the page is loaded by checking a unique element
    cy.contains("Lab Number Management").should("be.visible");
  }

  selectLabNumber(labNumberType) {
    // Ensure the dropdown is visible and interactable
    cy.get("#lab_number_type").should("be.visible").select(labNumberType); // Select the lab number type passed as an argument
  }

  checkPrefixCheckBox() {
    cy.get("#usePrefix").check({ force: true }); // Check the checkbox
  }
  typePrefix(prefix) {
    this.checkPrefixCheckBox();

    // Wait for the input to become enabled
    cy.get("#alphanumPrefix").should("not.be.disabled").type(prefix);
  }
  clickSubmitButton() {
    cy.get("button.cds--btn.cds--btn--primary[type='submit']")
      .should("be.visible")
      .click();
  }
}

export default LabNumberManagementPage;
