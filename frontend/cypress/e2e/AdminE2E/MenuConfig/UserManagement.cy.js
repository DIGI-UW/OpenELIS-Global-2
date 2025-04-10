import UserManagementPage from "../../pages/UserManagement";

describe("User Management E2E", () => {
  before(() => {
    UserManagementPage.visitUserManagementPage();
  });

  it("should open and close Add User modal", () => {
    UserManagementPage.openAddUserModal();
    UserManagementPage.isAddUserModalVisible();
    UserManagementPage.closeAddUserModal();
  });

  it("should show error for empty fields", () => {
    UserManagementPage.openAddUserModal();
    UserManagementPage.submitUserForm();
    UserManagementPage.validateFormErrors();
  });

  it("should add a user", () => {
    UserManagementPage.openAddUserModal();
    UserManagementPage.fillUserForm({
      username: "testuser",
      email: "test@example.com",
      role: "Admin",
    });
    UserManagementPage.submitUserForm();
    UserManagementPage.confirmAddUser();
    UserManagementPage.getExistingUsers().should("contain", "testuser");
  });
});
