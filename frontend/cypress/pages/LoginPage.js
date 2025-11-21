import HomePage from "./HomePage";
import TestProperties from "../common/TestProperties";

const SELECTORS = {
  USERNAME: "#loginName",
  PASSWORD: "#password",
  LOGIN_BUTTON: "[data-cy='loginButton']",
  USER_ICON: "#user-Icon",
  LOGOUT: "[data-cy='logOut']",
  CHANGE_PASSWORD: "[data-cy='changePassword']",
  CURRENT_PASSWORD: "#current-password",
};
class LoginPage {
  testProperties = new TestProperties();

  visit() {
    cy.visit("/login", { failOnStatusCode: false });
    cy.get("body").should("be.visible");
    // Wait for login page to be ready - check if we're still on login page
    cy.url().should("include", "/login");
    // Wait for login button or username field to appear (depending on config)
    cy.get("body").should(($body) => {
      expect($body.find(SELECTORS.LOGIN_BUTTON).length > 0 || $body.find(SELECTORS.USERNAME).length > 0).to.be.true;
    });
  }

  getUsernameElement() {
    return cy.get(SELECTORS.USERNAME);
  }

  getPasswordElement() {
    return cy.get(SELECTORS.PASSWORD);
  }

  enterUsername(value) {
    this.getUsernameElement()
      .should("be.visible")
      .should("not.be.disabled")
      .type(value);
    this.getUsernameElement().should("have.value", value);
  }

  enterPassword(value) {
    this.getPasswordElement().should("be.visible");
    this.getPasswordElement().type(value);
    this.getPasswordElement().should("have.value", value);
  }

  signIn() {
    cy.get(SELECTORS.LOGIN_BUTTON).should("be.visible");
    cy.get(SELECTORS.LOGIN_BUTTON).click();
  }

  signOut() {
    cy.get(SELECTORS.USER_ICON).should("be.visible");
    cy.get(SELECTORS.USER_ICON).click();
    cy.get(SELECTORS.LOGOUT).should("be.visible");
    cy.get(SELECTORS.LOGOUT).click();
  }

  changingPassword() {
    // Wait for change password button to be available (might be on login page or home page)
    cy.url().then((url) => {
      if (url.includes("/login")) {
        // On login page - change password button should be visible
        cy.get(SELECTORS.CHANGE_PASSWORD, { timeout: 5000 })
          .should("be.visible")
          .should("not.be.disabled")
          .click();
      } else {
        // On home page - need to navigate to login first or find change password option
        cy.visit("/login");
        cy.get(SELECTORS.CHANGE_PASSWORD, { timeout: 5000 })
          .should("be.visible")
          .should("not.be.disabled")
          .click();
      }
    });
    // Wait for change password form to load
    cy.url({ timeout: 5000 }).should("include", "ChangePassword");
    cy.get("#loginName", { timeout: 5000 }).should("be.visible");
  }

  enterCurrentPassword(value) {
    cy.get(SELECTORS.CURRENT_PASSWORD).should("be.visible");
    cy.get(SELECTORS.CURRENT_PASSWORD).type(value);
  }

  enterNewPassword(value) {
    cy.get("#new-password").should("be.visible");
    cy.get("#new-password").type(value);
  }

  repeatNewPassword(value) {
    cy.get("#repeat-new-password").should("be.visible");
    cy.get("#repeat-new-password").type(value);
  }

  submitNewPassword() {
    // Wait for button to be enabled (form validation must pass)
    cy.get("[data-cy='submitNewPassword']")
      .should("be.visible")
      .should("not.be.disabled", { timeout: 5000 })
      .click();
  }

  clickExitPasswordReset() {
    cy.get("[data-cy='exitPasswordReset']").should("be.visible").click();
  }
  clearInputs() {
    // Wait for elements to be stable before clearing (prevent detached element errors)
    this.getUsernameElement()
      .should("be.visible")
      .should("exist")
      .clear({ force: true });
    this.getPasswordElement()
      .should("be.visible")
      .should("exist")
      .clear({ force: true });
  }

  goToHomePage() {
    cy.wait(1000);
    cy.url().then((url) => {
      if (url.includes("/login")) {
        cy.get(SELECTORS.LOGIN_BUTTON, { timeout: 10000 }).should("be.visible");
        this.enterUsername(this.testProperties.getUsername());
        this.enterPassword(this.testProperties.getPassword());
        this.signIn();
      }
    });
    cy.get("#mainHeader, [data-cy='menuButton']", { timeout: 5000 }).should(
      "exist",
    );
    return new HomePage();
  }
}

export default LoginPage;
