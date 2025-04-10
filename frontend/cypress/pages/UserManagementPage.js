class UserManagementPage {
  openAddUserModal() {
    cy.get('[data-testid="add-user-btn"]').click();
  }

  closeAddUserModal() {
    cy.get('[aria-label="Cancel"]').click();
  }

  verifyAddUserModalVisible() {
    cy.get('[aria-label="Add New User"]').should("be.visible");
  }

  verifyAddUserModalNotVisible() {
    cy.get('[aria-label="Add New User"]').should("not.exist");
  }

  submitForm() {
    cy.get('[data-testid="submit-btn"]').click();
  }

  verifyFormValidation() {
    cy.get("#username")
      .parent()
      .should("contain.text", "This field is required");
    cy.get("#password")
      .parent()
      .should("contain.text", "This field is required");
  }

  fillUserForm({ username, password, role }) {
    cy.get("#username").clear().type(username);
    cy.get("#password").clear().type(password);
    cy.get("#roleDropdown").select(role);
  }

  confirmUserCreation(username) {
    cy.contains("User added successfully").should("exist");
    cy.get("#userList").should("contain.text", username);
  }
}

export default new UserManagementPage();
