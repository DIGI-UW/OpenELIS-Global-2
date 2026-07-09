const TEST_ORG_NAME = "TEST-ORG-E2E";
const TEST_LAB_NAME = "TEST-LAB-E2E";
const TEST_ORG_PREFIX = "E2E-ORG";

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

  addOrgName(orgName = TEST_ORG_NAME) {
    cy.get(this.selectors.orgName)
      .should("be.visible")
      .clear()
      .type(orgName)
      .should("have.value", orgName);
  }

  addInstituteName(instituteName = TEST_LAB_NAME) {
    cy.get(this.selectors.orgName)
      .should("be.visible")
      .clear()
      .type(instituteName)
      .should("have.value", instituteName);
  }

  activateOrganization() {
    cy.get(this.selectors.isActive).clear().type("Y").should("have.value", "Y");
  }

  addPrefix(prefix = TEST_ORG_PREFIX) {
    cy.get(this.selectors.orgPrefix)
      .should("be.visible")
      .clear()
      .type(prefix)
      .should("have.value", prefix);
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

  addParentOrg(parentOrgName = TEST_ORG_NAME) {
    cy.get(this.selectors.parentOrgName)
      .should("be.visible")
      .clear()
      .type(parentOrgName)
      .should("have.value", parentOrgName);
  }

  saveOrganization() {
    cy.get(this.selectors.saveButton).should("be.visible").click();
    cy.url().should("include", "/MasterListsPage");
  }

  searchOrganzation(orgName = TEST_ORG_NAME) {
    cy.get(`input${this.selectors.orgSearchBar}`)
      .should("be.visible")
      .scrollIntoView();

    cy.get(`input${this.selectors.orgSearchBar}`)
      .focus()
      .clear({ force: true });

    cy.get(`input${this.selectors.orgSearchBar}`).type(orgName, {
      force: true,
    });
  }

  searchInstitute(instituteName = TEST_LAB_NAME) {
    cy.get(`input${this.selectors.orgSearchBar}`)
      .should("be.visible")
      .scrollIntoView();

    cy.get(`input${this.selectors.orgSearchBar}`)
      .focus()
      .clear({ force: true });

    cy.get(`input${this.selectors.orgSearchBar}`).type(instituteName, {
      force: true,
    });
  }

  confirmOrganization(orgName = TEST_ORG_NAME) {
    cy.get(this.selectors.orgTableRow).contains(orgName).should("be.visible");
  }

  confirmInstitute(instituteName = TEST_LAB_NAME) {
    cy.get(this.selectors.orgTableRow)
      .contains(instituteName)
      .should("be.visible");
  }
}

export default OrganizationManagementPage;
