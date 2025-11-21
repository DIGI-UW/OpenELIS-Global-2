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
    // CustomDatePicker wraps the input - find the actual input element
    cy.get(this.selectors.passwordExpirationDate)
      .find("input")
      .should("be.visible")
      .clear({ force: true })
      .type(value, { force: true });
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
    cy.get(this.selectors.copyPermisionsFromUser)
      .should("be.visible")
      .type(value);
    cy.get(this.selectors.autoSuggestion)
      .should("be.visible")
      .contains(value)
      .click();
  }

  applyChanges() {
    // applyChanges() clicks the "Apply" button which calls userSavePostCall()
    // This SAVES the user and navigates to the list page (same as saveChanges())
    cy.get(this.selectors.applyButton).should("be.visible").click();
    // Wait for save to complete - success notification appears
    cy.get(".toastDisplay", { timeout: 10000 })
      .should("be.visible")
      .should("contain.text", "success");
    // Wait for navigation to list page
    cy.url().should("include", "userManagement");
    cy.get(".cds--data-table").should("be.visible");
  }

  removePermission() {
    cy.get(this.selectors.removePermission).click();
  }
  //All Lab Units
  addNewPermission() {
    cy.get(this.selectors.addNewPermission).should("be.visible").click();
    // Wait for the permissions section to appear after clicking - React needs time to render
    // Carbon Select component - wait for any select element with id starting with "select-"
    // Use Cypress retry-ability instead of fixed timeout
    cy.get(
      "[id^='select-'], .cds--select[id^='select-'], select[id^='select-']",
    )
      .first()
      .should("exist")
      .should("be.visible");
  }

  allPermissions() {
    // Wait for the checkbox to appear (it's created dynamically after addNewPermission)
    cy.get(this.selectors.allPermissions).should("be.visible");
    cy.screenshot("allPermissions-before-check");
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
    // Save button might be disabled until form is valid
    // Wait for it to be enabled
    cy.get(this.selectors.saveButton)
      .should("exist")
      .should("be.visible")
      .then(($btn) => {
        // If disabled, wait a bit for validation to complete
        if ($btn && $btn.length > 0 && $btn.is(":disabled")) {
          cy.wait(1000);
        }
      });
    // Re-query to avoid detached element issues
    cy.get(this.selectors.saveButton).should("not.be.disabled").click();
    // After save, app shows success notification then navigates to list page after 200ms
    // Wait for success notification - check for "success" text in toast container
    // Carbon ToastNotification animates in, so wait for text to appear (more reliable than checking visibility)
    cy.get(".toastDisplay", { timeout: 10000 }).should(
      "contain.text",
      "success",
    );
    // Wait for navigation to complete (navigation happens 200ms after notification)
    cy.url().should("include", "userManagement");
    // Wait for page to be fully loaded and stable
    cy.get(".cds--data-table").should("be.visible");
    // Ensure page is fully ready - wait for body to be stable
    cy.get("body").should("be.visible");
  }

  exitChanges() {
    cy.get(this.selectors.exitButton).should("be.visible").click();
    // Wait for navigation back to user management list page
    cy.url().should("include", "userManagement");
    // Wait for the user management table to be visible
    cy.get(this.selectors.tableData).should("be.visible");
  }

  //Global Roles
  analyzerImport() {
    // Wait for form to be ready
    cy.get("form").should("be.visible");
    cy.screenshot("analyzerImport-before-search");
    // Find Analyser Import checkbox - Carbon Checkbox renders labelText as part of the checkbox
    // Break up the chain to avoid undefined after screenshot
    cy.contains("Analyser Import")
      .should("be.visible")
      .scrollIntoView()
      .as("analyserImport");
    cy.screenshot("analyzerImport-found");
    cy.get("@analyserImport").click();
  }

  auditTrail() {
    cy.contains(this.selectors.span, "Audit Trail").click({ force: true });
  }

  cytopathologist() {
    cy.contains(this.selectors.span, "Cytopathologist").click();
  }

  globalAdministrator() {
    // Global Administrator is a Carbon Checkbox with labelText
    // Find the checkbox by its label text - Carbon renders labelText as part of the checkbox
    cy.contains("label", "Global Administrator")
      .should("be.visible")
      .scrollIntoView()
      .click();
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
    // Find the table first, then look for the column within tbody
    cy.get(this.selectors.tableData)
      .should("be.visible")
      .find(`tbody tr td:nth-child(${columnNum})`)
      .should("be.visible")
      .should("contain", value);
  }

  inactiveUser(value) {
    // Wait for table to load and filter to apply
    cy.get(this.selectors.tableData).should("be.visible");
    // Wait for filter to apply and table to update - filter triggers API call
    cy.wait(2000); // Increased wait for filter to apply and API to respond
    cy.screenshot(`inactiveUser-before-check-${value}`);
    // Check table body specifically (not just the table element)
    // Wait for table to update after filter
    cy.get(this.selectors.tableData)
      .find("tbody")
      .should("be.visible")
      .should("not.contain", value);
  }

  nonAdminUser(value) {
    cy.get(this.selectors.tableData).should("not.contain", value);
  }

  activeUser() {
    // Break up the chain to avoid detached element error
    cy.contains(this.selectors.span, "Only Active")
      .should("be.visible")
      .as("activeFilter");
    cy.get("@activeFilter").click();
    // Wait for filter to apply and table to update
    cy.wait(1000);
  }

  uncheckActiveUser() {
    cy.get(this.selectors.uncheckActiveUser)
      .should("be.visible")
      .uncheck({ force: true });
  }

  checkUser(columnNum, value) {
    // Wait for table to have data
    cy.get(this.selectors.tableData).should("be.visible");
    // Wait for table body to have rows with actual content (not height 0)
    cy.get(this.selectors.tableData)
      .find("tbody")
      .should("be.visible")
      .should("have.css", "height")
      .and("not.equal", "0px");
    cy.get(this.selectors.tableData)
      .find("tbody tr")
      .should("have.length.greaterThan", 0)
      .should("be.visible");
    cy.screenshot(`checkUser-before-search-column-${columnNum}-value-${value}`);
    cy.get(this.selectors.tableData)
      .find(`tbody tr td:nth-child(${columnNum})`)
      .should("be.visible")
      .should("contain", value)
      .click();
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
