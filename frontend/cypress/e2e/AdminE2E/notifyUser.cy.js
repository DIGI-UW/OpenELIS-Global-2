import LoginPage from "../../pages/LoginPage";

describe("Notify User", function () {
  let homePage, loginPage, adminPage, notifyUserPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
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
