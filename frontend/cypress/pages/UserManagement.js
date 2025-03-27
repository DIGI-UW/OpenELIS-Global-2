class UserPage {
  visit() {
    cy.visit("/admin/user-management"); // Adjust URL based on your app
  }

  clickAddUser() {
    cy.get('[data-cy="add-user-btn"]').click();
  }

  fillUserDetails(username, password, email, role) {
    cy.get('[data-cy="username-input"]').type(username);
    cy.get('[data-cy="password-input"]').type(password);
    cy.get('[data-cy="email-input"]').type(email);
    cy.get('[data-cy="role-select"]').select(role);
  }

  saveUser() {
    cy.get('[data-cy="save-user-btn"]').click();
  }

  searchUser(username) {
    cy.get('[data-cy="search-input"]').type(username);
    cy.get('[data-cy="search-btn"]').click();
  }

  clickEditUser(username) {
    this.searchUser(username);
    cy.get(`[data-cy="edit-btn-${username}"]`).click();
  }

  modifyUser(newUsername, newEmail) {
    cy.get('[data-cy="username-input"]').clear().type(newUsername);
    cy.get('[data-cy="email-input"]').clear().type(newEmail);
  }

  deleteUser(username) {
    this.searchUser(username);
    cy.get(`[data-cy="delete-btn-${username}"]`).click();
    cy.get('[data-cy="confirm-delete"]').click();
  }
}

export default UserPage;
