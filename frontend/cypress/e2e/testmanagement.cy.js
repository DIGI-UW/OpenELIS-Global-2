import TestManagementPage from "../pages/TestManagement";
describe("TestManagement -> Manage Method and Test Catalog", () => {
  // Manage Method Tests
  describe("Manage Method Component", () => {
    beforeEach(() => {
      cy.visit("/path-to-your-manage-method-page");
    });

    it("should render the Manage Method page correctly", () => {
      cy.contains("Manage Method").should("be.visible");
    });

    it('should open the add method modal when the "Add New Method" button is clicked', () => {
      cy.contains("Add New Method").click();
      cy.get('[role="dialog"]').should("be.visible");
    });

    it("should show input errors when English or French labels are empty and Save is clicked", () => {
      cy.contains("Add New Method").click();
      cy.contains("Save").click();
      cy.get(".bx--text-input--invalid").should("have.length", 2);
      cy.contains("This field is required").should("be.visible");
    });

    it("should successfully add method and display success notification", () => {
      cy.contains("Add New Method").click();
      cy.get('input[name="englishLabel"]').type("English Method");
      cy.get('input[name="frenchLabel"]').type("French Method");
      cy.contains("Save").click();
      cy.contains("Accept").click();
      cy.contains("Method added successfully").should("be.visible");
    });

    it("should handle error on failed method addition", () => {
      cy.intercept("POST", "/rest/MethodCreate", {
        statusCode: 500,
        body: { message: "Error occurred" },
      }).as("addMethodError");

      cy.contains("Add New Method").click();
      cy.get('input[name="englishLabel"]').type("English Method");
      cy.get('input[name="frenchLabel"]').type("French Method");
      cy.contains("Save").click();
      cy.contains("Accept").click();
      cy.wait("@addMethodError");
      cy.contains("Error occurred").should("be.visible");
    });

    it("should render existing and inactive methods", () => {
      cy.intercept("GET", "/rest/MethodCreate", {
        statusCode: 200,
        body: {
          existingMethodList: [{ id: 1, value: "Method 1" }],
          inactiveMethodList: [{ id: 2, value: "Method 2" }],
        },
      }).as("getMethods");

      cy.reload();
      cy.wait("@getMethods");
      cy.contains("Existing Methods").should("be.visible");
      cy.contains("Method 1").should("be.visible");
      cy.contains("Inactive Methods").should("be.visible");
      cy.contains("Method 2").should("be.visible");
    });
  });

  // Test Catalog Tests
  describe("Test Catalog Component", () => {
    beforeEach(() => {
      cy.visit("/path-to-your-test-catalog-page"); // Adjust the URL accordingly
    });

    it("should render the Test Catalog page correctly", () => {
      cy.contains("View Test Catalog").should("be.visible");
    });

    it("should toggle the Show Guide feature", () => {
      cy.get('[aria-label="Structuredlist"]').should("not.exist");
      cy.contains("ShowGuide").click();
      cy.get('[aria-label="Structuredlist"]').should("be.visible");
    });

    it("should select sections from the FilterableMultiSelect", () => {
      cy.get('[id="carbon-multiselect-example-3"]').click();
      cy.contains("Section 1").click();
      cy.contains("Section 1").should("be.visible");
    });

    it("should display the test catalog data in tables", () => {
      cy.intercept("GET", "/rest/TestCatalog", {
        statusCode: 200,
        body: {
          testSectionList: ["Section 1", "Section 2"],
          testCatalogList: [
            {
              id: "1",
              testUnit: "Unit 1",
              sampleType: "Sample A",
              panel: "Panel A",
              resultType: "Numeric",
            },
            {
              id: "2",
              testUnit: "Unit 2",
              sampleType: "Sample B",
              panel: "Panel B",
              resultType: "Text",
            },
          ],
        },
      }).as("getTestCatalogData");

      cy.reload();
      cy.wait("@getTestCatalogData");

      cy.contains("Unit 1").should("be.visible");
      cy.contains("Sample A").should("be.visible");
      cy.contains("Panel A").should("be.visible");
      cy.contains("Numeric").should("be.visible");
    });

    it("should display selected test section data in tabs", () => {
      cy.intercept("GET", "/rest/TestCatalog", {
        statusCode: 200,
        body: {
          testSectionList: ["Section 1", "Section 2"],
          testCatalogList: [
            {
              id: "1",
              testUnit: "Unit 1",
              sampleType: "Sample A",
              panel: "Panel A",
              resultType: "Numeric",
            },
            {
              id: "2",
              testUnit: "Unit 2",
              sampleType: "Sample B",
              panel: "Panel B",
              resultType: "Text",
            },
          ],
        },
      }).as("getTestCatalogData");

      cy.reload();
      cy.wait("@getTestCatalogData");

      cy.contains("Section 1").click();
      cy.contains("Unit 1").should("be.visible");
      cy.contains("Sample A").should("be.visible");

      cy.contains("Section 2").click();
      cy.contains("Unit 2").should("be.visible");
      cy.contains("Sample B").should("be.visible");
    });
  });
});
