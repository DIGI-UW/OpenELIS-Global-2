class UserManagementPage {
  constructor() {
    this.selectors = {
      pageTitle: "h2",
      userPageTitle: "h3",
      span: ".cds--checkbox-label-text",
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
      allPermissions: "[data-testid='all-permissions-All-Lab-Units']",
      allBioPermissions: "[data-testid='all-permissions-Biochemistry']",
      allHemaPermissions: "[data-testid='all-permissions-Hematology']",
      allSeroPermissions: "[data-testid='all-permissions-Serology-Immunology']",
      allImmunoPermissions: "[data-testid='all-permissions-Immunology']",
      allMolecularPermissions:
        "[data-testid='all-permissions-Molecular-Biology']",
      allCytoPermissions: "[data-testid='all-permissions-Cytology']",
      allSerologyPermissions: "[data-testid='all-permissions-Serology']",
      allViroPermissions: "[data-testid='all-permissions-Virology']",
      allPathoPermissions: "[data-testid='all-permissions-Pathology']",
      allImmunoHistoPermissions:
        "[data-testid='all-permissions-Immunohistochemistry']",
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
    // Find the actual input inside the CustomDatePicker component
    cy.get(this.selectors.passwordExpirationDate)
      .find("input")
      .clear({ force: true })
      .type(value, { force: true });
    // Close datepicker if open
    cy.get("body").type("{esc}"); // close datepicker without clicking (0,0) — that pixel is the logo link on desktop
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

  selectTestSection(sectionName) {
    cy.get('select[id^="select-"]').last().select(sectionName);
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

  watchUserListRequest(alias, matchesRequest) {
    cy.intercept("GET", "**/rest/SearchUnifiedSystemUserMenu*", (request) => {
      const searchParams = new URL(request.url).searchParams;
      if (matchesRequest(searchParams)) {
        request.alias = alias;
      }
    });
  }

  waitForUserListRequest(alias) {
    cy.wait(`@${alias}`).its("response.statusCode").should("equal", 200);
  }

  searchUser(value) {
    this.clearSearchBar();
    this.watchUserListRequest(
      "searchedUsers",
      (searchParams) =>
        searchParams.get("search") === "Y" &&
        searchParams.get("searchString") === value,
    );
    cy.get(this.selectors.searchBar)
      .should("be.visible")
      .and("be.enabled")
      .type(value);
    this.waitForUserListRequest("searchedUsers");
  }

  clearSearchBar() {
    cy.get(this.selectors.searchBar)
      .should("be.visible")
      .and("be.enabled")
      .invoke("val")
      .then((currentValue) => {
        if (!currentValue) {
          return;
        }

        this.watchUserListRequest(
          "clearedUserSearch",
          (searchParams) =>
            searchParams.get("search") === "Y" &&
            searchParams.get("searchString") === "",
        );
        cy.get(this.selectors.searchBar)
          .should("be.visible")
          .and("be.enabled")
          .clear();
        this.waitForUserListRequest("clearedUserSearch");
      });
  }

  searchByFilters(value) {
    cy.get(this.selectors.filters)
      .should("be.visible")
      .and("be.enabled")
      .find("option")
      .filter((_, option) => option.text.trim() === value)
      .should("have.length", 1)
      .then(($option) => {
        const roleFilter = $option.val();
        this.watchUserListRequest(
          "filteredUsersByRole",
          (searchParams) =>
            searchParams.get("search") === "N" &&
            searchParams.get("roleFilter") === roleFilter,
        );
        cy.get(this.selectors.filters).select(roleFilter);
        this.waitForUserListRequest("filteredUsersByRole");
      });
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
    this.watchUserListRequest("activeUsers", (searchParams) =>
      (searchParams.get("filter") || "").split(",").includes("isActive"),
    );
    cy.get(this.selectors.uncheckActiveUser).should("not.be.checked");
    cy.contains(this.selectors.span, "Only Active").click();
    cy.get(this.selectors.uncheckActiveUser).should("be.checked");
    this.waitForUserListRequest("activeUsers");
  }

  uncheckActiveUser() {
    this.watchUserListRequest(
      "allUsersAfterActiveFilter",
      (searchParams) =>
        !(searchParams.get("filter") || "").split(",").includes("isActive"),
    );
    cy.get(this.selectors.uncheckActiveUser).should("be.checked");
    cy.contains(this.selectors.span, "Only Active").click();
    cy.get(this.selectors.uncheckActiveUser).should("not.be.checked");
    this.waitForUserListRequest("allUsersAfterActiveFilter");
  }

  checkUser(columnNum, value) {
    cy.get(`td:nth-child(${columnNum})`).should("contain", value).click();
  }

  adminUser() {
    this.watchUserListRequest("administratorUsers", (searchParams) =>
      (searchParams.get("filter") || "").split(",").includes("isAdmin"),
    );
    cy.get(this.selectors.uncheckAdminUser).should("not.be.checked");
    cy.contains(this.selectors.span, "Only Administrator").click();
    cy.get(this.selectors.uncheckAdminUser).should("be.checked");
    this.waitForUserListRequest("administratorUsers");
  }

  uncheckAdminUser() {
    this.watchUserListRequest(
      "allUsersAfterAdministratorFilter",
      (searchParams) =>
        !(searchParams.get("filter") || "").split(",").includes("isAdmin"),
    );
    cy.get(this.selectors.uncheckAdminUser).should("be.checked");
    cy.contains(this.selectors.span, "Only Administrator").click();
    cy.get(this.selectors.uncheckAdminUser).should("not.be.checked");
    this.waitForUserListRequest("allUsersAfterAdministratorFilter");
  }
}

export default UserManagementPage;
