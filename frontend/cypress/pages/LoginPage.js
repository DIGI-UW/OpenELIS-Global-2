import HomePage from "./HomePage";
import TestProperties from "../common/TestProperties";


const SELECTORS = {
  username: "#loginName",
  password: "#password",
  loginButton: "[data-cy='loginButton']",
  logoutButton: "[data-cy='logOut']",
  userIcon: "#user-Icon",
  changePassword: "[data-cy='changePassword']",
  currentPassword: "#current-password",
  newPassword: "#new-password",
  repeatNewPassword: "#repeat-new-password",
  submitNewPassword: "[data-cy='submitNewPassword']",
  exitPasswordReset: "[data-cy='exitPasswordReset']",
};

class LoginPage {
  testProperties = new TestProperties();

  visit() {
    cy.visit("/login");
  }

  enterUsername(value) {
    cy.get(SELECTORS.username).should("be.visible").clear().type(value);
  }

  enterPassword(value) {
    cy.get(SELECTORS.password).should("be.visible").clear().type(value);
  }

  signIn() {
    cy.get(SELECTORS.loginButton).should("be.visible").click();
  }

  signOut() {
    cy.get(SELECTORS.userIcon).should("be.visible").click();
    cy.get(SELECTORS.logoutButton).should("be.visible").click();
  }

  changingPassword() {
    cy.get(SELECTORS.changePassword).should("be.visible").click();
  }

  enterCurrentPassword(value) {
    cy.get(SELECTORS.currentPassword).should("be.visible").type(value);
  }

  enterNewPassword(value) {
    cy.get(SELECTORS.newPassword).should("be.visible").type(value);
  }

  repeatNewPassword(value) {
    cy.get(SELECTORS.repeatNewPassword).should("be.visible").type(value);
  }

  submitNewPassword() {
    cy.get(SELECTORS.submitNewPassword).should("be.visible").click();
  }

  clickExitPasswordReset() {
    cy.get(SELECTORS.exitPasswordReset).should("be.visible").click();
  }

  clearInputs() {
    cy.get(SELECTORS.username).clear();
    cy.get(SELECTORS.password).clear();
  }

  goToHomePage() {
    cy.url().then((url) => {
      if (url.includes("/login")) {
        cy.contains("button", "Login", { timeout: 10000 }).should("be.visible");
        this.enterUsername(this.testProperties.getUsername());
        this.enterPassword(this.testProperties.getPassword());
        this.signIn();
      }
    });
    return new HomePage();
  }
}

export default LoginPage;
