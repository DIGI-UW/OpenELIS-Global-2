import LoginPage from "../../../pages/LoginPage";

let loginPage, homePage, adminPage, userManagementPage;

describe("User Management E2E", () => {
  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();
    loginPage.login(users.admin.username, users.admin.password);
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
    userManagementPage = adminPage.goToUserManagementPage();
  });

  it("should open and close Add User modal", () => {
    userManagementPage.openAddUserModal();
    cy.get('[aria-label="Add New User"]').should("be.visible");
    userManagementPage.closeAddUserModal();
    cy.get('[aria-label="Add New User"]').should("not.exist");
  });

  it("should show error when submitting empty Add User form", () => {
    userManagementPage.openAddUserModal();
    userManagementPage.submitUserForm();
    cy.get("#username").parent().should("contain", "This field is required");
  });

  it("should add a new user successfully", () => {
    userManagementPage.openAddUserModal();
    userManagementPage.fillAddUserForm(users.newUser);
    userManagementPage.submitUserForm();
    userManagementPage.assertUserAdded(users.newUser.username);
  });
});
