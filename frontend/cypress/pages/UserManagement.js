class UserManagementPage {
  visitUserManagementPage() {
    cy.visit("/home");
    cy.get("#menu_admin").click();
    cy.get("#menu_admin_usermgmt").click();
  }

  openAddUserModal() {
    cy.get('[aria-label="Add User"]').click();
  }

  closeAddUserModal() {
    cy.get('[aria-label="Cancel"]').click();
  }

  isAddUserModalVisible() {
    return cy.get('[aria-label="Add User Modal"]').should("be.visible");
  }

  fillUserForm({ username, email, role }) {
    if (username) cy.get("#username").clear().type(username);
    if (email) cy.get("#email").clear().type(email);
    if (role) cy.get("#roleDropdown").select(role);
  }

  submitUserForm() {
    cy.get('[aria-label="Submit"]').click();
  }

  confirmAddUser() {
    cy.contains("User added successfully").should("be.visible");
  }

  validateFormErrors() {
    cy.get("#username").parent().should("contain", "This field is required");
    cy.get("#email").parent().should("contain", "This field is required");
  }

  getExistingUsers() {
    return cy.get(".user-row");
  }

  getInactiveUsers() {
    return cy.get(".inactive-user-row");
  }

  searchUser(username) {
    cy.get("#searchUser").type(username);
    cy.get("#searchButton").click();
  }

  verifySearchResults(username) {
    cy.get(".user-row").should("contain", username);
  }

  toggleUserStatus(username) {
    this.searchUser(username);
    cy.contains(".user-row", username)
      .find('[aria-label="Toggle Status"]')
      .click();
  }

  deleteUser(username) {
    this.searchUser(username);
    cy.contains(".user-row", username)
      .find('[aria-label="Delete User"]')
      .click();
    cy.get('[aria-label="Confirm Delete"]').click();
  }

  verifyDeleteSuccess() {
    cy.contains("User deleted successfully").should("be.visible");
  }
}

export default new UserManagementPage();
