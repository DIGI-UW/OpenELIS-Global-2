import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

describe("Notify User", function () {
  let homePage, adminPage, notifyUserPage;

  // Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
  before(() => {
    cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
    // Navigate to home page after login
    const loginPage = new LoginPage();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
  });

  it("Navigate to Notify User Page", () => {
    notifyUserPage = adminPage.goToNotifyUserPage();
    notifyUserPage.validatePageTitle();
  });

  describe("Enter Only Message", () => {
    it("Type Message and Submit", () => {
      notifyUserPage.typeMessage();
      notifyUserPage.submitMessage();
      notifyUserPage.warningMessage();
    });
  });

  describe("Select User only and Submit", () => {
    it("Select User And Submit", () => {
      notifyUserPage.clearMessage();
      notifyUserPage.selectUser("External");
      notifyUserPage.submitMessage();
      notifyUserPage.warningMessage();
    });
  });

  describe("Enter Message and User", () => {
    it("Type Message", () => {
      notifyUserPage.validatePageTitle();
      notifyUserPage.typeMessage();
    });

    it("Select User And Submit", () => {
      notifyUserPage.selectUser("External");
      notifyUserPage.submitMessage();
    });
  });
});
