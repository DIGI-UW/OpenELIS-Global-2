class HelpPage {
  constructor() {}

  clickUserManual() {
    cy.get("#menu_help_user_manual").click();
  }

  clickProcessDocumentation() {
    cy.get("[data-cy='menu_help_documents']").click();
  }

  clickVLForm() {
    cy.get("#menu_help_form_VL").click();
  }

  clickDBSForm() {
    cy.get("[data-cy='menu_help_form_DBS']").click();
  }
}

export default HelpPage;
