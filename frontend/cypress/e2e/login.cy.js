import LoginPage from "../pages/LoginPage";

const login = new LoginPage();
let usersData; // Store fixture data globally to avoid multiple calls

describe("Login Test Cases", function () {
  before("Load users fixture", () => {
    cy.fixture("Users").then((users) => {
      usersData = users;
    });
  });

  beforeEach("User visits login page", () => {
    login.visit();
    login.clearInputs(); // Clear inputs instead of waiting for backend on each test
  });

  it("Tries to login without credentials", function () {
    cy.intercept("/api/OpenELIS-Global/LoginPage").as("backend");
    login.visit();
    cy.wait("@backend", { timeout: Cypress.env("STARTUP_WAIT_MILLISECONDS") });

    login.signIn();
    cy.contains("Username or Password are incorrect").should("be.visible");
  });

  it("Fails to login with only username", function () {
    login.enterUsername(usersData[3].username);
    login.signIn();
    cy.contains("Username or Password are incorrect").should("be.visible");
  });

  it("Fails to login with only password", function () {
    login.enterPassword(usersData[3].password);
    login.signIn();
    cy.contains("Username or Password are incorrect").should("be.visible");
  });

  it("User changes from default credentials", function () {
    login.changingPassword();
    login.enterUsername(usersData[3].username);
    login.enterCurrentPassword(usersData[3].password);
    login.enterNewPassword(usersData[4].password);
    login.repeatNewPassword(usersData[4].password);
    login.submitNewPassword();
    cy.contains("Password changed successfully").should("be.visible");
  });

  it("Logs in with correct credentials", function () {
    let user = usersData[4];
    login.enterUsername(user.username);
    login.enterPassword(user.password);
    login.signIn();
  });

  it("Resets the default credentials", function () {
    login.changingPassword();
    login.enterUsername(usersData[4].username);
    login.enterCurrentPassword(usersData[4].password);
    login.enterNewPassword(usersData[3].password);
    login.repeatNewPassword(usersData[3].password);
    login.submitNewPassword();
    //cy.get("div[role='status']").should("be.visible");
    cy.contains("Password changed successfully").should("be.visible");
  });

  it("User exits password reset", function () {
    login.changingPassword();
    login.enterUsername(usersData[3].username);
    login.enterCurrentPassword(usersData[3].password);
    login.enterNewPassword(usersData[4].password);
    login.repeatNewPassword(usersData[4].password);
    login.clickExitPasswordReset();
  });

  it("Validates user authentication", function () {
    usersData.forEach((user) => {
      // Reloads the page
      cy.reload();

      login.enterUsername(user.username);
      login.enterPassword(user.password);
      login.signIn();

      if (user.correctPass === true) {
        cy.get("#mainHeader").should("exist");
        cy.get("[data-cy='menuButton']").should("exist");
      }
    });
  });
});
