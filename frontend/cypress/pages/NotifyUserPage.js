class NotifyUserPage {
  constructor() {
    this.selectors = {
      title: "h2",
      textArea: "#message",
      enterUser: "#user",
      submit: "[data-cy='submitButton']",
      useroption: ".suggestion-active",
    };
  }

  validatePageTitle() {
    cy.get(this.selectors.title)
      .should("be.visible")
      .and("contain.text", "Notify User");
  }

  typeMessage() {
    cy.get(this.selectors.textArea).clear().type("Remember to logout");
  }

  clearMessage() {
    cy.get(this.selectors.textArea).clear();
  }

  selectUser(value) {
    cy.get(this.selectors.enterUser).clear().type(value);
    cy.contains(this.selectors.useroption, value).should("be.visible").click();
  }

  submitMessage() {
    cy.get(this.selectors.submit).click();
  }

  warningMessage() {
    cy.contains("div", "User and Message are required").should("be.visible");
  }
}

export default NotifyUserPage;
