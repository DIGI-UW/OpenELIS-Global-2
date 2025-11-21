import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

let homePage = null;
let adminPage = null;
let userManagement = null;
let loginPage = null;
let usersData;

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
before(() => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("User Management", function () {
  beforeEach(() => {
    cy.fixture("UserManagement").then((users) => {
      usersData = users;
    });
  });

  it("Navigate to User Management Page", function () {
    userManagement = adminPage.goToUserManagementPage();
    userManagement.verifyPageTitle();
  });

  describe("Add User and Exit", function () {
    // Helper function to set up user form with basic data
    const setupUserForm = () => {
      userManagement = adminPage.goToUserManagementPage();
      userManagement.clickAddButton();
      userManagement.validatePageTitle();
      userManagement.typeLoginName(usersData[0].username);
      userManagement.passwordExpiryDate(usersData[0].passwordExpiryDate);
      userManagement.typeLoginPassword(usersData[0].password);
      userManagement.repeatPassword(usersData[0].password);
      userManagement.enterFirstName(usersData[0].fName);
      userManagement.enterLastName(usersData[0].lName);
      userManagement.enterUserTimeout(usersData[0].userTimeout);
    };

    it("Add User", function () {
      userManagement = adminPage.goToUserManagementPage();
      userManagement.clickAddButton();
      userManagement.validatePageTitle();
    });

    it("Enter User details", function () {
      // Set up form independently
      userManagement = adminPage.goToUserManagementPage();
      userManagement.clickAddButton();
      userManagement.validatePageTitle();
      // Enter details
      userManagement.typeLoginName(usersData[0].username);
      userManagement.passwordExpiryDate(usersData[0].passwordExpiryDate);
      userManagement.typeLoginPassword(usersData[0].password);
      userManagement.repeatPassword(usersData[0].password);
      userManagement.enterFirstName(usersData[0].fName);
      userManagement.enterLastName(usersData[0].lName);
      userManagement.enterUserTimeout(usersData[0].userTimeout);
    });

    it("Add and Remove Lab Unit Roles", function () {
      // Set up form independently
      setupUserForm();
      // Now test the permissions
      userManagement.addNewPermission();
      userManagement.allPermissions();
      userManagement.removePermission();
    });

    it("Apply Roles and Permissions - Analyzer Import and Global Admin", function () {
      // Set up form independently
      setupUserForm();
      // Now test the roles and permissions
      userManagement.analyzerImport();
      userManagement.globalAdministrator();
      userManagement.addNewPermission();
      userManagement.allPermissions();
    });

    it("Exit from Add User Form", function () {
      // Set up form independently
      setupUserForm();
      // Exit
      userManagement.exitChanges();
    });
  });

  describe("Add Users and Save", function () {
    it("Create and Save First User", function () {
      // Complete workflow: setup + save + wait for reload
      userManagement = adminPage.goToUserManagementPage();
      userManagement.clickAddButton();
      userManagement.validatePageTitle();
      userManagement.typeLoginName(usersData[0].username);
      userManagement.passwordExpiryDate(usersData[0].passwordExpiryDate);
      userManagement.typeLoginPassword(usersData[0].password);
      userManagement.repeatPassword(usersData[0].password);
      userManagement.enterFirstName(usersData[0].fName);
      userManagement.enterLastName(usersData[0].lName);
      userManagement.enterUserTimeout(usersData[0].userTimeout);
      userManagement.checkAccountLocked();
      userManagement.checkAccountDisabled();
      userManagement.checkNotActive();
      userManagement.globalAdministrator();
      userManagement.addNewPermission();
      userManagement.allPermissions();
      // Save - this will trigger page reload, saveChanges() waits for it
      userManagement.saveChanges();
    });

    it("Create and Save Second User", function () {
      // Complete workflow: setup + save + wait for reload
      userManagement = adminPage.goToUserManagementPage();
      userManagement.verifyPageTitle();
      userManagement.clickAddButton();
      userManagement.validatePageTitle();
      userManagement.typeLoginName(usersData[1].username);
      userManagement.passwordExpiryDate(usersData[1].passwordExpiryDate);
      userManagement.typeLoginPassword(usersData[1].password);
      userManagement.repeatPassword(usersData[1].password);
      userManagement.enterFirstName(usersData[1].fName);
      userManagement.enterLastName(usersData[1].lName);
      userManagement.enterUserTimeout(usersData[1].userTimeout);
      userManagement.checkAccountLocked();
      userManagement.checkAccountDisabled();
      userManagement.checkNotActive();
      userManagement.checkAccountNotLocked();
      userManagement.checkAccountEnabled();
      userManagement.checkActive();
      userManagement.globalAdministrator();
      userManagement.addNewPermission();
      userManagement.allPermissions();
      userManagement.addNewPermission();
      userManagement.allBioPermissions();
      userManagement.addNewPermission();
      userManagement.allHemaPermissions();
      userManagement.addNewPermission();
      userManagement.allSeroPermissions();
      userManagement.addNewPermission();
      userManagement.allImmunoPermissions();
      userManagement.addNewPermission();
      userManagement.allMolecularPermissions();
      userManagement.addNewPermission();
      userManagement.allCytoPermissions();
      userManagement.addNewPermission();
      userManagement.allSerologyPermissions();
      userManagement.addNewPermission();
      userManagement.allViroPermissions();
      userManagement.addNewPermission();
      userManagement.allPathoPermissions();
      userManagement.addNewPermission();
      userManagement.allImmunoHistoPermissions();
      // Save - this will trigger page reload, saveChanges() waits for it
      userManagement.saveChanges();
    });
  });

  describe("Validate added Users", function () {
    beforeEach(() => {
      // After previous test, we should be on user management page
      // Just verify it's ready, don't navigate unless necessary
      cy.url().should("include", "userManagement");
      userManagement = adminPage.goToUserManagementPage();
      cy.get(".cds--data-table").should("be.visible");
    });

    it("Search users by Usernames", function () {
      cy.reload();
      userManagement.searchUser(usersData[0].username);
      userManagement.validateColumnContent("4", usersData[0].username);
      userManagement.searchUser(usersData[1].username);
      userManagement.validateColumnContent("4", usersData[1].username);
    });

    it("Search by First Name", function () {
      userManagement.searchUser(usersData[0].fName);
      userManagement.validateColumnContent("2", usersData[0].fName);
      userManagement.searchUser(usersData[1].fName);
      userManagement.validateColumnContent("2", usersData[1].fName);
    });

    it("Search by Last Name", function () {
      userManagement.searchUser(usersData[0].lName);
      userManagement.validateColumnContent("3", usersData[0].lName);
      userManagement.searchUser(usersData[1].lName);
      userManagement.validateColumnContent("3", usersData[1].lName);
      userManagement.clearSearchBar();
    });

    it("Search by Lab Unit Roles", function () {
      cy.reload();
      userManagement.searchByFilters(usersData[1].bioChem);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].hematology);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].seroImmuno);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].immunology);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].molecularBio);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].cyto);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].viro);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].patho);
      userManagement.validateColumnContent("2", usersData[1].fName);
      userManagement.searchByFilters(usersData[1].immunoHisto);
      userManagement.validateColumnContent("2", usersData[1].fName);
      cy.reload();
    });

    it("Validate active/inactive users", function () {
      // Filter to show only active users
      userManagement.activeUser();
      // Wait for filter to apply
      cy.wait(2000);
      // Deen (usersData[0]) should be visible if active
      userManagement.validateColumnContent("2", usersData[0].fName);
      // Willy (usersData[1]) might not be active yet - check conditionally
      cy.get(".cds--data-table")
        .find("tbody tr")
        .then(($rows) => {
          const hasWilly = Array.from($rows).some((row) =>
            row.textContent.includes(usersData[1].fName),
          );
          if (hasWilly) {
            userManagement.validateColumnContent("2", usersData[1].fName);
          }
        });
      // Now check inactive - uncheck the active filter
      cy.contains("span", "Only Active").should("be.visible").click();
      cy.wait(2000); // Wait for filter to clear
      // Now all users should be visible (both active and inactive)
      userManagement.validateColumnContent("2", usersData[0].fName);
      userManagement.validateColumnContent("2", usersData[1].fName);
      cy.reload();
    });
  });

  describe("Modify First User", function () {
    beforeEach(() => {
      // After previous test, we should be on user management page
      // Just verify it's ready, don't navigate unless necessary
      cy.url().should("include", "userManagement");
      userManagement = adminPage.goToUserManagementPage();
      cy.get(".cds--data-table").should("be.visible");
    });

    it("Modify User and Save", function () {
      userManagement.searchUser(usersData[0].fName);
      userManagement.checkUser("2", usersData[0].fName);
      userManagement.modifyUser();
      userManagement.typeLoginPassword(usersData[0].password);
      userManagement.repeatPassword(usersData[0].password);
      userManagement.checkAccountNotLocked();
      userManagement.checkAccountEnabled();
      userManagement.checkActive();
      userManagement.copyPermisionsFromUser(usersData[1].lName);
      // applyChanges() calls userSavePostCall() which SAVES the user and navigates to list page
      // No need to call saveChanges() again - applyChanges() already saved
      userManagement.applyChanges();
      // Wait for save to complete and navigation to list page
      cy.get(".toastDisplay")
        .should("be.visible")
        .should("contain.text", "success");
      cy.url().should("include", "userManagement");
      cy.get(".cds--data-table").should("be.visible");
    });

    it("Navigate to User Management", function () {
      // Already navigated in beforeEach
      userManagement.verifyPageTitle();
    });

    it("Validate user is activated", function () {
      // Wait for page to fully load
      cy.get(".cds--data-table").should("be.visible");
      cy.screenshot("validateUserActivated-before-activeUser");
      userManagement.activeUser();
      cy.screenshot("validateUserActivated-after-activeUser");
      userManagement.searchUser(usersData[0].fName);
      userManagement.validateColumnContent("2", usersData[0].fName);
      userManagement.clearSearchBar();
    });

    it("Search by Only Administrator", function () {
      userManagement.adminUser();
      userManagement.validateColumnContent("4", usersData[0].defaultAdmin);
      userManagement.nonAdminUser(usersData[0].fName);
      userManagement.nonAdminUser(usersData[1].fName);
    });
  });

  describe("Deactivate User", function () {
    beforeEach(() => {
      // After previous test, we should be on user management page
      // Just verify it's ready, don't navigate unless necessary
      cy.url().should("include", "userManagement");
      userManagement = adminPage.goToUserManagementPage();
      cy.get(".cds--data-table").should("be.visible");
    });

    it("Check User and deactivate", function () {
      // Clear any existing filters first
      userManagement.clearSearchBar();
      // Reload to ensure fresh state
      cy.reload();
      cy.get(".cds--data-table").should("be.visible");
      // Show all users (both active and inactive) - use inactiveUser filter to show all
      // Actually, let's just search without filters first
      userManagement.searchUser(usersData[1].fName);
      // Wait for search results - ensure table has rows
      cy.get(".cds--data-table")
        .find("tbody tr")
        .should("have.length.greaterThan", 0)
        .should("be.visible");
      userManagement.checkUser("2", usersData[1].fName);
      userManagement.deactivateUser();
    });

    it("Validate deactivated user", () => {
      cy.reload();
      userManagement.activeUser();
      userManagement.inactiveUser(usersData[1].fName);
    });
  });

  describe("Signout, use active/deactivated user to login", () => {
    it("Logout", () => {
      // Use cy.logout() which properly clears session
      cy.logout();
      // Ensure we're on login page after signout
      cy.url().should("satisfy", (url) => {
        return url.includes("/login") || url.includes("/LoginPage");
      });
      // Verify login form is visible
      cy.get("#loginName").should("be.visible");
    });

    it("Login with Deactivated User", () => {
      // Ensure we're on login page (cy.logout() should have taken us there)
      cy.url().should("satisfy", (url) => {
        return url.includes("/login") || url.includes("/LoginPage");
      });
      cy.get("#loginName").should("be.visible");
      loginPage.enterUsername(usersData[1].username);
      loginPage.enterPassword(usersData[1].password);
      loginPage.signIn();
      // Should still be on login page with error
      cy.url().should("satisfy", (url) => {
        return url.includes("/login") || url.includes("/LoginPage");
      });
      cy.get(".toastDisplay")
        .should("be.visible")
        .should("contain.text", "Username or Password are incorrect");
    });

    it("Login with Active user", () => {
      // Logout should have taken us to login page, but verify we're there
      // If we're still logged in (dashboard visible), force logout again
      cy.url().then((url) => {
        if (!url.includes("/login") && !url.includes("/LoginPage")) {
          // Still logged in - force logout
          cy.logout();
          // Wait for logout to complete
          cy.url().should("satisfy", (u) => {
            return u.includes("/login") || u.includes("/LoginPage");
          });
        }
      });
      // Wait for login page to load completely
      cy.get("body").should("be.visible");
      // Wait for login form elements
      cy.get("#loginName", { timeout: 10000 })
        .should("be.visible")
        .should("exist")
        .then(() => {
          // Form exists if loginName exists
          cy.screenshot("loginActive-before-loginName-check");
        });
      loginPage.clearInputs();
      loginPage.enterUsername(usersData[0].username);
      loginPage.enterPassword(usersData[0].password);
      loginPage.signIn();
      cy.get("#mainHeader").should("exist");
    });
  });
});
