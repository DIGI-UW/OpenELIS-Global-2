class HelpPage {
  constructor() {
    this.selectors = {
      userManual: "#menu_help_user_manual",
      processDocumentation: "#menu_help_documents",
      vlForm: "#menu_help_form_VL",
      dbsForm: "[data-cy='menu_help_form_DBS']",
    };
  }

  clickUserManual() {
    cy.get(this.selectors.userManual)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  clickProcessDocumentation() {
    cy.get(this.selectors.processDocumentation)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  clickVLForm() {
    cy.get(this.selectors.vlForm)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  clickDBSForm() {
    cy.get(this.selectors.dbsForm)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }
}

export default HelpPage;
