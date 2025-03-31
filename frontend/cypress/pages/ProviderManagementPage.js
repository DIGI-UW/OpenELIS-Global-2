class ProviderManagementPage {
  constructor() {}

  clickAddProviderButton() {
    cy.get("[data-cy='add-Button']").should("be.visible").click();
    cy.wait(200);
  }

  enterProviderLastName() {
    cy.get("#lastName").type("Prime");
  }
  enterProviderFirstName() {
    cy.get("#firstName").type("Optimus");
  }
  clickActiveDropdown() {
    cy.get("#isActive").contains("Yes").click();
    //cy.get("#downshift-1-toggle-button").select("Yes");
  }

  addProvider() {
    cy.get("div.cds--modal").contains("button", "Add").click();
    cy.wait(200);
  }
}

export default ProviderManagementPage;
