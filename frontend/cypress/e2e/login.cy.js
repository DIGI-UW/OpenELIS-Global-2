import LoginPage from "../pages/LoginPage";

const login = new LoginPage();

describe("Failing or Succeeding to Login", function () {
  beforeEach("User visits login page", () => {
    cy.intercept("/api/OpenELIS-Global/LoginPage").as("backend");
    login.visit();
    cy.wait("@backend", {
      //wait up to configured time minutes for application to startup
      timeout: Cypress.env("STARTUP_WAIT_MILLISECONDS"),
    });
    // login.acceptSelfAssignedCert();
  });

  afterEach("Close Browser", () => {
    cy.clearLocalStorage();
  });
  it("Attempts to login without providing username and password", function () {
    login.signIn();
  });

  it("Attempts to login with only a username", function () {
    cy.fixture("Users").then((users) => {
      let user = users[3];
      login.enterUsername(user.username);
      login.signIn();
    });
  });

  it("Attempts to login with only a password", function () {
    cy.wait(500);
    cy.fixture("Users").then((users) => {
      let user = users[3];
      login.enterPassword(user.password);
      login.signIn();
    });
  });
  it("Should validate user authentication", function () {
    cy.wait(500);
    cy.fixture("Users").then((users) => {
      users.forEach((user) => {
        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();

        if (user.correctPass === true) {
          cy.get("header#mainHeader > button[title='Open menu']").should(
            "exist",
          );
          cy.get(".custom-action svg path:first-of-type").should("exist");
        } else {
          cy.get("div[role='status']").should("be.visible");
        }
      });
    });
  });
});
