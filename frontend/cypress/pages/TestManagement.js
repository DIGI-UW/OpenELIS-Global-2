class TestManagementPage {
    // ===== Test Catalog Methods =====
    visitTestCatalog() {
      cy.visit('/MasterListsPage#TestCatalog');
    }
  
    getToggleButton() {
      return cy.get('#toggle');
    }
  
    getGuideSection() {
      return cy.get('.structured-list-wrapper');
    }
  
    getTestSectionMultiSelect() {
      return cy.get('#carbon-multiselect-example-3');
    }
  
    selectTestSection(section) {
      this.getTestSectionMultiSelect().click();
      cy.contains(section).click();
    }
  
    getSelectedTags() {
      return cy.get('[type="cyan"]');
    }
  
    getTabs() {
      return cy.get('[role="tablist"] button');
    }
  
    selectTab(section) {
      cy.contains('[role="tab"]', section).click();
    }
  
    getDataTable() {
      return cy.get('.bx--data-table');
    }
  
    getBreadcrumb() {
      return cy.get('.breadcrumb');
    }
  
    // ===== Manage Method Methods =====
    visitManageMethod() {
      cy.visit('/MasterListsPage#MethodManagment');
    }
  
    openAddMethodModal() {
      cy.get('button').contains('Add New Method').click();
    }
  
    fillMethodForm(english, french) {
      cy.get('#englishLabel').clear().type(english);
      cy.get('#frenchLabel').clear().type(french);
    }
  
    submitForm() {
      cy.get('button').contains('Save').click();
    }
  
    confirmAddMethod() {
      cy.get('button').contains('Accept').click();
    }
  
    getExistingMethods() {
      return cy.get('h4').contains('Existing Methods').parent().find('div > div');
    }
  
    getInactiveMethods() {
      return cy.get('h4').contains('Inactive Methods').parent().find('div > div');
    }
  }
  
  export default new TestManagementPage();
  