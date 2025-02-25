import HomePage from "./HomePage";
import TestProperties from "../common/TestProperties";

class LoginPage {
  testProperties = new TestProperties();

  visit() {
    cy.visit("/login");
  }

  getUsernameElement() {
    return cy.get("#loginName");
  }

  getPasswordElement() {
    return cy.get("#password");
  }

  enterUsername(value) {
    this.getUsernameElement().should("be.visible");
    this.getUsernameElement().type(value);
  }

  enterPassword(value) {
    this.getPasswordElement().should("be.visible");
    this.getPasswordElement().type(value);
  }

  signIn() {
    cy.get("[data-cy='loginButton']").should("be.visible");
    cy.get("[data-cy='loginButton']").click();
  }

  signOut() {
    cy.get("#user-Icon").should("be.visible");
    cy.get("#user-Icon").click();
    cy.wait(200);
    cy.get("[data-cy='logOut']").should("be.visible");
    cy.get("[data-cy='logOut']").click();
    cy.wait(1000);
  }

  changingPassword() {
    cy.get("[data-cy='changePassword']").click();
    cy.wait(500);
  }

  enterCurrentPassword(value) {
    cy.get("#current-password").should("be.visible");
    cy.get("#current-password").type(value);
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
    return new HomePage();
  }
}

export default LoginPage;
