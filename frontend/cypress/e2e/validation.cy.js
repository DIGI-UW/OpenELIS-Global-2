import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let validation = null;

// Mock data for validation results
const mockValidationResults = {
  statusCode: 200,
  body: {
    resultList: [
      {
        analysisId: "12345",
        accessionNumber: "DEV01260000000000001",
        testName: "Glucose",
        result: "5.5",
        normalRange: "3.9-6.1",
        isNormal: true,
        isAccepted: false,
        patientName: "John Doe",
      },
      {
        analysisId: "12346",
        accessionNumber: "DEV01260000000000002",
        testName: "Hemoglobin",
        result: "14.2",
        normalRange: "12.0-16.0",
        isNormal: true,
        isAccepted: false,
        patientName: "Jane Smith",
      },
    ],
    testSections: [],
    testSectionsByName: [],
    paging: { totalPages: "1", currentPage: "1" },
  },
};

const mockEmptyResults = {
  statusCode: 200,
  body: {
    resultList: [],
    testSections: [],
    testSectionsByName: [],
    paging: { totalPages: "0", currentPage: "1" },
  },
};

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

const navigateToValidationPage = (validationType) => {
  homePage = loginPage.goToHomePage();
  validation = homePage[`goToValidationBy${validationType}`]();
};

const setupValidationIntercepts = (mockResults = null) => {
  cy.intercept("GET", "**/rest/user-test-sections/**").as("testSections");
  if (mockResults) {
    cy.intercept("GET", "**/rest/AccessionValidation**", mockResults).as(
      "searchResults",
    );
  } else {
    cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
  }
};

const selectFirstLabUnitAndSearch = () => {
  cy.get("#lab-unit-select option")
    .eq(1)
    .invoke("val")
    .then((value) => {
      if (value) {
        cy.get("#lab-unit-select").select(value);
      }
    });
  cy.get('button[type="submit"]').click();
};

describe("Validation Page - Basic Navigation", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Routine");
  });

  it("User visits Validation Page and sees heading", function () {
    validation.checkForHeading();
  });

  it("Should display lab unit dropdown", function () {
    cy.get("#lab-unit-select").should("be.visible");
  });

  it("Should display search query input", function () {
    cy.get("#search-query").should("be.visible");
  });

  it("Should display search button", function () {
    cy.get('button[type="submit"]').should("be.visible");
  });
});

describe("Validation Page - Search by Lab Unit", function () {
  before("navigate to Validation Page", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
  });

  it("Should load lab units in dropdown", function () {
    cy.get("#lab-unit-select option").should("have.length.greaterThan", 1);
  });

  it("Should select test unit and trigger search", function () {
    cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 30000 })
      .its("response.statusCode")
      .should("be.oneOf", [200, 304]);
  });
});

describe("Validation Page - Search by Lab Number", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Order");
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Should search by lab number", function () {
    cy.fixture("Patient").then((data) => {
      cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
      validation.enterSearchQuery(data.labNo);
      validation.clickSearch();
      cy.wait("@searchResults", { timeout: 30000 });
    });
  });
});

describe("Validation Page - Advanced Filters", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Routine");
  });

  it("Should toggle advanced filters panel", function () {
    cy.contains("button", "Filters").click();
    cy.get("#lab-number-from").should("be.visible");
    cy.get("#lab-number-to").should("be.visible");
  });

  it("Should enter lab number range filter", function () {
    cy.fixture("Patient").then((data) => {
      cy.get("#lab-number-from").clear().type(data.labNo);
      cy.get("#lab-number-to").clear().type(data.endLabNo);
    });
  });

  it("Should toggle quick filters", function () {
    cy.get("#filter-normal").check({ force: true });
    cy.get("#filter-normal").should("be.checked");
    cy.get("#filter-abnormal").check({ force: true });
    cy.get("#filter-abnormal").should("be.checked");
    cy.get("#filter-normal").should("not.be.checked");
  });

  it("Should clear filters", function () {
    cy.contains("button", "Clear").click();
    cy.get("#lab-number-from").should("have.value", "");
    cy.get("#lab-number-to").should("have.value", "");
    cy.get("#filter-normal").should("not.be.checked");
    cy.get("#filter-abnormal").should("not.be.checked");
  });
});

describe("Validation Page - Results Table", function () {
  before("navigate to Validation Page and search", function () {
    setupValidationIntercepts(mockValidationResults);
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 30000 });
  });

  it("Should display results table", function () {
    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
      "be.visible",
    );
    cy.get('table[aria-label="validation results"]').should("be.visible");
  });

  it("Should display batch action buttons", function () {
    cy.contains("button", "Accept").should("be.visible");
    cy.contains("button", "Retest").should("be.visible");
  });

  it("Should have select all checkbox", function () {
    cy.get("#select-all").should("exist");
  });
});

describe("Validation Page - Batch Actions", function () {
  it("Should disable Accept button when no rows selected", function () {
    setupValidationIntercepts(mockValidationResults);
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 10000 });
    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
      "be.visible",
    );
    cy.contains("button", "Accept").should("be.disabled");
  });

  it("Should disable Retest button when no rows selected", function () {
    setupValidationIntercepts(mockValidationResults);
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 10000 });
    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
      "be.visible",
    );
    cy.contains("button", "Retest").should("be.disabled");
  });
});

describe("Validation Page - Retest Modal", function () {
  before("navigate to Validation Page", function () {
    setupValidationIntercepts(mockValidationResults);
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 10000 });
  });

  it("Should open retest modal when rows selected", function () {
    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
      "be.visible",
    );
    cy.get('input[type="checkbox"][id^="checkbox-"]').first().check({
      force: true,
    });
    cy.contains("button", "Retest").click();
    cy.get(".cds--modal").should("be.visible");
    cy.get("#retest-reason").should("be.visible");
  });

  it("Should require reason before submitting", function () {
    cy.get(".cds--modal-footer .cds--btn--primary").click();
    cy.get(".cds--text-area--invalid").should("exist");
  });

  it("Should close modal on cancel", function () {
    cy.get(".cds--modal-footer .cds--btn--secondary").click();
    cy.get(".cds--modal", { timeout: 5000 }).should("not.be.visible");
  });
});

describe("Validation Page - Accept and Release", function () {
  before("navigate to Validation Page", function () {
    setupValidationIntercepts(mockValidationResults);
    cy.intercept("POST", "**/rest/AccessionValidation", {
      statusCode: 200,
      body: { success: true },
    }).as("acceptRequest");
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 10000 });
  });

  it("Should select all rows using checkbox", function () {
    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
      "be.visible",
    );
    cy.get("#select-all").check({ force: true });
    cy.contains("2 Selected").should("be.visible");
  });

  it("Should select only normal results", function () {
    cy.get("#select-all").uncheck({ force: true });
    cy.contains("button", "Select Normal").click();
    cy.contains("Selected").should("be.visible");
  });

  it("Should submit accept and release request", function () {
    cy.intercept("POST", "**/rest/AccessionValidation", {
      statusCode: 200,
      body: { success: true },
    }).as("acceptRequest");
    cy.get("#select-all").check({ force: true });
    cy.contains("button", "Accept").click();
    cy.wait("@acceptRequest", { timeout: 10000 })
      .its("response.statusCode")
      .should("eq", 200);
  });
});

describe("Validation Page - Empty State", function () {
  before("navigate to Validation Page", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
  });

  it("Should display empty state when no results", function () {
    cy.intercept("GET", "**/rest/AccessionValidation**", mockEmptyResults).as(
      "emptyResults",
    );
    cy.get("#search-query").clear().type("NONEXISTENT12345");
    cy.get('button[type="submit"]').click();
    cy.wait("@emptyResults", { timeout: 10000 });
    cy.get("body").then(($body) => {
      const hasEmptyState = $body.find(".validation-empty-state").length > 0;
      const hasNoResultsText = $body.text().includes("No results");
      expect(hasEmptyState || hasNoResultsText).to.be.true;
    });
  });
});
