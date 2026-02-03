class OrganizationManagementPage {
  constructor() {
    this.selectors = {
      addButton: "[data-cy='add-button']",
      saveButton: "#saveButton",
      orgName: "#org-name",
      orgPrefix: "#org-prefix",
      isActive: "#is-active",
      parentOrgName: "#parentOrgName",
      orgSearchBar: "#org-name-search-bar",
      orgSearchBarInput:
        "#org-name-search-bar input, input#org-name-search-bar",
      referringClinic: '[id="5:select"]',
      referralLab: '[id="6:select"]',
      orgTableRow: ".cds--data-table tbody",
    };
  }

  clickAddOrganization() {
    cy.get(this.selectors.addButton).should("be.visible").click();
    cy.wait(200);
  }

  addOrgName() {
    cy.get(this.selectors.orgName).should("be.visible").type("CAMES MAN");
    cy.wait(200);
  }

  addInstituteName() {
    cy.get(this.selectors.orgName).should("be.visible").type("CEDRES");
    cy.wait(200);
  }

  activateOrganization() {
    cy.get(this.selectors.isActive).clear().type("Y");
    cy.wait(200);
  }

  addPrefix() {
    cy.get(this.selectors.orgPrefix).should("be.visible").type("279");
    cy.wait(200);
  }

  addInstitutePrefix() {
    cy.get(this.selectors.orgPrefix).should("be.visible").clear().type("");
    cy.wait(200);
  }

  checkReferringClinic() {
    cy.get(this.selectors.referringClinic).check({ force: true });
    cy.wait(200);
  }

  checkReferalLab() {
    cy.get(this.selectors.referralLab).check({ force: true });
    cy.wait(200);
  }

  addParentOrg() {
    cy.get(this.selectors.parentOrgName).should("be.visible").type("CAMESM AN");
    cy.wait(200);
  }

  saveOrganization() {
    cy.get(this.selectors.saveButton).should("be.visible").click();
    cy.wait(3000);
  }

  searchOrganzation() {
    // Carbon Search may put id on wrapper; target the input reliably
    const searchInput = this.selectors.orgSearchBarInput;
    cy.get(searchInput).should("be.visible").scrollIntoView();
    cy.get(searchInput).focus().clear({ force: true });
    cy.get(searchInput).type("CAMES MAN", { force: true });
    // Rely on confirmOrganization's retry; minimal delay for debounce/API
    cy.wait(500);
  }

  searchInstitute() {
    const searchInput = this.selectors.orgSearchBarInput;
    cy.get(searchInput).should("be.visible").scrollIntoView();
    cy.get(searchInput).focus().clear({ force: true });
    cy.get(searchInput).type("CEDRES", { force: true });
    cy.wait(500);
  }

  confirmOrganization() {
    // Wait for search API + re-render (retry up to 10s)
    cy.get(this.selectors.orgTableRow, { timeout: 10000 })
      .contains("CAMES MAN")
      .should("be.visible");
  }

  confirmInstitute() {
    cy.get(this.selectors.orgTableRow, { timeout: 10000 })
      .contains("CEDRES")
      .should("be.visible");
  }
}

export default OrganizationManagementPage;
