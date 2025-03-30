class UserManagementPage {
  visitLoginPage() {
    cy.visit("/login");
  }

  login() {
    cy.get("#login-button").click();
  }

  goToAdminPage() {
    cy.get("#admin-menu").click();
  }

  goToUserManagementPage() {
    cy.get("#user-management").click();
  }

  visitUserManagementPage() {
    cy.visit("/admin/user-management");
  }

  clickAddUser() {
    cy.get("#add-user").click();
  }

  fillUserDetails({ username, fullName, email, role }) {
    cy.get("#username").type(username);
    cy.get("#fullName").type(fullName);
    cy.get("#email").type(email);
    cy.get("#role").select(role);
  }

  setPermissions() {
    cy.get("#permissions").click();
    cy.get("#select-all-permissions").click();
  }

  submitForm() {
    cy.get("#save-user").click();
  }

  searchUser(username) {
    cy.get("#search-user").type(username);
    cy.get("#search-button").click();
  }

  selectUser(username) {
    cy.contains(username).click();
  }

  updateUserDetails({ fullName }) {
    cy.get("#fullName").clear().type(fullName);
  }

  copyPermissions(targetUser) {
    cy.get("#copy-permissions").click();
    cy.get("#copy-from").type(targetUser);
    cy.get("#confirm-copy").click();
  }

  disableUser() {
    cy.get("#disable-user").click();
  }

  deleteUser() {
    cy.get("#delete-user").click();
    cy.get("#confirm-delete").click();
  }
}
export default UserManagementPage;
