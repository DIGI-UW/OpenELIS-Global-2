import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";
import AdminPage from "../../pages/AdminPage";
import TestManagementPage from "../../pages/TestManagement";

let loginPage = null;
let homePage = null;
let adminPage = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Manage Method Page Tests", () => {
  beforeEach(() => {
    TestManagementPage.visitMethodManagementPage();
  });

  it("should open and close the add method modal", () => {
    TestManagementPage.openAddMethodModal();
    cy.get('[aria-label="Add New Method"]').should("be.visible");
    cy.get('[aria-label="Cancel"]').click();
    cy.get('[aria-label="Add New Method"]').should("not.exist");
  });

  it("should show error when adding method with empty fields", () => {
    TestManagementPage.openAddMethodModal();
    TestManagementPage.submitForm();
    cy.get("#englishLabel")
      .parent()
      .should("contain", "This field is required");
    cy.get("#frenchLabel").parent().should("contain", "This field is required");
  });

  it("should add a new method", () => {
    const englishMethod = "Test Method EN";
    const frenchMethod = "Test Method FR";
    TestManagementPage.openAddMethodModal();
    TestManagementPage.fillMethodForm(englishMethod, frenchMethod);
    TestManagementPage.submitForm();
    TestManagementPage.confirmAddMethod();
    cy.contains("Save configuration successful");
    TestManagementPage.getExistingMethods().should("contain", englishMethod);
  });

  it("should display existing and inactive methods", () => {
    TestManagementPage.getExistingMethods().should(
      "have.length.greaterThan",
      0,
    );
    TestManagementPage.getInactiveMethods().should(
      "have.length.greaterThan",
      0,
    );
  });
});

describe("Test Catalog Page E2E Tests", () => {
  beforeEach(() => {
    TestManagementPage.visitTestCatalogPage();
  });

  it("should load the Test Catalog page successfully", () => {
    cy.url().should("include", "/MasterListsPage#TestCatalog");
  });

  it("should toggle the guide visibility", () => {
    TestManagementPage.getToggleButton().click();
    TestManagementPage.getGuideSection().should("be.visible");
    TestManagementPage.getToggleButton().click();
    TestManagementPage.getGuideSection().should("not.exist");
  });

  it("should select test sections and display tags", () => {
    TestManagementPage.selectTestSection("All");
    TestManagementPage.getSelectedTags().should("contain.text", "All");
  });

  it("should display tabs for selected sections and switch between them", () => {
    TestManagementPage.selectTestSection("All");
    TestManagementPage.getTabs().should("have.length.greaterThan", 0);
    TestManagementPage.selectTab("All");
    TestManagementPage.getDataTable().should("be.visible");
  });

  it("should filter data correctly when selecting a specific section", () => {
    TestManagementPage.selectTestSection("Specific Section");
    TestManagementPage.getSelectedTags().should(
      "contain.text",
      "Specific Section",
    );
    TestManagementPage.selectTab("Specific Section");
    TestManagementPage.getDataTable().should("be.visible");
  });

  it("should handle no data scenario gracefully", () => {
    TestManagementPage.selectTestSection("Empty Section");
    TestManagementPage.selectTab("Empty Section");
    TestManagementPage.getDataTable().should("not.exist");
    cy.contains("No data available").should("be.visible");
  });

  it("should display breadcrumb navigation correctly", () => {
    cy.get(".breadcrumb").should("exist");
    cy.get(".breadcrumb").contains("home.label").should("exist");
    cy.get(".breadcrumb")
      .contains("breadcrums.admin.managment")
      .should("exist");
    cy.get(".breadcrumb")
      .contains("sidenav.label.admin.testmgt.ViewtestCatalog")
      .should("exist");
  });
});
