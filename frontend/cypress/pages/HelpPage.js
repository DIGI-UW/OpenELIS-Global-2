class HelpPage {
  constructor() {
    this.selectors = {
      userManual: "#menu_help_user_manual",
      processDocumentation: "[data-cy='menu_help_documents']",
      vlForm: "#menu_help_form_VL",
      dbsForm: "[data-cy='menu_help_form_DBS']",
    };
  }

  clickUserManual() {
    cy.get(this.selectors.userManual).click();
  }

  clickProcessDocumentation() {
    cy.get(this.selectors.processDocumentation).click();
  }

  clickVLForm() {
    cy.get(this.selectors.vlForm).click();
  }

  clickDBSForm() {
    cy.get(this.selectors.dbsForm).click();
  }
}

export default HelpPage;
