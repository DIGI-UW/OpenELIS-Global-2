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
      referringClinic: '[id="5:select"]',
      referralLab: '[id="6:select"]',
      orgTableRowOne:
        "div > div.cds--data-table-container > div > table > tbody > tr:nth-child(1)",
      orgTableRow: ".cds--data-table > tbody:nth-child(2)",
    };
  }

  clickAddOrganization() {
    cy.get(this.selectors.addButton, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    // Wait for form to be ready after clicking add
    cy.get(this.selectors.orgName, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled");
  }

  addOrgName() {
    cy.get(this.selectors.orgName, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .type("CAMES MAN");
  }

  addInstituteName() {
    cy.get(this.selectors.orgName, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .type("CEDRES");
  }

  activateOrganization() {
    cy.get(this.selectors.isActive, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .clear()
      .type("Y");
  }

  addPrefix() {
    cy.get(this.selectors.orgPrefix, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .type("279");
  }

  addInstitutePrefix() {
    cy.get(this.selectors.orgPrefix, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .clear()
      .type("");
  }

  checkReferringClinic() {
    cy.get(this.selectors.referringClinic, { timeout: 5000 })
      .should("be.visible")
      .check({ force: true });
  }

  checkReferalLab() {
    cy.get(this.selectors.referralLab, { timeout: 5000 })
      .should("be.visible")
      .check({ force: true });
  }

  addParentOrg() {
    cy.get(this.selectors.parentOrgName, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .type("CAMESM AN");
  }

  saveOrganization() {
    cy.get(this.selectors.saveButton, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .click();
  }

  searchOrganzation() {
    cy.get(this.selectors.orgSearchBar, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .type("CAMES MAN");
  }

  searchInstitute() {
    cy.get(this.selectors.orgSearchBar, { timeout: 5000 })
      .should("be.visible")
      .should("not.be.disabled")
      .type("CEDRES");
  }

  confirmOrganization() {
    cy.get(this.selectors.orgTableRowOne)
      .contains("CAMES MAN")
      .should("be.visible");
  }

  confirmInstitute() {
    cy.get(this.selectors.orgTableRow).contains("CEDRES").should("be.visible");
  }
}

export default OrganizationManagementPage;
