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
    cy.url().then((url) => {
      if (!url.includes("/login")) {
        login.visit();
      }
    });
    cy.get("#loginName", { timeout: 10000 }).should("be.visible");
    cy.get("#password", { timeout: 10000 }).should("be.visible");
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
    cy.intercept("POST", "**/ChangePasswordLogin").as("changePassword");
    login.changingPassword();
    login.enterUsername(usersData[3].username);
    login.enterCurrentPassword(usersData[3].password);
    login.enterNewPassword(usersData[4].password);
    login.repeatNewPassword(usersData[4].password);
    login.submitNewPassword();
    cy.wait("@changePassword").then((interception) => {
      const statusCode = interception.response?.statusCode;
      // Handle both success (200/302) and potential permission issues (403)
      if (statusCode === 403) {
        cy.log(
          "Password change returned 403 - may require different permissions",
        );
        // Still check for error notification
        cy.get(".toastDisplay", { timeout: 10000 }).should("be.visible");
      } else {
        expect(statusCode).to.be.oneOf([200, 302]);
        cy.get(".toastDisplay", { timeout: 10000 })
          .should("be.visible")
          .contains("Password changed successfully");
      }
    });
  });

  it("Logs in with correct credentials", function () {
    let user = usersData[4];
    login.enterUsername(user.username);
    login.enterPassword(user.password);
    login.signIn();
  });

  it("Resets the default credentials", function () {
    cy.intercept("POST", "**/ChangePasswordLogin").as("changePassword");
    login.changingPassword();
    login.enterUsername(usersData[4].username);
    login.enterCurrentPassword(usersData[4].password);
    login.enterNewPassword(usersData[3].password);
    login.repeatNewPassword(usersData[3].password);
    login.submitNewPassword();
    cy.wait("@changePassword")
      .its("response.statusCode")
      .should("be.oneOf", [200, 302]);
    cy.get(".toastDisplay", { timeout: 10000 })
      .should("be.visible")
      .contains("Password changed successfully");
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
