import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let userManagement = null;
let usersData;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

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
    it("Add User", function () {
      userManagement.clickAddButton();
      userManagement.validatePageTitle();
    });

    it("Enter User details", function () {
      userManagement.typeLoginName(usersData[0].username);
      userManagement.passwordExpiryDate(usersData[0].passwordExpiryDate);
      userManagement.typeLoginPassword(usersData[0].password);
      userManagement.repeatPassword(usersData[0].password);
      userManagement.enterFirstName(usersData[0].fName);
      userManagement.enterLastName(usersData[0].lName);
      userManagement.enterUserTimeout(usersData[0].userTimeout);
    });

    it("Add and Remove Lab Unit Roles", function () {
      userManagement.addNewPermission();
      userManagement.allPermissions();
      userManagement.removePermission();
    });

    it("Apply Roles and Permissions", function () {
      userManagement.analyzerImport();
      userManagement.globalAdministrator();
      userManagement.addNewPermission();
      userManagement.allPermissions();
    });

    it("Exit", function () {
      userManagement.exitChanges();
    });
  });

  describe("Add Users and Save", function () {
    it("Add First User", function () {
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
    });

    it("Apply Roles and Permissions", function () {
      userManagement.globalAdministrator();
      userManagement.addNewPermission();
      userManagement.allPermissions();
    });

    it("Save User", function () {
      userManagement.saveChanges();
    });

    it("Add Second User", function () {
      userManagement = adminPage.goToUserManagementPage();
    });

    it("Enter details", function () {
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
    });

    it("Apply Roles and Permissions", function () {
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
    });

    it("Save User", function () {
      userManagement.saveChanges();
      cy.wait(2000);
    });
  });

  describe("Validate added Users", function () {
    it("Search users by Usernames", function () {
      userManagement = adminPage.goToUserManagementPage();
      userManagement.verifyPageTitle();
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
      userManagement.activeUser(); //checks active users
      userManagement.inactiveUser(usersData[0].fName);
      userManagement.validateColumnContent("2", usersData[1].fName);
      cy.reload();
    });
  });

  describe("Modify First User", function () {
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
      userManagement.applyChanges();
      cy.wait(1000);
      userManagement.saveChanges();
    });

    it("Navigate to User Management", function () {
      userManagement = adminPage.goToUserManagementPage();
    });

    it("Validate user is activated", function () {
      userManagement.verifyPageTitle();
      userManagement.activeUser();
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
    it("Check User and deactivate", function () {
      userManagement.adminUser();
      userManagement.searchUser(usersData[1].fName);
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
      userManagement = loginPage.signOut();
    });

    it("Login with Deactivated User", () => {
      loginPage.enterUsername(usersData[1].username);
      loginPage.enterPassword(usersData[1].password);
      loginPage.signIn();
      cy.contains("Username or Password are incorrect").should("be.visible");
    });

    it("Login with Active user", () => {
      loginPage.clearInputs();
      loginPage.enterUsername(usersData[0].username);
      loginPage.enterPassword(usersData[0].password);
      loginPage.signIn();
    });
  });
});
