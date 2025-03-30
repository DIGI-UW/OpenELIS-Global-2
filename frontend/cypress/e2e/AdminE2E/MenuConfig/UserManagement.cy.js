import LoginPage from "../pages/LoginPage";
import HomePage from "../pages/HomePage";
import AdminPage from "../pages/AdminPage";
import UserManagementPage from "../pages/UserManagement";

let loginPage = null;
let homePage = null;
let adminPage = null;
let userManagementPage = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
  userManagementPage = adminPage.goToUserManagementPage();
});

describe("User Management Page Tests", () => {
  beforeEach(() => {
    userManagementPage.visitUserManagementPage();
  });

  it("should open and close the add user modal", () => {
    userManagementPage.clickAddUser();
    cy.get('[aria-label="Add New User"]').should("be.visible");
    cy.get('[aria-label="Cancel"]').click();
    cy.get('[aria-label="Add New User"]').should("not.exist");
  });

  it("should show error when adding user with empty fields", () => {
    userManagementPage.clickAddUser();
    userManagementPage.submitForm();
    cy.get("#username").parent().should("contain", "This field is required");
    cy.get("#fullName").parent().should("contain", "This field is required");
  });

  it("should add a new user", () => {
    const newUser = {
      username: "testuser",
      fullName: "Test User",
      email: "testuser@example.com",
      role: "Admin",
    };
    userManagementPage.clickAddUser();
    userManagementPage.fillUserDetails(newUser);
    userManagementPage.setPermissions();
    userManagementPage.submitForm();
    cy.contains("User added successfully");
    userManagementPage
      .searchUser(newUser.username)
      .should("contain", newUser.username);
  });

  it("should modify an existing user", () => {
    userManagementPage.searchUser("testuser");
    userManagementPage.selectUser("testuser");
    userManagementPage.updateUserDetails({ fullName: "Updated User" });
    userManagementPage.submitForm();
    cy.contains("User updated successfully");
  });

  it("should copy permissions from another user", () => {
    userManagementPage.searchUser("testuser");
    userManagementPage.selectUser("testuser");
    userManagementPage.copyPermissions("adminUser");
    userManagementPage.submitForm();
    cy.contains("Permissions copied successfully");
  });

  it("should disable an existing user", () => {
    userManagementPage.searchUser("testuser");
    userManagementPage.selectUser("testuser");
    userManagementPage.disableUser();
    userManagementPage.submitForm();
    cy.contains("User disabled successfully");
  });

  it("should delete an existing user", () => {
    userManagementPage.searchUser("testuser");
    userManagementPage.selectUser("testuser");
    userManagementPage.deleteUser();
    cy.contains("User deleted successfully");
  });
});
