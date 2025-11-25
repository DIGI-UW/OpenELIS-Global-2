class UserManagementPage {
  constructor() {
    this.selectors = {
      pageTitle: "h2",
      userPageTitle: "h3",
      span: "span",
      addButton: "[data-cy='add-button']",
      modifyUser: "[data-cy='modify-button']",
      deactivateUser: "[data-cy='deactivate-button']",
      loginName: "#login-name",
      loginPassword: "#login-password",
      repeatPassword: "#login-repeat-password",
      firstName: "#first-name",
      lastName: "#last-name",
      passwordExpirationDate: "#password-expire-date",
      userTimeOut: "#login-timeout",
      accountLocked: "[for='radio-1']",
      accountNotLocked: "[for='radio-2']",
      accountDisabled: "[for='radio-3']",
      accountEnabled: "[for='radio-4']",
      isActive: "[for='radio-5']",
      isNotActive: "[for='radio-6']",
      copyPermisionsFromUser: "#copy-permissions",
      autoSuggestion: "[data-cy='auto-suggestion']",
      applyButton: "[data-cy='apply-button']",
      addNewPermission: "[data-cy='addNewPermission']",
      removePermission: "[data-cy='removePermission']",
      saveButton: "[data-cy='saveButton']",
      exitButton: "[data-cy='exitButton']",
      searchBar: "#user-name-search-bar",
      filters: "#filters",
      tableData: ".cds--data-table",
      menuButton: "[data-cy='menuButton']",
      enterLoginName: "#loginName",
      enterPassword: "#password",
      allPermissions: "#all-permissions-AllLabUnits",
      allBioPermissions: "#all-permissions-56",
      allHemaPermissions: "#all-permissions-36",
      allSeroPermissions: "#all-permissions-117",
      allImmunoPermissions: "#all-permissions-59",
      allMolecularPermissions: "#all-permissions-136",
      allCytoPermissions: "#all-permissions-165",
      allSerologyPermissions: "#all-permissions-97",
      allViroPermissions: "#all-permissions-76",
      allPathoPermissions: "#all-permissions-163",
      allImmunoHistoPermissions: "#all-permissions-164",
      loginButton: "[data-cy='loginButton']",
      uncheckActiveUser: "#only-active",
      uncheckAdminUser: "#only-administrator",
    };
  }

  enterLoginName(value) {
    cy.get(this.selectors.enterLoginName).clear().type(value);
  }

  enterPassword(value) {
    cy.get(this.selectors.enterPassword).clear().type(value);
  }

  loginButton() {
    cy.get(this.selectors.loginButton).click();
  }

  verifyPageTitle() {
    cy.contains(this.selectors.pageTitle, "User Management").should(
      "be.visible",
    );
  }

  validatePageTitle() {
    cy.contains(this.selectors.userPageTitle, "Add User").should("be.visible");
  }
  clickAddButton() {
    cy.get(this.selectors.addButton).click();
  }

  modifyUser() {
    cy.get(this.selectors.modifyUser).click();
    cy.wait(1000);
  }

  deactivateUser() {
    cy.get(this.selectors.deactivateUser).click();
  }

  typeLoginName(value) {
    cy.wait(1500);
    cy.get(this.selectors.loginName).clear().type(value);
  }

  typeLoginPassword(value) {
    cy.get(this.selectors.loginPassword).clear().type(value);
  }

  repeatPassword(value) {
    cy.get(this.selectors.repeatPassword).clear().type(value);
  }

  enterFirstName(value) {
    cy.get(this.selectors.firstName).type(value);
  }

  enterLastName(value) {
    cy.get(this.selectors.lastName).type(value);
  }

  passwordExpiryDate(value) {
    cy.get(this.selectors.passwordExpirationDate).clear().type(value);
  }

  enterUserTimeout(value) {
    cy.get(this.selectors.userTimeOut).clear().type(value);
  }

  checkAccountLocked() {
    cy.get(this.selectors.accountLocked).click();
  }

  checkAccountNotLocked() {
    cy.get(this.selectors.accountNotLocked).click();
  }

  checkActive() {
    cy.get(this.selectors.isActive).click();
  }

  checkNotActive() {
    cy.get(this.selectors.isNotActive).click();
  }

  checkAccountEnabled() {
    cy.get(this.selectors.accountEnabled).click();
  }

  checkAccountDisabled() {
    cy.get(this.selectors.accountDisabled).click();
  }

  copyPermisionsFromUser(value) {
    cy.get(this.selectors.copyPermisionsFromUser).type(value);
    cy.contains(this.selectors.autoSuggestion, value).click();
  }

  applyChanges() {
    cy.get(this.selectors.applyButton).click();
  }

  removePermission() {
    cy.get(this.selectors.removePermission).click();
  }
  //All Lab Units
  addNewPermission() {
    cy.get(this.selectors.addNewPermission).click();
  }

  allPermissions() {
    cy.get(this.selectors.allPermissions).check({ force: true });
  }

  allBioPermissions() {
    cy.get(this.selectors.allBioPermissions).check({ force: true });
  }

  allHemaPermissions() {
    cy.get(this.selectors.allHemaPermissions).check({ force: true });
  }

  allSeroPermissions() {
    cy.get(this.selectors.allSeroPermissions).check({ force: true });
  }

  allImmunoPermissions() {
    cy.get(this.selectors.allImmunoPermissions).check({ force: true });
  }

  allMolecularPermissions() {
    cy.get(this.selectors.allMolecularPermissions).check({ force: true });
  }

  allCytoPermissions() {
    cy.get(this.selectors.allCytoPermissions).check({ force: true });
  }

  allSerologyPermissions() {
    cy.get(this.selectors.allSerologyPermissions).check({ force: true });
  }

  allViroPermissions() {
    cy.get(this.selectors.allViroPermissions).check({ force: true });
  }

  allPathoPermissions() {
    cy.get(this.selectors.allPathoPermissions).check({ force: true });
  }

  allImmunoHistoPermissions() {
    cy.get(this.selectors.allImmunoHistoPermissions).check({ force: true });
  }

  reception() {
    cy.contains(this.selectors.span, "Reception").click();
  }

  reports() {
    cy.contains(this.selectors.span, "Reports").click();
  }

  results() {
    cy.contains(this.selectors.span, "Results").click();
  }

  saveChanges() {
    cy.get(this.selectors.saveButton).click();
  }

  exitChanges() {
    cy.get(this.selectors.exitButton).click();
  }

  //Global Roles
  analyzerImport() {
    cy.contains(this.selectors.span, "Analyser Import").click();
  }

  auditTrail() {
    cy.contains(this.selectors.span, "Audit Trail").click({ force: true });
  }

  cytopathologist() {
    cy.contains(this.selectors.span, "Cytopathologist").click();
  }

  globalAdministrator() {
    cy.contains(this.selectors.span, "Global Administrator").click();
  }

  pathologist() {
    cy.contains(this.selectors.span, "Pathologist").click();
  }

  userAccountAdmin() {
    cy.contains(this.selectors.span, "User Account Administrator").click();
  }

  searchUser(value) {
    cy.get(this.selectors.searchBar).clear().type(value);
  }

  clearSearchBar() {
    cy.get(this.selectors.searchBar).clear();
  }

  searchByFilters(value) {
    cy.get(this.selectors.filters).select(value);
  }

  validateColumnContent(columnNum, value) {
    cy.get(`td:nth-child(${columnNum})`).should("contain", value);
  }

  inactiveUser(value) {
    cy.get(this.selectors.tableData).should("not.contain", value);
  }

  nonAdminUser(value) {
    cy.get(this.selectors.tableData).should("not.contain", value);
  }

  activeUser() {
    cy.contains(this.selectors.span, "Only Active").click();
  }

  uncheckActiveUser() {
    cy.wait(900);
    cy.get(this.selectors.uncheckActiveUser).uncheck({ force: true });
  }

  checkUser(columnNum, value) {
    cy.get(`td:nth-child(${columnNum})`).should("contain", value).click();
  }

  adminUser() {
    cy.contains(this.selectors.span, "Only Administrator").click();
  }

  uncheckAdminUser() {
    cy.wait(900);
    cy.get(this.selectors.uncheckAdminUser).uncheck({ force: true });
  }
}

export default UserManagementPage;
