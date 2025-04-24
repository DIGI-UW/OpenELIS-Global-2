class OrganizationManagementPage {
  constructor() {}

  clickAddOrganization() {
    cy.get("[data-cy='add-button']").should("be.visible").click();
  }

  addOrgName() {
    cy.get("#org-name").should("be.visible").type("CAMES MAN");
  }

  activateOrganization() {
    cy.get("#is-active").clear().type("Y");
  }

  addPrefix() {
    cy.get("#org-prefix").should("be.visible").type("279");
  }

  checkReferringClinic() {
    cy.get('[id="5:select"]').check({ force: true });
    //cy.contains(".cds--data-table", "referring clinic").click();
  }

  addParentOrg() {
    cy.get("#parentOrgName").should("be.visible").type("CAMESM AN");
  }

  saveOrganization() {
    cy.get("#saveButton").should("be.visible").click();
  }

  searchOrganzation() {
    cy.get("#org-name-search-bar").should("be.visible").type("CAMES MAN");
  }

  confirmOrganization() {
    cy.contains(".cds--data-table > tbody:nth-child(2)", "CAMES MAN").should(
      "be.visible",
    );
  }
}

export default OrganizationManagementPage;
