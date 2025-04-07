import LoginPage from "../pages/LoginPage";

describe("Login Test Cases", function () {
  const login = new LoginPage();

  beforeEach("User visits login page", () => {
    cy.fixture("Users").as("usersData");
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
    cy.get("@usersData").then((usersData) => {
      login.enterUsername(usersData[3].username);
      login.signIn();
      cy.contains("Username or Password are incorrect").should("be.visible");
    });
  });

  it("Fails to login with only password", function () {
    cy.get("@usersData").then((usersData) => {
      login.enterPassword(usersData[3].password);
      login.signIn();
      cy.contains("Username or Password are incorrect").should("be.visible");
    });
  });

  it("User changes from default credentials", function () {
    cy.get("@usersData").then((usersData) => {
      login.changingPassword();
      login.enterUsername(usersData[3].username);
      login.enterCurrentPassword(usersData[3].password);
      login.enterNewPassword(usersData[4].password);
      login.repeatNewPassword(usersData[4].password);
      login.submitNewPassword();
      cy.contains("Password changed successfully").should("be.visible");

      // Reset the password back to default to maintain test isolation
      login.changingPassword();
      login.enterUsername(usersData[4].username);
      login.enterCurrentPassword(usersData[4].password);
      login.enterNewPassword(usersData[3].password);
      login.repeatNewPassword(usersData[3].password);
      login.submitNewPassword();
    });
  });

  it("Logs in with correct credentials", function () {
    cy.get("@usersData").then((usersData) => {
      login.enterUsername(usersData[3].username);
      login.enterPassword(usersData[3].password);
      login.signIn();
      cy.get("#mainHeader").should("exist");
    });
  });

  it("Resets the default credentials", function () {
    cy.get("@usersData").then((usersData) => {
      // First change the password
      login.changingPassword();
      login.enterUsername(usersData[3].username);
      login.enterCurrentPassword(usersData[3].password);
      login.enterNewPassword(usersData[4].password);
      login.repeatNewPassword(usersData[4].password);
      login.submitNewPassword();

      // Now reset it back
      login.visit();
      login.changingPassword();
      login.enterUsername(usersData[4].username);
      login.enterCurrentPassword(usersData[4].password);
      login.enterNewPassword(usersData[3].password);
      login.repeatNewPassword(usersData[3].password);
      login.submitNewPassword();
      cy.contains("Password changed successfully").should("be.visible");
    });
  });

  it("User exits password reset", function () {
    cy.get("@usersData").then((usersData) => {
      login.changingPassword();
      login.enterUsername(usersData[3].username);
      login.enterCurrentPassword(usersData[3].password);
      login.enterNewPassword(usersData[4].password);
      login.repeatNewPassword(usersData[4].password);
      login.clickExitPasswordReset();
    });
  });

  it("Validates user authentication", function () {
    cy.get("@usersData").then((usersData) => {
      // Test each user's authentication individually
      for (const user of usersData) {
        // Clear inputs and try with this user
        login.clearInputs();

        login.enterUsername(user.username);
        login.enterPassword(user.password);
        login.signIn();

        if (user.correctPass === true) {
          cy.get("#mainHeader").should("exist");
          cy.get("[data-cy='menuButton']").should("exist");
          // Go back to login page for next iteration
          cy.visit(Cypress.config().baseUrl);
        } else {
          cy.contains("Username or Password are incorrect").should(
            "be.visible",
          );
        }
      }
    });
  });
});
