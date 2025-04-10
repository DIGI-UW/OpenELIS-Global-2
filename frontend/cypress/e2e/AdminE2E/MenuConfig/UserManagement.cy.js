import LoginPage from "../../../pages/LoginPage";
import UserManagementPage from "../../../pages/UserManagementPage";

describe("User Management E2E", () => {
  before(() => {
    LoginPage.visit();
    LoginPage.login();
    UserManagementPage.visitUserManagementPage();
  });

  it("should open and close Add User modal", () => {
    UserManagementPage.openAddUserModal();
    UserManagementPage.verifyAddUserModalVisible();
    UserManagementPage.closeAddUserModal();
    UserManagementPage.verifyAddUserModalNotVisible();
  });

  it("should show validation when required fields are empty", () => {
    UserManagementPage.openAddUserModal();
    UserManagementPage.submitForm();
    cy.get("#username").parent().should("contain", "This field is required");
    cy.get("#password").parent().should("contain", "This field is required");
  });

  it("should add a new user successfully", () => {
    const user = {
      username: "testuser",
      password: "Test@123",
      role: "Technician",
    };

    UserManagementPage.openAddUserModal();
    UserManagementPage.fillUserForm(user);
    UserManagementPage.submitForm();
    UserManagementPage.confirmUserCreation();
  });
});
