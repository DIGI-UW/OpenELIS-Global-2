class HelpPage {
  constructor() {
    this.selectors = {
      // Leaf items use _nav suffix to target the actual interactive element (SideNavMenuItem)
      // Parent menus (that expand submenus) don't need the suffix
      userManual: "#menu_help_user_manual_nav",
      processDocumentation: "#menu_help_documents", // parent menu - no _nav suffix
      vlForm: "#menu_help_form_VL_nav",
      dbsForm: "#menu_help_form_DBS_nav",
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
