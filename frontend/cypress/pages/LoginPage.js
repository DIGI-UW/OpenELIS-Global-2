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
    cy.visit("/login");
  }

  getUsernameElement() {
    return cy.get(SELECTORS.USERNAME);
  }

  getPasswordElement() {
    return cy.get(SELECTORS.PASSWORD);
  }

  enterUsername(value) {
    cy.wait(3000);
    this.getUsernameElement().should("be.visible");
    this.getUsernameElement().type(value);
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
    cy.wait(200);
    cy.get(SELECTORS.LOGOUT).should("be.visible");
    cy.get(SELECTORS.LOGOUT).click();
    cy.wait(1000);
  }

  changingPassword() {
    cy.get(SELECTORS.CHANGE_PASSWORD).click();
    cy.wait(500);
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
    cy.get("[data-cy='submitNewPassword']").should("be.visible");
    cy.get("[data-cy='submitNewPassword']").click();
    cy.wait(800);
  }

  clickExitPasswordReset() {
    cy.get("[data-cy='exitPasswordReset']").should("be.visible");
    cy.get("[data-cy='exitPasswordReset']").click();
    cy.wait(800);
  }
  clearInputs() {
    this.getUsernameElement().clear();
    this.getPasswordElement().clear();
  }

  goToHomePage() {
    cy.wait(1000);
    cy.url().then((url) => {
      if (url.includes("/login")) {
        cy.contains("button", "Login", { timeout: 10000 }).should("be.visible");
        this.enterUsername(this.testProperties.getUsername());
        this.enterPassword(this.testProperties.getPassword());
        this.signIn();
      }
    });
    cy.wait(5000);
    return new HomePage();
  }
}

export default LoginPage;
