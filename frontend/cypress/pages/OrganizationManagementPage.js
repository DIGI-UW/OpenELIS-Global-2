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
      orgTableRow: ".cds--data-table > tbody:nth-child(2)",
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
    // Break up the chain to avoid detached DOM issues
    // First, ensure the element is visible and scroll into view
    cy.get(`input${this.selectors.orgSearchBar}`)
      .should("be.visible")
      .scrollIntoView();

    // Re-query after scroll (page may have updated)
    cy.get(`input${this.selectors.orgSearchBar}`)
      .focus()
      .clear({ force: true });

    // Re-query again before typing
    cy.get(`input${this.selectors.orgSearchBar}`).type("CAMES MAN", {
      force: true,
    });
    cy.wait(200);
  }

  searchInstitute() {
    // Break up the chain to avoid detached DOM issues
    // First, ensure the element is visible and scroll into view
    cy.get(`input${this.selectors.orgSearchBar}`)
      .should("be.visible")
      .scrollIntoView();

    // Re-query after scroll (page may have updated)
    cy.get(`input${this.selectors.orgSearchBar}`)
      .focus()
      .clear({ force: true });

    // Re-query again before typing
    cy.get(`input${this.selectors.orgSearchBar}`).type("CEDRES", {
      force: true,
    });
    cy.wait(200);
  }

  confirmOrganization() {
    cy.get(this.selectors.orgTableRow)
      .contains("CAMES MAN")
      .should("be.visible");
  }

  confirmInstitute() {
    cy.get(this.selectors.orgTableRow).contains("CEDRES").should("be.visible");
  }
}

export default OrganizationManagementPage;
