import LoginPage from "../../pages/LoginPage";
import TestManagementPage from "../../pages/TestManagement";

describe("Test Management Tests", () => {
  let loginPage;
  let testManagementPage;
  let testData;

  before("Load fixtures and login", () => {
    // Load test data from fixture
    cy.fixture("testManagement").then((data) => {
      testData = data;
    });

    // Initialize the page objects
    loginPage = new LoginPage();
    testManagementPage = new TestManagementPage();

    // Login
    loginPage.visit();
    loginPage.goToHomePage();
  });

  describe("Method Management Tests", () => {
    beforeEach("Go to Method Management Page", () => {
      testManagementPage.visitMethodManagementPage();
    });

    it("should open and close the add method modal", () => {
      testManagementPage
        .openAddMethodModal()
        .verifyMethodModalVisible()
        .closeAddMethodModal();
    });

    it("should add a new valid method successfully", () => {
      cy.fixture("testManagement").then((data) => {
        const validMethod = data.methods.valid;

        testManagementPage
          .openAddMethodModal()
          .fillMethodForm(
            validMethod.name,
            validMethod.description,
            validMethod.category,
          )
          .saveMethod()
          .verifyMethodExists(validMethod.name);
      });
    });

    it("should validate required fields when saving an empty method", () => {
      testManagementPage
        .openAddMethodModal()
        .fillMethodForm("", "", "")
        .saveMethod();

      testManagementPage
        .getValidationError()
        .should("be.visible")
        .and("contain", "required");
    });

    it("should handle method names with special characters", () => {
      cy.fixture("testManagement").then((data) => {
        const specialCharsMethod = data.methods.specialChars;

        testManagementPage
          .openAddMethodModal()
          .fillMethodForm(
            specialCharsMethod.name,
            specialCharsMethod.description,
            specialCharsMethod.category,
          )
          .saveMethod()
          .verifyMethodExists(specialCharsMethod.name);
      });
    });

    it("should validate method name length", () => {
      cy.fixture("testManagement").then((data) => {
        const longNameMethod = data.methods.longName;

        testManagementPage
          .openAddMethodModal()
          .fillMethodForm(
            longNameMethod.name,
            longNameMethod.description,
            longNameMethod.category,
          )
          .saveMethod();

        testManagementPage
          .getValidationError()
          .should("be.visible")
          .and("contain", "maximum length");
      });
    });

    it("should search for an existing method", () => {
      cy.fixture("testManagement").then((data) => {
        const validMethod = data.methods.valid;

        testManagementPage
          .searchMethod(validMethod.name)
          .verifyMethodExists(validMethod.name);
      });
    });

    it("should show no results for non-existent method search", () => {
      const nonExistentMethod = "NonExistentMethodXYZ123";
      testManagementPage
        .searchMethod(nonExistentMethod)
        .verifyMethodNotExists(nonExistentMethod);
    });

    it("should cancel method deletion", () => {
      cy.fixture("testManagement").then((data) => {
        const validMethod = data.methods.valid;

        testManagementPage
          .deleteMethod(validMethod.name)
          .cancelDelete()
          .verifyMethodExists(validMethod.name);
      });
    });

    it("should delete a method successfully", () => {
      cy.fixture("testManagement").then((data) => {
        const specialCharsMethod = data.methods.specialChars;

        testManagementPage
          .deleteMethod(specialCharsMethod.name)
          .confirmDelete()
          .verifyMethodNotExists(specialCharsMethod.name);
      });
    });

    it("should handle pagination correctly", () => {
      cy.fixture("testManagement").then((data) => {
        // Assuming there are multiple pages of methods
        testManagementPage
          .changePageSize(data.pageSizes[0])
          .goToNextPage()
          .goToPreviousPage();
      });
    });
  });

  describe("Test Catalog Page Tests", () => {
    beforeEach("Go to Test Catalog Page", () => {
      testManagementPage.visitTestCatalogPage();
    });

    it("should load the Test Catalog page successfully", () => {
      cy.contains("Test Catalog").should("be.visible");
    });

    it("should add a new test to catalog successfully", () => {
      cy.fixture("testManagement").then((data) => {
        const validTest = data.tests.valid;
        const validMethod = data.methods.valid;

        testManagementPage
          .addTestToCatalog()
          .fillTestForm(validTest.name, validMethod.name, validTest.protocol)
          .saveTest()
          .verifyMethodExists(validTest.name); // Reusing the method verification for tests
      });
    });

    it("should validate required fields when saving an empty test", () => {
      testManagementPage.addTestToCatalog().fillTestForm("", "", "").saveTest();

      testManagementPage
        .getValidationError()
        .should("be.visible")
        .and("contain", "required");
    });

    it("should search for an existing test", () => {
      cy.fixture("testManagement").then((data) => {
        const validTest = data.tests.valid;

        testManagementPage
          .searchMethod(validTest.name)
          .verifyMethodExists(validTest.name);
      });
    });

    it("should handle edge case with method having no associated tests", () => {
      cy.fixture("testManagement").then((data) => {
        const noTestsMethod = data.methods.noTests;
        const noTestMethodTest = data.tests.noTestMethod;

        // Create a method with no tests
        testManagementPage
          .visitMethodManagementPage()
          .openAddMethodModal()
          .fillMethodForm(
            noTestsMethod.name,
            noTestsMethod.description,
            noTestsMethod.category,
          )
          .saveMethod()
          .visitTestCatalogPage()
          .addTestToCatalog();

        // Verify the method is available in the dropdown
        cy.get("[data-cy='testMethod']").select(noTestsMethod.name);

        testManagementPage
          .fillTestForm(
            noTestMethodTest.name,
            noTestsMethod.name,
            noTestMethodTest.protocol,
          )
          .saveTest()
          .verifyMethodExists(noTestMethodTest.name);
      });
    });

    it("should handle duplicate test name error", () => {
      cy.fixture("testManagement").then((data) => {
        const validTest = data.tests.valid;
        const duplicateTest = data.tests.duplicate;
        const validMethod = data.methods.valid;

        // Try to add test with same name
        testManagementPage
          .addTestToCatalog()
          .fillTestForm(
            duplicateTest.name,
            validMethod.name,
            duplicateTest.protocol,
          )
          .saveTest();

        testManagementPage
          .getValidationError()
          .should("be.visible")
          .and("contain", "already exists");
      });
    });
  });

  describe("Cross-functional and session tests", () => {
    it("should maintain state when navigating between pages", () => {
      cy.fixture("testManagement").then((data) => {
        const tempMethod = data.methods.temporary;
        const tempTest = data.tests.navigation;

        testManagementPage
          .visitMethodManagementPage()
          .openAddMethodModal()
          .fillMethodForm(
            tempMethod.name,
            tempMethod.description,
            tempMethod.category,
          )
          .saveMethod()
          .verifyMethodExists(tempMethod.name)
          .visitTestCatalogPage()
          .addTestToCatalog()
          .fillTestForm(tempTest.name, tempMethod.name, tempTest.protocol)
          .saveTest()
          .verifyMethodExists(tempTest.name);
      });
    });

    it("should handle session timeout gracefully", () => {
      // Simulate session timeout by manipulating localStorage
      cy.window().then((win) => {
        win.localStorage.removeItem("authToken");
      });

      // Try to navigate
      testManagementPage.visitMethodManagementPage();

      // Should be redirected to login
      cy.url().should("include", "/login");

      // Login again
      loginPage.login();
    });
  });

  after("Cleanup", () => {
    cy.fixture("testManagement").then((data) => {
      // Clean up created test data
      testManagementPage
        .visitMethodManagementPage()
        .searchMethod(data.methods.valid.name)
        .deleteMethod(data.methods.valid.name)
        .confirmDelete();

      // Delete temporary method
      testManagementPage
        .searchMethod(data.methods.temporary.name)
        .deleteMethod(data.methods.temporary.name)
        .confirmDelete();

      // Delete no test method
      testManagementPage
        .searchMethod(data.methods.noTests.name)
        .deleteMethod(data.methods.noTests.name)
        .confirmDelete();

      // Finally, logout
      loginPage.logout();
    });
  });
});
