import LoginPage from "../../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let userManagementPage = null;
let adminPage = null;

before(() => {
  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("User Management E2E", function () {
  it("User opens and closes Add User modal", function () {
    userManagementPage.openAddUserModal();
    userManagementPage.verifyAddUserModalVisible();
    userManagementPage.closeAddUserModal();
    userManagementPage.verifyAddUserModalNotVisible();
  });

  it("User gets validation errors for empty fields", function () {
    userManagementPage.openAddUserModal();
    userManagementPage.submitForm();
    userManagementPage.verifyFormValidation();
    userManagementPage.closeAddUserModal();
  });

  it("User successfully creates a new user", function () {
    const newUser = {
      username: "cypressuser1",
      password: "Test@123",
      role: "Technician",
    };

    userManagementPage.openAddUserModal();
    userManagementPage.fillUserForm(newUser);
    userManagementPage.submitForm();
    userManagementPage.confirmUserCreation(newUser.username);
  });
});
