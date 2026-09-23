import LoginPage from "../pages/LoginPage";

describe("Session logout", () => {
  it("ends the authenticated session through the user menu", () => {
    const login = new LoginPage();
    login.visit();
    login
      .getUsernameElement()
      .should("be.visible")
      .type(login.testProperties.getUsername());
    login
      .getPasswordElement()
      .should("be.visible")
      .type(login.testProperties.getPassword());
    login.signIn();
    cy.get("#mainHeader").should("be.visible");
    login.signOut();
    cy.request("/api/OpenELIS-Global/session")
      .its("body.authenticated")
      .should("eq", false);
  });
});
