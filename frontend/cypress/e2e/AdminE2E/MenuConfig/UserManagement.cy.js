import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let userPage = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("User Add/Modify", function () {
  it("User navigates to User Management Page", function () {
    userPage = adminPage.goToUserManagementPage();
  });

  it("User adds a new user", function () {
    userPage.clickAddUser();
    userPage.fillUserDetails({
      username: "testuser",
      fullName: "Test User",
      email: "testuser@example.com",
      role: "Admin",
    });
    userPage.setPermissions();
    userPage.saveUser();
  });

  it("User modifies an existing user", function () {
    userPage.searchUser("testuser");
    userPage.selectUser("testuser");
    userPage.updateUserDetails({ fullName: "Updated User" });
    userPage.saveChanges();
  });

  it("User copies permissions from another user", function () {
    userPage.searchUser("testuser");
    userPage.selectUser("testuser");
    userPage.copyPermissions("adminUser");
    userPage.saveChanges();
  });

  it("User disables an existing user", function () {
    userPage.searchUser("testuser");
    userPage.selectUser("testuser");
    userPage.disableUser();
    userPage.saveChanges();
  });

  it("User deletes an existing user", function () {
    userPage.searchUser("testuser");
    userPage.selectUser("testuser");
    userPage.deleteUser();
  });
});
