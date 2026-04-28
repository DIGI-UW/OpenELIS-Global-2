class HelpPage {
  constructor() {
    this.selectors = {
      // Original selectors - may be work in progress or legacy implementation
      userManual: "#menu_help_user_manual_nav",
      processDocumentation: "#menu_help_documents", // parent menu - no _nav suffix
      vlForm: "#menu_help_form_VL_nav",
      dbsForm: "#menu_help_form_DBS_nav",

      // New SlideOverHelp CSS selectors (currently implemented)
      // Note: Only User Manual is available in new implementation
      newUserManual: ".help-slide-button",
    };
  }

  clickUserManual() {
    cy.get(this.selectors.userManual)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  // Alternative method using new SlideOverHelp implementation
  clickUserManualNew() {
    // Uses new CSS selector - finds button by text content
    cy.get(this.selectors.newUserManual)
      .contains("User Manual")
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  clickProcessDocumentation() {
    // Note: Process Documentation not yet implemented in new SlideOverHelp component
    // This method uses legacy selectors - may be work in progress
    cy.get(this.selectors.processDocumentation)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  clickVLForm() {
    // Note: VL Form not yet implemented in new SlideOverHelp component
    // This method uses legacy selectors - may be work in progress
    cy.get(this.selectors.vlForm)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }

  clickDBSForm() {
    // Note: DBS Form not yet implemented in new SlideOverHelp component
    // This method uses legacy selectors - may be work in progress
    cy.get(this.selectors.dbsForm)
      .scrollIntoView()
      .should("exist")
      .click({ force: true });
  }
}

export default HelpPage;
