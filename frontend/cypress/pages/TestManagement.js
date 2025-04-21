class TestManagementPage {
  // Navigation methods
  visitMethodManagementPage() {
    cy.get("#menu_administration").click();
    cy.contains("span", "Test Management").click();
    cy.get("[data-cy='methodManagement']").should("be.visible").click();
    cy.url().should("include", "#methodManagement");
    cy.contains("Method Management").should("be.visible");
    return this;
  }

  visitTestCatalogPage() {
    cy.get("#menu_administration").click();
    cy.contains("span", "Test Management").click();
    cy.get("[data-cy='testCatalog']").should("be.visible").click();
    cy.url().should("include", "#testCatalog");
    cy.contains("Test Catalog").should("be.visible");
    return this;
  }

  // Method Management modal operations
  openAddMethodModal() {
    cy.get("[data-cy='addMethod']").click();
    cy.contains("Add Method").should("be.visible");
    return this;
  }

  closeAddMethodModal() {
    cy.get("[data-cy='cancelAddMethod']").click();
    cy.contains("Add Method").should("not.exist");
    return this;
  }

  verifyMethodModalVisible() {
    cy.contains("Add Method").should("be.visible");
    return this;
  }

  // Method creation and verification
  fillMethodForm(methodName, description, category) {
    if (methodName) {
      cy.get("[data-cy='methodName']").clear().type(methodName);
    }

    if (description) {
      cy.get("[data-cy='methodDescription']").clear().type(description);
    }

    if (category) {
      cy.get("[data-cy='methodCategory']").select(category);
    }

    return this;
  }

  saveMethod() {
    cy.get("[data-cy='saveMethod']").click();
    return this;
  }

  verifyMethodExists(methodName) {
    cy.get("[data-cy='methodsTable']").should("be.visible");
    cy.contains(methodName).should("be.visible");
    return this;
  }

  verifyMethodNotExists(methodName) {
    cy.get("[data-cy='methodsTable']").should("be.visible");
    cy.contains(methodName).should("not.exist");
    return this;
  }

  searchMethod(searchTerm) {
    cy.get("[data-cy='methodSearchBox']").clear().type(searchTerm);
    cy.get("[data-cy='searchButton']").click();
    return this;
  }

  // Method deletion
  deleteMethod(methodName) {
    cy.contains("tr", methodName).within(() => {
      cy.get("[data-cy='deleteMethod']").click();
    });
    return this;
  }

  confirmDelete() {
    cy.get("[data-cy='confirmDelete']").click();
    return this;
  }

  cancelDelete() {
    cy.get("[data-cy='cancelDelete']").click();
    return this;
  }

  // Test Catalog operations
  addTestToCatalog() {
    cy.get("[data-cy='addTest']").click();
    cy.contains("Add Test").should("be.visible");
    return this;
  }

  fillTestForm(testName, method, protocol) {
    if (testName) {
      cy.get("[data-cy='testName']").clear().type(testName);
    }

    if (method) {
      cy.get("[data-cy='testMethod']").select(method);
    }

    if (protocol) {
      cy.get("[data-cy='testProtocol']").clear().type(protocol);
    }

    return this;
  }

  saveTest() {
    cy.get("[data-cy='saveTest']").click();
    return this;
  }

  // Validation handling
  getValidationError() {
    return cy.get("[data-cy='validationError']");
  }

  // Table pagination
  goToNextPage() {
    cy.get("[data-cy='nextPage']").click();
    return this;
  }

  goToPreviousPage() {
    cy.get("[data-cy='previousPage']").click();
    return this;
  }

  changePageSize(size) {
    cy.get("[data-cy='pageSize']").select(size);
    return this;
  }
}

export default TestManagementPage;
