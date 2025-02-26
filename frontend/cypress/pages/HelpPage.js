class HelpPage {
  constructor() {}

  clickUserManual() {
    cy.get("#menu_help_user_manual").should("be.visible");
  }

  clickProcessDocumentation() {
    cy.get("[data-cy='menu_help_documents']").click();
  }

  clickVLForm() {
    cy.get("#menu_help_form_VL").should("be.visible");
  }

  clickDBSForm() {
    cy.get("[data-cy='menu_help_form_DBS']").should("be.visible");
  }
}

export default HelpPage;
