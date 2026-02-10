const TEST_ORG_NAME = "TEST-ORG-E2E";
const TEST_LAB_NAME = "TEST-LAB-E2E";

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
  }

  addOrgName() {
    cy.get(this.selectors.orgName)
      .should("be.visible")
      .type(TEST_ORG_NAME)
      .should("have.value", TEST_ORG_NAME);
  }

  addInstituteName() {
    cy.get(this.selectors.orgName)
      .should("be.visible")
      .type(TEST_LAB_NAME)
      .should("have.value", TEST_LAB_NAME);
  }

  activateOrganization() {
    cy.get(this.selectors.isActive).clear().type("Y").should("have.value", "Y");
  }

  addPrefix() {
    cy.get(this.selectors.orgPrefix)
      .should("be.visible")
      .type("279")
      .should("have.value", "279");
  }

  addInstitutePrefix() {
    cy.get(this.selectors.orgPrefix).should("be.visible").clear();
  }

  checkReferringClinic() {
    cy.get(this.selectors.referringClinic)
      .check({ force: true })
      .should("be.checked");
  }

  checkReferalLab() {
    cy.get(this.selectors.referralLab)
      .check({ force: true })
      .should("be.checked");
  }

  addParentOrg() {
    cy.get(this.selectors.parentOrgName)
      .should("be.visible")
      .type(TEST_ORG_NAME)
      .should("have.value", TEST_ORG_NAME);
  }

  saveOrganization() {
    cy.get(this.selectors.saveButton).should("be.visible").click();
    cy.url().should("include", "/MasterListsPage");
  }

  searchOrganzation() {
    cy.get(`input${this.selectors.orgSearchBar}`)
      .should("be.visible")
      .scrollIntoView();

    cy.get(`input${this.selectors.orgSearchBar}`)
      .focus()
      .clear({ force: true });

    cy.get(`input${this.selectors.orgSearchBar}`).type(TEST_ORG_NAME, {
      force: true,
    });
  }

  searchInstitute() {
    cy.get(`input${this.selectors.orgSearchBar}`)
      .should("be.visible")
      .scrollIntoView();

    cy.get(`input${this.selectors.orgSearchBar}`)
      .focus()
      .clear({ force: true });

    cy.get(`input${this.selectors.orgSearchBar}`).type(TEST_LAB_NAME, {
      force: true,
    });
  }

  confirmOrganization() {
    cy.get(this.selectors.orgTableRow)
      .contains(TEST_ORG_NAME)
      .should("be.visible");
  }

  confirmInstitute() {
    cy.get(this.selectors.orgTableRow)
      .contains(TEST_LAB_NAME)
      .should("be.visible");
  }
}

export default OrganizationManagementPage;
